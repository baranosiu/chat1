package local.pbaranowski.chat.server;

import local.pbaranowski.chat.commons.Constants;
import local.pbaranowski.chat.commons.transportlayer.MessageInternetFrame;
import local.pbaranowski.chat.commons.transportlayer.Transcoder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.synchronizedMap;

@Slf4j
class DiskFileStorage implements FileStorage {
    private final Transcoder<MessageInternetFrame> frameTranscoder;
    private final Map<String, FileStorageRecord> filesUploaded = synchronizedMap(new HashMap<>());
    private final Map<String, FileStorageRecord> filesInProgress = synchronizedMap(new HashMap<>());

    DiskFileStorage(Transcoder<MessageInternetFrame> transcoder) {
        File storage = new File(Constants.FILE_STORAGE_DIR);
        if (!storage.isDirectory()) {
            storage.mkdirs();
        }
        frameTranscoder = transcoder;
    }

    @Override
    public void append(String key, byte[] data) {
        if (filesInProgress.containsKey(key)) {
            File file = new File(Constants.FILE_STORAGE_DIR
                    + File.separator
                    + filesInProgress.get(key).getDiskFilename());
            try (FileOutputStream fileOutputStream = new FileOutputStream(file, true)) {
                fileOutputStream.write(data);
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
    }


    @Override
    public synchronized void publish(String key) throws MaxFilesExceededException {
        try {
            String uploadedKey = createUniqueFileKey();
            filesUploaded.put(uploadedKey, filesInProgress.get(key));
        } catch (MaxFilesExceededException excededException) {
            new File(filesInProgress.get(key).getDiskFilename()).delete();
            throw excededException;
        } finally {
            filesInProgress.remove(key);
        }
    }

    @SneakyThrows
    @Override
    public void delete(String key) {
        if (key == null) return;
        FileStorageRecord file = filesUploaded.get(key);
        if (file != null) {
            filesUploaded.remove(key);
            Files.delete(Paths.get(Constants.FILE_STORAGE_DIR + File.separator + file.getDiskFilename()));
        }
    }

    @Override
    public List<String> getFilesOnChannel(String channel) {
        return filesUploaded.keySet()
                .stream()
                .filter(key -> filesUploaded.get(key).getChannel().equals(channel))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteAllFilesOnChannel(String channel) {
        List<String> toDelete = new LinkedList<>();
        for (String fileId : filesUploaded.keySet()) {
            FileStorageRecord fileRecord = filesUploaded.get(fileId);
            if (fileRecord.getChannel().equals(channel)) {
                toDelete.add(fileId);
            }
        }
        toDelete.forEach(this::delete);
    }

    @Override
    public synchronized String requestNewKey(String userName, String channel, String fileName) throws MaxFilesExceededException {
        String key = createTmpFileKey();
        filesInProgress.put(key, new FileStorageRecord(userName, channel, fileName, UUID.randomUUID().toString()));
        return key;
    }

    @Override
    public String getSender(String key) {
        return filesUploaded.get(key).getSender();
    }

    @Override
    public String getChannel(String key) {
        return filesUploaded.get(key).getChannel();
    }

    @Override
    public String getOriginalFileName(String key) {
        return filesUploaded.get(key).getFilename();
    }

    @SneakyThrows
    public InputStream getFile(String key) {
        return new FileInputStream(Constants.FILE_STORAGE_DIR + File.separator + filesUploaded.get(key).getDiskFilename());
    }


    private String createUniqueFileKey() throws MaxFilesExceededException {
        for (int i = 1; i <= Constants.MAX_NUMBER_OF_FILES_IN_STORAGE; i++) {
            String key = Integer.toString(i);
            if (!filesUploaded.containsKey(key))
                return key;
        }
        throw new MaxFilesExceededException();
    }

    private String createTmpFileKey() throws MaxFilesExceededException {
        for (int i = 1; i <= Constants.MAX_NUMBER_OF_FILES_IN_STORAGE; i++) {
            String key = Integer.toString(i);
            if (!filesInProgress.containsKey(key))
                return key;
        }
        throw new MaxFilesExceededException();
    }

}

