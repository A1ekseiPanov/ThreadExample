package ru.panov;


public class Main {
    public static void main(String[] args) {
        Counter counter = new Counter(10L);
        Thread t1 = new Thread(()-> {
            try {
                counter.increase(true);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        Thread t2 = new Thread(()-> {
            try {
                counter.increase(false);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        t1.start();
        t2.start();
    }
}