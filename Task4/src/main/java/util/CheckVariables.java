package util;

/**
 * Утилитный класс CheckVariables предоставляет методы для проверки и валидации переменных.
 */
public final class CheckVariables {
    private CheckVariables() {
    }

    /**
     * Проверяет, является ли переданная целочисленная переменная положительной.
     * Если переменная меньше или равна нулю, возвращается значение по умолчанию - 1.
     *
     * @param variable целочисленная переменная для проверки
     * @return значение переменной, если она больше 0; иначе возвращает 1
     */
    public static int intCheck(int variable) {
        return variable > 0 ? variable : 1;
    }
}