package model;

import java.io.Serial;
import java.io.Serializable;

public class KeyValue implements Serializable {
    @Serial
    private static final long serialVersionUID = 1;
    private final String key;
    private final Integer value;

    public KeyValue(String key) {
        this.key = key;
        this.value = 1;
    }

    public String getKey() {
        return key;
    }

    public Integer getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "model.KeyValue{" +
                "key='" + key + '\'' +
                ", value=" + value +
                '}';
    }
}