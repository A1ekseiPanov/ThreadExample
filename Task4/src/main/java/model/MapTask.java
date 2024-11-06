package model;

import java.util.UUID;

/**
 * Класс MapTask представляет map задачу.
 * Каждая задача включает уникальный идентификатор, имя файла, с которым необходимо работать,
 * и количество reduce задач, на которые будет разделен результат.
 */
public class MapTask {
    /**
     * Уникальный идентификатор задачи
     */
    private final UUID taskId;

    /**
     * Имя файла, с которым связана задача
     */
    private final String fileName;

    /**
     * Количество reduce задач, на которые будет разделен результат
     */
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