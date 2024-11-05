package model;

import java.util.List;
import java.util.Objects;

public class ReduceTask {
    private int reduceNumberTask;
    private List<String> files;

    public ReduceTask(int reduceNumberTask, List<String> files) {
        this.reduceNumberTask = reduceNumberTask;
        this.files = files;
    }

    public int getReduceNumberTask() {
        return reduceNumberTask;
    }

    public List<String> getFiles() {
        return files;
    }

    @Override
    public String toString() {
        return "model.ReduceTask{" +
                "reduceNumberTask=" + reduceNumberTask +
                ", files=" + files +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReduceTask that = (ReduceTask) o;
        return reduceNumberTask == that.reduceNumberTask && Objects.equals(files, that.files);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reduceNumberTask, files);
    }
}