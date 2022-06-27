package local.pbaranowski.chat.server;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.Iterator;

import static local.pbaranowski.chat.constants.Constants.*;

@Slf4j
@RequiredArgsConstructor
public class HistoryFilePersistence implements HistoryPersistence {
    private final LogSerializer logSerializer;
    private static final File file = new File(HISTORY_FILE_NAME);

    @SneakyThrows
    @Override
    public synchronized void save(Message message) {
        if (!file.exists()) {
            file.createNewFile();
        }
        FileWriter fileWriter = new FileWriter(file, true);
        fileWriter.append(logSerializer.fromMessageToString(message));
        fileWriter.close();
    }

    @SneakyThrows
    @Override
    public Iterator<String> retrieve(String user) {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        return new BufferedReaderIterable(bufferedReader).iterator();
    }
}
