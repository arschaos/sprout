package io.arschaos.scanner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class JavaSourceScanner {

    private static final Set<String> EXCLUDED_DIR_NAMES = Set.of(
            "target", ".git", ".idea", ".mvn", ".settings", "bin", "build", "node_modules"
    );

    /**
     * Discovers all Java source files under the given project directory.
     * Prefers `src/main/java` if present, falls back to `src`, and lastly scans the project root.
     */
    public List<File> scan(File projectDir) {
        List<File> files = new ArrayList<>();
        if (projectDir == null || !projectDir.exists()) {
            return files;
        }

        File srcMainJava = new File(projectDir, "src/main/java");
        if (srcMainJava.exists() && srcMainJava.isDirectory()) {
            collectJavaFiles(srcMainJava.toPath(), files);
            return files;
        }

        File srcDir = new File(projectDir, "src");
        if (srcDir.exists() && srcDir.isDirectory()) {
            collectJavaFiles(srcDir.toPath(), files);
            return files;
        }

        collectJavaFiles(projectDir.toPath(), files);
        return files;
    }

    private void collectJavaFiles(Path root, List<File> targetList) {
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !isExcluded(p))
                    .forEach(p -> targetList.add(p.toFile()));
        } catch (IOException e) {
            // best-effort scanning
        }
    }

    private boolean isExcluded(Path path) {
        for (Path part : path) {
            if (EXCLUDED_DIR_NAMES.contains(part.toString())) {
                return true;
            }
        }
        return false;
    }
}
