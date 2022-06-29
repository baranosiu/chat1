package local.pbaranowski.chat.server;

import java.io.InputStream;
import java.util.Map;

interface FileStorage {
    void appendFile(Message message) throws MaxFilesExceededException; // Koniec pliku przez odpowiedni MessageType

    void deleteFile(Message message);

    // TODO: Przerobić aby zwracało tylko listę kluczy oraz dodać metody zwracające właściwości na podstawie klucza
    Map<String, FileStorageRecord> getFilesOnChannel(String channel);

    InputStream getFile(Message message);

    void deleteAllFilesOnChannel(Message message);
}
