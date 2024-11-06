package exceptions;

/**
 * Класс исключения FileWorkException используется для обработки ошибок, связанных с работой с файлами.
 * Это исключение является unchecked (наследуется от RuntimeException) и может быть использовано
 * для перехвата и обработки ошибок ввода-вывода, таких как невозможность найти файл или ошибки чтения/записи.
 */
public class FileWorkException extends RuntimeException {
    public FileWorkException(Throwable cause) {
        super(cause);
    }

    public FileWorkException(String message, Throwable cause) {
        super(message, cause);
    }
}