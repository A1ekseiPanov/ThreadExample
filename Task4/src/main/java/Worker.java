import exceptions.FileWorkException;
import model.KeyValue;
import model.MapTask;
import model.ReduceTask;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Класс Worker представляет собой рабочий поток, выполняющий задачи Map и Reduce.
 * Он читает, обрабатывает, распределяет и записывает данные, координируя действия
 * с объектом Coordinator.
 */
public class Worker implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(Worker.class.getName());
    private final Coordinator coordinator;
    private final Phaser phaser;

    public Worker(Coordinator coordinator) {
        this.coordinator = coordinator;
        this.phaser = coordinator.getPhaser();
        phaser.register();
    }

    /**
     * Запускает процесс выполнения задач. Сначала выполняет Map-функцию, затем Reduce-функцию.
     */
    @Override
    public void run() {
        mapFunction();

        coordinator.getIntermediateFiles()
                .forEach((k, v) -> coordinator.addTasksReduce(new ReduceTask(k, v)));

        phaser.arriveAndAwaitAdvance();

        reduceFunction();
    }

    /**
     * Выполняет Map-функцию, читая содержимое файла, создавая пары ключ-значение,
     * и записывает их в промежуточные файлы.
     */
    public void mapFunction() {
        MapTask task;
        while ((task = coordinator.getTasksMap()) != null) {
            String content = readTextFile(task.getFileName());
            List<KeyValue> map = convertStringToListKeyValue(content);
            writeToIntermediateFiles(map, task);
        }
        phaser.arriveAndAwaitAdvance();
    }

    /**
     * Выполняет Reduce-функцию, объединяя значения по ключам, записывая результат в итоговые файлы
     * и удаляя промежуточные файлы после обработки.
     */
    public void reduceFunction() {
        ReduceTask task;
        while ((task = coordinator.getTasksReduce()) != null) {
            List<String> files = task.getFiles();
            List<KeyValue> keyValues = new ArrayList<>();

            for (String fn : files) {
                keyValues.addAll(readListKeyValue(fn));
                try {
                    Files.delete(Path.of(fn));
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Error deleting file", e);
                    throw new FileWorkException("Error deleting file", e);
                }
            }

            Map<String, List<Integer>> groupedByKeys = keyValues.stream()
                    .collect(Collectors.groupingBy(KeyValue::getKey,
                            Collectors.mapping(KeyValue::getValue, Collectors.toList())));

            Map<String, Integer> result = new TreeMap<>();
            groupedByKeys.forEach((key, value) -> result.merge(key, reduce(value), Integer::sum));
            writeReduceResult(task.getReduceNumberTask(), result);
        }

        phaser.arriveAndAwaitAdvance();
        phaser.arriveAndDeregister();
    }

    /**
     * Читает текстовый файл.
     *
     * @param fileName имя файла
     * @return содержимое файла в виде строки
     */
    private String readTextFile(String fileName) {
        StringBuilder stringBuilder = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {

            while (reader.ready()) {
                stringBuilder.append(reader.readLine());
            }

        } catch (FileNotFoundException e) {
            LOGGER.log(Level.SEVERE, "File not found: " + fileName, e);
            throw new FileWorkException("File not found: " + fileName, e);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "I/O error", e);
            throw new FileWorkException("I/O error", e);
        }
        return stringBuilder.toString();
    }

    /**
     * Преобразует строку текста в список объектов KeyValue.
     *
     * @param content содержимое файла
     * @return список пар ключ-значение
     */
    private List<KeyValue> convertStringToListKeyValue(String content) {
        String[] strings = divisionWords(content);

        return Arrays.stream(strings)
                .map(KeyValue::new)
                .toList();

    }

    /**
     * Записывает список пар ключ-значение в промежуточные файлы.
     *
     * @param keyValues список KeyValue
     * @param task      задача MapTask
     */
    private void writeToIntermediateFiles(List<KeyValue> keyValues, MapTask task) {
        Map<Integer, List<KeyValue>> map = distributeKeysToBuckets(keyValues, task.getCountReduce());

        Map<Integer, List<String>> buckets = new HashMap<>();

        for (Map.Entry<Integer, List<KeyValue>> entry : map.entrySet()) {
            String fileName = "mr-" + task.getTaskId() + "-" + entry.getKey();
            writeListObject(entry.getValue(), fileName);
            buckets.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(fileName);
        }

        addStagingStorage(buckets);
    }

    /**
     * Добавляет промежуточные файлы в хранилище координатора.
     *
     * @param map хранилище промежуточных файлов
     */
    private void addStagingStorage(Map<Integer, List<String>> map) {
        map.forEach((key, value) ->
                coordinator.getIntermediateFiles().computeIfAbsent(key, e -> new ArrayList<>()).addAll(value));
        phaser.arriveAndAwaitAdvance();
    }

    /**
     * Записывает объект в файл.
     *
     * @param keyValues список объектов для записи
     * @param fileName  имя файла
     */
    private static void writeListObject(List<?> keyValues, String fileName) {
        try (BufferedOutputStream bufferedOutputStream =
                     new BufferedOutputStream(new FileOutputStream(fileName));
             ObjectOutputStream objectOutputStream =
                     new ObjectOutputStream(bufferedOutputStream)) {

            objectOutputStream.writeObject(keyValues);

        } catch (FileNotFoundException e) {
            LOGGER.log(Level.SEVERE, "File not found: " + fileName, e);
            throw new FileWorkException("File not found: " + fileName, e);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "I/O error", e);
            throw new FileWorkException("I/O error", e);
        }

    }

    /**
     * Распределяет пары ключ-значение по корзинам в зависимости от хэш-кода ключа.
     *
     * @param keyValues список KeyValue
     * @param m         количество корзин
     * @return карта, распределяющая KeyValue по корзинам
     */
    private Map<Integer, List<KeyValue>> distributeKeysToBuckets(List<KeyValue> keyValues, int m) {
        return keyValues.stream()
                .collect(Collectors.groupingBy(kv -> Math.abs(kv.getKey().hashCode()) % m));
    }

    /**
     * Применяет операцию reduce к списку значений.
     *
     * @param values список значений
     * @return сумма значений
     */
    private int reduce(List<Integer> values) {
        return values.stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Записывает результат задачи Reduce в файл.
     *
     * @param reduceNumber номер задачи Reduce
     * @param results      результаты в виде карты ключ-значение
     */
    private void writeReduceResult(int reduceNumber, Map<String, Integer> results) {
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter("kr-" + reduceNumber + ".txt"))) {
            System.out.println(results);
            for (Map.Entry<String, Integer> entry : results.entrySet()) {
                writer.write(entry.getKey() + " " + entry.getValue() + "\n");
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "I/O error", e);
            throw new FileWorkException("I/O error", e);
        }
    }

    /**
     * Читает список KeyValue из файла.
     *
     * @param fileName имя файла
     * @return список KeyValue, прочитанный из файла
     */
    private List<KeyValue> readListKeyValue(String fileName) {
        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(fileName));
             ObjectInputStream objectInputStream = new ObjectInputStream(bufferedInputStream)) {

            return (List<KeyValue>) objectInputStream.readObject();

        } catch (FileNotFoundException | ClassNotFoundException e) {
            throw new FileWorkException(e);
        } catch (IOException e) {
            throw new FileWorkException("I/O error", e);
        }
    }

    /**
     * Разделяет содержимое строки на слова, удаляя пунктуацию.
     *
     * @param content строка текста
     * @return массив слов
     */
    private String[] divisionWords(String content) {
        return Arrays.stream(content.trim().toLowerCase()
                        .split("(?<=\\s)-(?=\\s)|[\\pP\\s&&[^'-]]+"))
                .filter(part -> !part.trim().isEmpty())
                .toArray(String[]::new);
    }
}