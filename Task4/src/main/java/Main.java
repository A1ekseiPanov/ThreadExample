import java.util.ArrayList;
import java.util.List;

import static util.Constants.*;

public class Main {
    public static void main(String[] args) {

        List<String> fileNmaes = new ArrayList<>(List.of(PATH1, PATH2, PATH3));

        Coordinator coordinator = new Coordinator(
                2, fileNmaes, 8);
        coordinator.run();
    }
}