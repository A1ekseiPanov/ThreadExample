import model.MapTask;
import model.ReduceTask;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.*;

public class Coordinator {
    private final int countReduce;
    private final Phaser phaser;
    private final int countWorker;
    private final ExecutorService executor;
    private Queue<MapTask> tasksMap;
    private Queue<ReduceTask> tasksReduce;
    private Map<Integer, List<String>> intermediateFiles;
    private final Set<ReduceTask> uniqueReduceTasks;


    public Coordinator(int countWorker, List<String> fileNames, int countReduce) {
        this.intermediateFiles = new ConcurrentHashMap<>();
        this.countReduce = countReduce;
        this.phaser = new Phaser();
        this.countWorker = countWorker;
        this.executor = Executors.newFixedThreadPool(countWorker);
        this.tasksMap = new ConcurrentLinkedQueue<>();
        this.tasksReduce = new ConcurrentLinkedQueue<>();
        fileNames.forEach(fileName -> this.tasksMap.add(new MapTask(fileName, countReduce)));
        this.uniqueReduceTasks = ConcurrentHashMap.newKeySet();

    }

    public void run() {
        for (int i = 0; i < this.countWorker; i++) {
            executor.execute(new Worker(this, this.phaser));
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

    public MapTask getTasksMap() {
        return tasksMap.poll();
    }

    public ReduceTask getTasksReduce() {
        return tasksReduce.poll();
    }

    public void addTasksReduce(ReduceTask reduceTask) {
        if (uniqueReduceTasks.add(reduceTask)) {
            tasksReduce.add(reduceTask);
        }
    }
}