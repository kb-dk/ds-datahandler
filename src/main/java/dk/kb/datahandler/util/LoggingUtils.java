package dk.kb.datahandler.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class LoggingUtils {

    public static void writeToFile(String toWrite, String fileName) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.ROOT).format(new java.util.Date());

        String timestampedContent = timeStamp + ": " + toWrite;
        Path path = Paths.get(fileName);
        byte[] strToBytes = timestampedContent.getBytes(StandardCharsets.UTF_8);

        Files.write(path, strToBytes);

    }
}
