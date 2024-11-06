import exceptions.FileWorkException;
import model.ReduceTask;
import org.junit.jupiter.api.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class WorkerTest {
    private static final String HEAD = "mr-";
    private static final String FILE_NAME_1 ="file1.txt";
    private static final String FILE_NAME_2 ="file2.txt";
    private static final String FILE_NAME_3 ="file3.txt";
    private static final int COUNT_WORKER = 3;
    private final int COUNT_REDUCE = 2;
    private Worker worker;
    private Coordinator coordinator;
    private static final Path PATH = Path.of("");
    private List<String> fileNames;

    @BeforeEach
    public void setUp() {
        createTestFile(FILE_NAME_1, "word1 word2 word2 word3");
        createTestFile(FILE_NAME_2, "word1 word2 word2 word3");
        createTestFile(FILE_NAME_3, "word3 word4 word5 word6");

        fileNames = Arrays.asList(FILE_NAME_1, FILE_NAME_2, FILE_NAME_3);
        coordinator = new Coordinator(COUNT_WORKER, fileNames, COUNT_REDUCE);
        worker = new Worker(coordinator);
    }

    private void createTestFile(String fileName, String content) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @AfterEach
    public void deleteMidFiles() throws IOException {
        Files.list(PATH)
                .filter(x -> x.getFileName().toString().startsWith(HEAD))
                .forEach(x -> {
                    try {
                        Files.deleteIfExists(x);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @AfterAll
    static void deleteTestFiles() throws IOException {
        Files.deleteIfExists(Paths.get(FILE_NAME_1));
        Files.deleteIfExists(Paths.get(FILE_NAME_2));
        Files.deleteIfExists(Paths.get(FILE_NAME_3));

        Files.list(PATH)
                .filter(x -> x.getFileName().toString().startsWith("kr-"))
                .forEach(x -> {
                    try {
                        Files.deleteIfExists(x);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Test
    void mapFunction() throws IOException {
        worker.mapFunction();

        List<Path> list = Files.list(PATH)
                .filter(x -> x.getFileName().toString().startsWith(HEAD))
                .toList();

        assertThat(list).isNotEmpty();
        assertThat(list.size()).isEqualTo(COUNT_WORKER * COUNT_REDUCE);
    }

    @Test
    void mapFunction_ConstructorParametersNotValid_WorkNotDone() throws IOException {
        Coordinator coordinator1 = new Coordinator(-1, null, -1);
        Worker worker1 = new Worker(coordinator1);

        worker1.mapFunction();

        List<Path> list = Files.list(PATH)
                .filter(x -> x.getFileName().toString().startsWith(HEAD))
                .toList();
        assertThat(list.size()).isEqualTo(0);
    }

    @Test
    void mapFunction_FilesNotFound_FileWorkException() {
        Coordinator coordinator1 = new Coordinator(COUNT_WORKER, new ArrayList<>(List.of("")), COUNT_REDUCE);
        Worker worker1 = new Worker(coordinator1);

        assertThrows(FileWorkException.class, worker1::mapFunction);
    }


    @Test
    void reduceFunction() throws IOException {
        List<String> expectedList = List.of("word1 2", "word2 4", "word3 3", "word4 1", "word5 1", "word6 1");

        worker.mapFunction();
        coordinator.getIntermediateFiles()
                .forEach((k, v) -> coordinator.addTasksReduce(new ReduceTask(k, v)));
        worker.reduceFunction();

        List<String> lines = new ArrayList<>();
        Files.list(PATH)
                .filter(x -> x.getFileName().toString().startsWith("kr-"))
                .forEach(x -> {
                    try {
                        Files.lines(x).forEach(lines::add);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

        assertThat(lines).isNotEmpty();
        assertThat(lines).containsExactlyInAnyOrderElementsOf(expectedList);
    }
}