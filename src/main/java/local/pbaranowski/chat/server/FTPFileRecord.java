package local.pbaranowski.chat.server;

import lombok.Value;

@Value
public class FTPFileRecord {
    String sender;
    String channel;
    String filename; // Nazwa nadana przez użytkownika wysyłającego
    String diskFilename; // Nazwa pod jaką plik jest przechowywany w storage
}
