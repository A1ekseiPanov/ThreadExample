package ru.panov;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RingBuffer<T> {
    private final Object[] buffer;
    private final int capacity;
    private final Lock lock;
    private final Condition empty;
    private int addFlag;
    private int removeFlag;
    private int count;


    public RingBuffer() {
        this.capacity = 7;
        this.buffer = new Object[this.capacity];
        this.lock = new ReentrantLock();
        this.empty = lock.newCondition();
        this.addFlag = 0;
        this.removeFlag = 0;
        this.count = 0;
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

            empty.signalAll();
            return t;

        } finally {
            lock.unlock();
        }
    }

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

    public Object[] getArray() {
        lock.lock();
        try {
            return  buffer.clone();
        } finally {
            lock.unlock();
        }
    }
}
