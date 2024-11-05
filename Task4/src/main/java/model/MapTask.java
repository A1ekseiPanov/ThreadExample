package model;

import java.util.UUID;

public class MapTask {
    private final UUID taskId;
    private final String fileName;
    private final int countReduce;

    public MapTask(String fileName, int countReduce) {
        this.taskId = UUID.randomUUID();
        this.fileName = fileName;
        this.countReduce = countReduce;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public String getFileName() {
        return fileName;
    }

    public int getCountReduce() {
        return countReduce;
    }
}