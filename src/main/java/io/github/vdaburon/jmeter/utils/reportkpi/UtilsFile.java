package io.github.vdaburon.jmeter.utils.reportkpi;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class UtilsFile {

    public static void writeFileUtf8(String fileName, String toWrite) throws java.io.IOException {
        Path path = Paths.get(fileName);
        BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
        writer.append(toWrite);
        writer.newLine();
        writer.close();
    }
}
