package ru.panov;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Класс RingBuffer представляет собой кольцевой буфер,
 * который поддерживает операции добавления и удаления элементов с блокировками
 * для обеспечения потокобезопасности.
 *
 * @param <T> Тип элементов, которые будут храниться в буфере.
 */
public class RingBuffer<T> {
    private final Object[] buffer;
    private final int capacity;
    private final Lock lock;
    private final Condition empty;
    private int addFlag;
    private int removeFlag;
    private int count;


    public RingBuffer() {
        this(7);
    }

    public RingBuffer(int capacity) {
        this.capacity = (capacity < 1) ? 7 : capacity;
        this.buffer = new Object[this.capacity];
        this.lock = new ReentrantLock();
        this.empty = lock.newCondition();
        this.addFlag = 0;
        this.removeFlag = 0;
        this.count = 0;
    }


    /**
     * Добавляет элемент в буфер.
     * Если буфер полон, новый элемент замещает самый старый элемент.
     *
     * @param t Элемент для добавления (не может быть null).
     * @return Добавленный элемент.
     * @throws NullPointerException Если передан null-элемент.
     */
    public T add(T t) {
        Objects.requireNonNull(t);
        lock.lock();
        try {
            buffer[addFlag] = t;
            addFlag = (addFlag + 1) % capacity;

            if (count == capacity) {
                removeFlag = (removeFlag + 1) % capacity;
            } else {
                count++;
            }

            empty.signal();
            return t;

        } finally {
            lock.unlock();
        }
    }

    /**
     * Удаляет и возвращает элемент из буфера.
     * Если буфер пуст, поток ожидает, пока не появится элемент.
     *
     * @return Удалённый элемент.
     * @throws RuntimeException Если поток был прерван во время ожидания.
     */
    public T remove() {
        lock.lock();
        try {
            while (count == 0) {
                empty.await();
            }
            T removedElement = (T) buffer[removeFlag];
            buffer[removeFlag] = null;

            removeFlag = (removeFlag + 1) % capacity;
            count--;
            return removedElement;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Возвращает копию текущего состояния буфера.
     *
     * @return Массив объектов, представляющий текущее состояние буфера.
     */
    public Object[] getArray() {
        lock.lock();
        try {
            return Arrays.copyOf(buffer, buffer.length);
        } finally {
            lock.unlock();
        }
    }
}