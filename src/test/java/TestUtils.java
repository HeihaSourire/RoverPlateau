import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestUtils {
    static Path toFile(String content) {
        try {
            Path p = Files.createTempFile("rover-", ".txt");
            Files.writeString(p, content);
            p.toFile().deleteOnExit();
            return p;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
