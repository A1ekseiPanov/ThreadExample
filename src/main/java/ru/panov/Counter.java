package ru.panov;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Класс {@code Counter} представляет собой синхронизированный счетчик, который используется
 * несколькими потоками. Счетчик увеличивается потоками в зависимости от четности его текущего значения.
 * Один поток обрабатывает четные значения, другой — нечетные.
 *
 * <p>Потоки чередуются в зависимости от значения счетчика: если счетчик четный, он обрабатывается одним потоком,
 * если нечетный — другим.</p>
 */
public class Counter {
    /**
     * Максимальное значение, до которого должен увеличиваться счетчик
     */
    private final Long maxValue;
    private static final AtomicLong count = new AtomicLong(0);

    public Counter(Long maxValue) {
        this.maxValue = maxValue;
    }

    /**
     * Метод {@code increase} увеличивает значение счетчика на единицу в зависимости от четности текущего значения.
     * Поток, который вызывает этот метод, будет либо увеличивать значение, либо ожидать, пока другой поток
     * выполнит свое действие.
     *
     * <p>Если текущий поток должен обрабатывать четные значения, он выполнит инкремент только если значение
     * счетчика четное. Аналогично, для нечетных значений, поток будет ждать, пока значение не станет подходящим.</p>
     *
     * <p>Используется синхронизация для корректной работы с общим счетчиком. Если условие для текущего потока
     * не выполняется, поток переходит в состояние ожидания с помощью метода {@code wait()}. Когда другой поток
     * изменяет значение счетчика, он уведомляет все ожидающие потоки с помощью {@code notifyAll()}.</p>
     *
     * @param isEven если {@code true}, то поток обрабатывает только четные значения, если {@code false} — нечетные
     * @throws InterruptedException если поток был прерван во время ожидания
     */
    public synchronized void increase(boolean isEven) throws InterruptedException {
        while (count.get() < maxValue) {
            if ((count.get() % 2 == 0)==isEven) {
                Thread.sleep(1000);
                System.out.println(Thread.currentThread().getName() + " " + count);
                count.incrementAndGet();
                notifyAll();
            } else {
                wait();
            }
        }
    }
}