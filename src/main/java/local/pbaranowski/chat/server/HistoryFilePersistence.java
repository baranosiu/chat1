package local.pbaranowski.chat.server;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.Iterator;

@Slf4j
public class HistoryFilePersistence implements HistoryPersistence {
    private static final String fileName = "history.txt";
    private static File file = new File(fileName);

    @SneakyThrows
    @Override
    public synchronized void save(String user, String text) {
        if(!file.exists()) {
            file.createNewFile();
        }
        FileWriter fileWriter = new FileWriter(file,true);
        fileWriter.append(user + ":"+text+System.lineSeparator());
        fileWriter.close();
    }

    @SneakyThrows
    @Override
    public Iterator<String> retrieve(String user) {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        return new BufferedReaderIterable(bufferedReader).iterator();
    }
}
