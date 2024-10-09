package ru.panov.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Утилитный класс для работы с файлами свойств, такими как `application.properties`.
 * <p>
 * Класс загружает свойства из файла при инициализации и предоставляет возможность
 * доступа к значениям по ключу.
 * <p>
 * Свойства загружаются автоматически при загрузке класса благодаря статическому блоку.
 */
public class PropertiesUtil {
    public static final Properties PROPERTIES = new Properties();

    private PropertiesUtil() {
    }

    static {
        loadProperties();
    }

    /**
     * Получает значение свойства по указанному ключу.
     *
     * @param key ключ для поиска значения
     * @return значение, связанное с указанным ключом, или {@code null}, если ключ не найден
     */
    public static String get(String key) {
        return PROPERTIES.getProperty(key);
    }

    /**
     * Загружает свойства из файла `application.properties`, расположенного в корне
     * ресурсов (например, в classpath).
     * <p>
     * Если файл не найден или возникает ошибка при загрузке, выбрасывается {@link RuntimeException}.
     */
    private static void loadProperties() {
        try (InputStream inputStream = PropertiesUtil.class
                .getClassLoader().getResourceAsStream("application.properties")) {
            PROPERTIES.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}