package model;

import java.io.Serial;
import java.io.Serializable;

/**
 * Класс KeyValue представляет собой модель данных, состоящую из пары ключ-значение.
 * Реализует интерфейс Serializable для поддержки сериализации объектов.
 */
public class KeyValue implements Serializable {
    @Serial
    private static final long serialVersionUID = 1;

    /**
     * Поле ключа, представленное строкой
     */
    private final String key;

    /**
     * Поле значения, представленное целым числом
     */
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