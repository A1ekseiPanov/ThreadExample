package ru.panov;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        RingBuffer1<Integer> buffer = new RingBuffer1<>(4);
        new Thread(() -> {
            for (int i = 0; i < 30; i++) {

                ;

                System.out.println("Добавление числа: " + buffer.add(i) +"; Стало: " + Arrays.toString(buffer.getArray()));
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        new Thread(() -> {
            for (int i = 0; i < 30; i++) {

                try {
                    Thread.sleep(50);

                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("Удаление числа: " + buffer.remove() + ". Стало: " + Arrays.toString(buffer.getArray()));

            }
        }).start();
    }
}
