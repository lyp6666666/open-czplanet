import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class MavenWrapperDownloader {

    private static final String WRAPPER_PROPERTIES = ".mvn/wrapper/maven-wrapper.properties";
    private static final String WRAPPER_JAR = ".mvn/wrapper/maven-wrapper.jar";

    public static void main(String[] args) throws Exception {
        Path baseDir = Path.of(System.getProperty("maven.multiModuleProjectDirectory", "."));
        Path propertiesPath = baseDir.resolve(WRAPPER_PROPERTIES);
        Path jarPath = baseDir.resolve(WRAPPER_JAR);

        if (Files.exists(jarPath)) {
            return;
        }
        Files.createDirectories(jarPath.getParent());

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(propertiesPath)) {
            props.load(in);
        }
        String wrapperUrl = props.getProperty("wrapperUrl");
        if (wrapperUrl == null || wrapperUrl.isBlank()) {
            throw new IllegalStateException("Missing wrapperUrl in " + WRAPPER_PROPERTIES);
        }
        download(wrapperUrl, jarPath.toFile());
    }

    private static void download(String url, File destination) throws IOException {
        try (InputStream in = new URL(url).openStream(); FileOutputStream out = new FileOutputStream(destination)) {
            in.transferTo(out);
        }
    }
}

