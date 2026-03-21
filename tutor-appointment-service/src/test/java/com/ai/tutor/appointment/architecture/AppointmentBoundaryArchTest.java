package com.ai.tutor.appointment.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AppointmentBoundaryArchTest {

    @Test
    void controllersShouldNotDependOnMapperTypes() throws IOException {
        Path controllerRoot = Path.of("src/main/java/com/ai/tutor/appointment/controller");
        try (Stream<Path> pathStream = Files.walk(controllerRoot)) {
            List<String> mapperDependencies = pathStream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith("Controller.java"))
                    .filter(path -> !path.getFileName().toString().startsWith("Internal"))
                    .filter(this::containsMapperImport)
                    .map(controllerRoot::relativize)
                    .map(Path::toString)
                    .collect(Collectors.toList());

            assertTrue(mapperDependencies.isEmpty(),
                    () -> "Controllers must not depend on mapper types: " + mapperDependencies);
        }
    }

    private boolean containsMapperImport(Path controllerFile) {
        try {
            return Files.readString(controllerFile, StandardCharsets.UTF_8)
                    .contains(".mapper.");
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + controllerFile, e);
        }
    }
}
