package local.pbaranowski.chat.server;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.*;

import static java.util.Collections.synchronizedMap;

@Slf4j
public class FTPDiskStorage implements FTPStorage {
    private static final String FILE_STORAGE_DIR = "storage";
    private Map<String, FTPFileRecord> files = synchronizedMap(new HashMap<>());

    public FTPDiskStorage() {
        File storage = new File(FILE_STORAGE_DIR);
        if (!storage.isDirectory()) {
            storage.mkdirs();
        }
    }

    @Override
    @SneakyThrows
    public void appendFile(Message message) {
        String fields[] = message.getPayload().split("[ ]+", 2);
        if (!files.containsKey(FTPClientUtils.fileToKeyString(message.getSender(), message.getReceiver(), fields[1]))) {
            files.put(FTPClientUtils.fileToKeyString(message.getSender(), message.getReceiver(), fields[1]),
                    new FTPFileRecord(message.getSender(), message.getReceiver(), fields[1], UUID.randomUUID().toString()));
        }
        File file = new File(FILE_STORAGE_DIR
                + File.separator
                + files.get(FTPClientUtils.fileToKeyString(message.getSender(), message.getReceiver(), fields[1])).getDiskFilename());
        try (FileOutputStream fileOutputStream = new FileOutputStream(file, true)) {
            fileOutputStream.write(java.util.Base64.getDecoder().decode(fields[0]));
        }
    }


    @Override
    public void removeFile(Message message) {
        for (FTPFileRecord fileRecord : files.values()) {
            if(!fileRecord.getSender().equals(message.getSender()))
                continue;
            if (fileRecord.getDiskFilename().startsWith(message.getPayload())) {
                new File(FILE_STORAGE_DIR+File.separator+fileRecord.getDiskFilename()).delete();
                files.remove(FTPClientUtils.fileToKeyString(fileRecord.getSender(),fileRecord.getChannel(),fileRecord.getFilename()));
                return;
            }
        }
    }

    @Override
    public List<FTPFileRecord> getFilesOnChannel(String channel) {
        List<FTPFileRecord> filesOnChannel = new LinkedList<>();
        //TODO: Jeśli zostanie hashMap, to zamienić na stream()->filter()
        for (FTPFileRecord file : files.values()) {
            if (file.getChannel().equals(channel)) {
                filesOnChannel.add(file);
            }
            log.info("getFilesOnChannel {} {} {}", file.getSender(), file.getChannel(), file.getFilename());
        }
        return filesOnChannel;
    }

    @SneakyThrows
    @Override
    public InputStream getFile(Message message) {
        for (FTPFileRecord fileRecord : files.values()) {
            if (fileRecord.getDiskFilename().startsWith(message.getPayload().split("[ ]+")[0])) {
                return new FileInputStream(FILE_STORAGE_DIR + File.separator + fileRecord.getDiskFilename());
            }
        }
        return null;
    }

}
