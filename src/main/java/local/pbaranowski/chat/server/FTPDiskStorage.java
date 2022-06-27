package local.pbaranowski.chat.server;

import local.pbaranowski.chat.constants.Constants;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static java.util.Collections.synchronizedMap;
import static local.pbaranowski.chat.constants.Constants.FILE_STORAGE_DIR;
import static local.pbaranowski.chat.constants.Constants.FILE_TRANSFER_COMPLETED;

@Slf4j
public class FTPDiskStorage implements FTPStorage {
    private final Map<String, FTPFileRecord> filesUploaded = synchronizedMap(new HashMap<>());
    private final Map<String, FTPFileRecord> filesInProgress = synchronizedMap(new HashMap<>());

    public FTPDiskStorage() {
        File storage = new File(FILE_STORAGE_DIR);
        if (!storage.isDirectory()) {
            storage.mkdirs();
        }
    }

    //format payload dla MESSAGE_APPEND_FILE: blokBase64 [spacja] FTPClientUtils.fileToKeyString()
    @Override
    public void appendFile(Message message) throws MaxFilesExceededException {
        String[] fields = message.getPayload().split("[ ]+", 2);
        if (!filesInProgress.containsKey(FTPClientUtils.fileToKeyString(message.getSender(), message.getReceiver(), fields[1]))) {
            filesInProgress.put(FTPClientUtils.fileToKeyString(message.getSender(), message.getReceiver(), fields[1]),
                    new FTPFileRecord(message.getSender(), message.getReceiver(), fields[1], UUID.randomUUID().toString()));
        }
        File file = new File(FILE_STORAGE_DIR
                + File.separator
                + filesInProgress.get(FTPClientUtils.fileToKeyString(message.getSender(), message.getReceiver(), fields[1])).getDiskFilename());
        if (fields[0].equals(FILE_TRANSFER_COMPLETED)) {
            uploadDone(message);
        } else
            try (FileOutputStream fileOutputStream = new FileOutputStream(file, true)) {
                fileOutputStream.write(java.util.Base64.getDecoder().decode(fields[0]));

            } catch (IOException e) {
                log.error(e.getMessage());
            }
    }

    @Override
    public void uploadDone(Message message) throws MaxFilesExceededException {
        String[] fields = message.getPayload().split("[ ]+", 2);
        String inProgressKey = FTPClientUtils.fileToKeyString(message.getSender(), message.getReceiver(), fields[1]);
        try {
            String uploadedKey = createUniqueFileKey();
            filesUploaded.put(uploadedKey, filesInProgress.get(inProgressKey));
        } catch (MaxFilesExceededException excededException) {
            new File(filesInProgress.get(inProgressKey).getDiskFilename()).delete();
            throw excededException;
        } finally {
            filesInProgress.remove(inProgressKey);
        }
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

    @SneakyThrows
    private void deleteFile(String fileId) {
        if (fileId == null) return;
        FTPFileRecord file = filesUploaded.get(fileId);
        if (file != null) {
            filesUploaded.remove(fileId);
            Files.delete(Paths.get(FILE_STORAGE_DIR + File.separator + file.getDiskFilename()));
        }
    }

    @Override
    public void deleteAllFilesOnChannel(Message message) {
        List<String> toDelete = new LinkedList<>();
        for (String fileId : filesUploaded.keySet()) {
            FTPFileRecord fileRecord = filesUploaded.get(fileId);
            if (fileRecord.getChannel().equals(message.getReceiver())) {
                toDelete.add(fileId);
            }
        }
        toDelete.forEach(this::deleteFile);
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


    private synchronized String createUniqueFileKey() throws MaxFilesExceededException {
        for (int i = 1; i <= Constants.MAX_NUMBER_OF_FILES_IN_STORAGE; i++) {
            String key = Integer.toString(i);
            if (!filesUploaded.containsKey(key))
                return key;
        }
        throw new MaxFilesExceededException();
    }

}
