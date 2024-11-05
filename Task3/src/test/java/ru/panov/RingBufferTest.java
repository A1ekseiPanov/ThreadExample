package ru.panov;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class RingBufferTest {

    private RingBuffer<Integer> ringBuffer;
    private final int capacity = 5;

    @BeforeEach
    public void setUp() {
        ringBuffer = new RingBuffer<>(capacity);
    }

    @Test
    @DisplayName("Добавление null должно вызвать NullPointerException")
    public void addNull_throwNullPointerException() {
        assertThrows(NullPointerException.class, () -> ringBuffer.add(null));
    }

    @Test
    @DisplayName("Добавление и удаление одного элемента")
    public void addAndRemove() {
        Integer add = ringBuffer.add(1);

        assertEquals(ringBuffer.getArray()[0], add);

        Integer remove = ringBuffer.remove();

        assertEquals(add, remove);
        assertFalse(Arrays.stream(ringBuffer.getArray()).toList().contains(remove));
    }

    @Test
    @DisplayName("Добавление и удаление нескольких элементов")
    public void addAndRemove_MultipleElements() {
        ringBuffer.add(1);
        ringBuffer.add(2);
        ringBuffer.add(3);

        assertEquals(1, ringBuffer.remove());
        assertEquals(2, ringBuffer.remove());
        assertEquals(3, ringBuffer.remove());
    }

    @Test
    @DisplayName("Перезапись в буфере при переполнении")
    public void bufferOverwrite() {
        ringBuffer.add(1);
        ringBuffer.add(2);
        ringBuffer.add(3);
        ringBuffer.add(4);
        ringBuffer.add(5);
        ringBuffer.add(6);

        assertEquals(2, ringBuffer.remove());
        assertEquals(3, ringBuffer.remove());
        assertEquals(4, ringBuffer.remove());
        assertEquals(5, ringBuffer.remove());
        assertEquals(6, ringBuffer.remove());
    }

    @Test
    @DisplayName("Удаление из пустого буфера должно вызвать RuntimeException")
    public void bufferIsEmpty_throwRuntimeException() {
        assertThrows(RuntimeException.class, () -> {
            Thread.currentThread().interrupt();
            ringBuffer.remove();
        });
    }

    @Test
    @DisplayName("Проверка вместимости буфера")
    public void testBufferCapacity() {
        assertEquals(5, ringBuffer.getArray().length);
    }

    @Test
    @DisplayName("Получение буфера")
    public void getArray() {
        Integer i1 = ringBuffer.add(1);
        Integer i2 = ringBuffer.add(2);

        Object[] currentBuffer = ringBuffer.getArray();

        assertEquals(i1, currentBuffer[0]);
        assertEquals(i2, currentBuffer[1]);
    }

    @Test
    @DisplayName("Многопоточное добавление элементов")
    public void multithreadedAdd() throws Exception {
        int threads = 5;
        int elements = 10;

        ExecutorService executor = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                for (int j = 0; j < elements / threads; j++) {
                    ringBuffer.add(threadId * elements + j);
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);

        Object[] bufferContent = ringBuffer.getArray();
        assertNotNull(bufferContent);

        for (Object obj : bufferContent) {
            assertNotNull(obj);
        }
    }

    @Test
    @DisplayName("Многопоточное удаление элементов")
    public void multithreadedRemove() throws Exception {
        int threads = 5;
        int elements = 10;

        ExecutorService executor = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < elements; i++) {
            ringBuffer.add(i);
        }

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                for (int j = 0; j < capacity / threads; j++) {
                    ringBuffer.remove();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);

        Object[] bufferContent = ringBuffer.getArray();
        for (Object obj : bufferContent) {
            assertNull(obj);
        }
    }

    @Test
    @DisplayName("Многопоточное добавление и удаление элементов")
    public void multithreadedAddAndRemove() throws Exception {
        int elements = 100;
        int threads = 5;

        ExecutorService executor = Executors.newCachedThreadPool();

        for (int i = 0; i < threads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                for (int j = 0; j < elements / threads; j++) {
                    ringBuffer.add(threadId + elements + j);
                }
            });
        }

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                for (int j = 0; j < capacity; j++) {

                    ringBuffer.remove();

                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);

        Object[] bufferContent = ringBuffer.getArray();

        for (Object obj : bufferContent) {
            assertNull(obj);
        }
    }
}