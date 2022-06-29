package local.pbaranowski.chat.server;

import lombok.Value;

// TODO: To trzeba będzie przerobić w taki sposób, aby FTPClient nie musiał wiedzieć jaka jest struktura danych w storage
// czyli odpytywanie metodami na podstawie klucza o poszczególne dane a nie poprzez przekazywanie rekordu
@Value
class FileStorageRecord {
    String sender;
    String channel;
    String filename; // Nazwa nadana przez użytkownika wysyłającego
    String diskFilename; // Nazwa pod jaką plik jest przechowywany w storage
}
