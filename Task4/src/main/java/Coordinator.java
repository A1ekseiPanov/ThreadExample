import model.MapTask;
import model.ReduceTask;
import util.CheckVariables;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.*;

import static util.CheckVariables.*;

/**
 * Класс Coordinator управляет выполнением задач Map и Reduce .
 * Он организует исполнение задач с помощью многопоточности и синхронизации.
 */
public class Coordinator {
    /**
     * Объект Phaser для управления фазами выполнения задач
     */
    private final Phaser phaser;
    /**
     * Количество рабочих потоков
     */
    private final int countWorker;
    /**
     * ExecutorService для управления потоками
     */
    private final ExecutorService executor;
    /**
     * Очередь задач Map
     */
    private final Queue<MapTask> tasksMap;
    /**
     * Очередь задач Reduce
     */
    private final Queue<ReduceTask> tasksReduce;
    /**
     * Хранит промежуточные файлы, сгруппированные по ключу
     */
    private final Map<Integer, List<String>> intermediateFiles;
    /**
     * Уникальный набор задач Reduce для предотвращения дублирования
     */
    private final Set<ReduceTask> uniqueReduceTasks;

    public Coordinator(int countWorker, List<String> fileNames, int countReduce) {
        this.intermediateFiles = new ConcurrentHashMap<>();
        this.phaser = new Phaser();
        this.countWorker = intCheck(countWorker);
        this.executor = Executors.newFixedThreadPool(this.countWorker);
        this.tasksMap = new ConcurrentLinkedQueue<>();
        this.tasksReduce = new ConcurrentLinkedQueue<>();
        if (fileNames != null) {
            fileNames.forEach(fileName ->
                    this.tasksMap.add(new MapTask(fileName, intCheck(countReduce))));
        }
        this.uniqueReduceTasks = ConcurrentHashMap.newKeySet();

    }

    /**
     * Запускает выполнение задач Map и Reduce с помощью потоков.
     */
    public void run() {
        for (int i = 0; i < this.countWorker; i++) {
            executor.execute(new Worker(this));
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public Map<Integer, List<String>> getIntermediateFiles() {
        return intermediateFiles;
    }

    /**
     * Возвращает следующую задачу Map из очереди.
     *
     * @return задача MapTask или null, если очередь пуста
     */
    public MapTask getTasksMap() {
        return tasksMap.poll();
    }

    /**
     * Возвращает следующую задачу Reduce из очереди.
     *
     * @return задача ReduceTask или null, если очередь пуста
     */
    public ReduceTask getTasksReduce() {
        return tasksReduce.poll();
    }

    /**
     * Добавляет задачу Reduce в очередь, если она уникальна.
     *
     * @param reduceTask задача Reduce для добавления
     */
    public void addTasksReduce(ReduceTask reduceTask) {
        if (uniqueReduceTasks.add(reduceTask)) {
            tasksReduce.add(reduceTask);
        }
    }

    public Phaser getPhaser() {
        return phaser;
    }
}