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

public class Worker implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(Worker.class.getName());
    private final Coordinator coordinator;
    private final Phaser phaser;

    public Worker(Coordinator coordinator, Phaser phaser) {
        this.coordinator = coordinator;
        this.phaser = phaser;
        phaser.register();
    }

    @Override
    public void run() {
        mapFunction();

        coordinator.getIntermediateFiles()
                .forEach((k, v) -> coordinator.addTasksReduce(new ReduceTask(k, v)));

        phaser.arriveAndAwaitAdvance();

        reduceFunction();
    }


    public void mapFunction() {
        MapTask task;
        while ((task = coordinator.getTasksMap()) != null) {
            String content = readTextFile(task.getFileName());
            List<KeyValue> map = convertStringToListKeyValue(content);
            writeToIntermediateFiles(map, task);
        }
        phaser.arriveAndAwaitAdvance();
    }

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

    private String readTextFile(String fileName) {
        StringBuilder stringBuilder = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {

            while (reader.ready()) {
                stringBuilder.append(reader.readLine());
            }

        } catch (FileNotFoundException e) {
            LOGGER.log(Level.SEVERE, "File not found: " + fileName, e);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "I/O error", e);
        }
        return stringBuilder.toString();
    }

    private List<KeyValue> convertStringToListKeyValue(String content) {
        String[] strings = divisionWords(content);

        return Arrays.stream(strings)
                .map(KeyValue::new)
                .toList();

    }

    public void writeToIntermediateFiles(List<KeyValue> keyValues, MapTask task) {
        Map<Integer, List<KeyValue>> map = distributeKeysToBuckets(keyValues, task.getCountReduce());

        Map<Integer, List<String>> buckets = new HashMap<>();

        for (Map.Entry<Integer, List<KeyValue>> entry : map.entrySet()) {
            String fileName = "mr-" + task.getTaskId() + "-" + entry.getKey();
            writeListObject(entry.getValue(), fileName);
            buckets.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(fileName);
        }

        addStagingStorage(buckets);
    }

    private void addStagingStorage(Map<Integer, List<String>> map) {
        map.forEach((key, value) ->
                coordinator.getIntermediateFiles().computeIfAbsent(key, e -> new ArrayList<>()).addAll(value));
        phaser.arriveAndAwaitAdvance();
    }

    private static void writeListObject(List<?> keyValues, String fileName) {
        try (BufferedOutputStream bufferedOutputStream =
                     new BufferedOutputStream(new FileOutputStream(fileName));
             ObjectOutputStream objectOutputStream =
                     new ObjectOutputStream(bufferedOutputStream)) {

            objectOutputStream.writeObject(keyValues);

        } catch (FileNotFoundException e) {
            LOGGER.log(Level.SEVERE, "File not found: " + fileName, e);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "I/O error", e);
        }
    }

    private Map<Integer, List<KeyValue>> distributeKeysToBuckets(List<KeyValue> keyValues, int m) {
        return keyValues.stream()
                .collect(Collectors.groupingBy(kv -> Math.abs(kv.getKey().hashCode()) % m));
    }

    private int reduce(List<Integer> values) {
        return values.stream().mapToInt(Integer::intValue).sum();
    }

    private void writeReduceResult(int reduceNumber, Map<String, Integer> results) {
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter("kr-" + reduceNumber + ".txt"))) {
            System.out.println(results);
            for (Map.Entry<String, Integer> entry : results.entrySet()) {
                writer.write(entry.getKey() + " " + entry.getValue() + "\n");
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "I/O error", e);
        }
    }

    private List<KeyValue> readListKeyValue(String fileName) {
        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(fileName));
             ObjectInputStream objectInputStream = new ObjectInputStream(bufferedInputStream)) {

            return (List<KeyValue>) objectInputStream.readObject();

        } catch (IOException | ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Error reading file: " + fileName, e);
            return Collections.emptyList();
        }
    }


    private String[] divisionWords(String content) {
        return Arrays.stream(content.trim().toLowerCase()
                        .split("(?<=\\s)-(?=\\s)|[\\pP\\s&&[^'-]]+"))
                .filter(part -> !part.trim().isEmpty())
                .toArray(String[]::new);
    }
}