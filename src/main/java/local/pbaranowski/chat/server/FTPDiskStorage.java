package local.pbaranowski.chat.server;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.*;

import static java.util.Collections.synchronizedMap;

@Slf4j
public class FTPDiskStorage implements FTPStorage {
    private static final String FILE_STORAGE_DIR = "storage";
    private Map<String, FTPFileRecord> filesUploaded = synchronizedMap(new HashMap<>());
    private Map<String, FTPFileRecord> filesInProgress = synchronizedMap(new HashMap<>());

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
        if (!filesInProgress.containsKey(FTPClientUtils.fileToKeyString(message.getSender(), message.getReceiver(), fields[1]))) {
            filesInProgress.put(FTPClientUtils.fileToKeyString(message.getSender(), message.getReceiver(), fields[1]),
                    new FTPFileRecord(message.getSender(), message.getReceiver(), fields[1], UUID.randomUUID().toString()));
        }
        File file = new File(FILE_STORAGE_DIR
                + File.separator
                + filesInProgress.get(FTPClientUtils.fileToKeyString(message.getSender(), message.getReceiver(), fields[1])).getDiskFilename());
        if (fields[0].equals("C")) {
            uploadDone(message);
        } else
            try (FileOutputStream fileOutputStream = new FileOutputStream(file, true)) {
                fileOutputStream.write(java.util.Base64.getDecoder().decode(fields[0]));
            }
    }

    @Override
    public void uploadDone(Message message) {
        String fields[] = message.getPayload().split("[ ]+", 2);
        String inProgressKey = FTPClientUtils.fileToKeyString(message.getSender(), message.getReceiver(), fields[1]);
        filesUploaded.put(createUniqueFileKey(), filesInProgress.get(inProgressKey));
        filesInProgress.remove(inProgressKey);
    }


    @Override
    public void deleteFile(Message message) {
        for (String fileId : filesUploaded.keySet()) {
            FTPFileRecord file = filesUploaded.get(fileId);
            if (!file.getSender().equals(message.getSender()))
                continue;
            if (fileId.equals(message.getPayload())) {
                deleteFile(fileId);
                return;
            }
        }
    }

    private void deleteFile(String fileId) {
        if(fileId == null) return;
        FTPFileRecord file = filesUploaded.get(fileId);
        if(file != null) {
            filesUploaded.remove(fileId);
            new File(FILE_STORAGE_DIR + File.separator + file.getDiskFilename()).delete();
        }
    }
    @Override
    public void deleteAllFilesOnChannel(Message message) {
        List<String> toDelete = new LinkedList<>();
        for(String fileId : filesUploaded.keySet()) {
            FTPFileRecord fileRecord = filesUploaded.get(fileId);
            if(fileRecord.getChannel().equals(message.getReceiver())) {
                toDelete.add(fileId);
            }
        }
        toDelete.forEach(fileId -> deleteFile(fileId));
    }

    @Override
    public Map<String, FTPFileRecord> getFilesOnChannel(String channel) {
        Map<String, FTPFileRecord> filesOnChannel = new HashMap<>();
        for (String fileKey : filesUploaded.keySet()) {
            if (filesUploaded.get(fileKey).getChannel().equals(channel)) {
                filesOnChannel.put(fileKey, filesUploaded.get(fileKey));
            }
        }
        return filesOnChannel;
    }

    @SneakyThrows
    @Override
    public InputStream getFile(Message message) {
        for (String fileKey : filesUploaded.keySet()) {
            if (fileKey.equals(message.getPayload().split("[ ]+")[0])) {
                return new FileInputStream(FILE_STORAGE_DIR + File.separator + filesUploaded.get(fileKey).getDiskFilename());
            }
        }
        return null;
    }


    private synchronized String createUniqueFileKey() {
        for(int i=1; i <= 2048; i++) { // TODO: Max files on server
            String key = Integer.valueOf(i).toString();
            if(!filesUploaded.containsKey(key))
                return key;
        }
        throw new RuntimeException("Max files exceed");
    }

}
