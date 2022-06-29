package local.pbaranowski.chat.server;

import local.pbaranowski.chat.commons.Constants;
import local.pbaranowski.chat.commons.MessageType;
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
    public void appendFile(Message message) throws MaxFilesExceededException {
        MessageInternetFrame frame;
        synchronized (frameTranscoder) {
            frame = frameTranscoder.decodeObject(message.getPayload(), MessageInternetFrame.class);
        }
        String inProgressKey = DiskFileStorageUtils.fileToKeyString(message.getSender(), frame.getDestinationName(), frame.getSourceName());
        if (!filesInProgress.containsKey(inProgressKey)) {
            filesInProgress.put(inProgressKey,
                    new FileStorageRecord(message.getSender(), frame.getDestinationName(), frame.getSourceName(), UUID.randomUUID().toString()));
        }
        File file = new File(Constants.FILE_STORAGE_DIR
                + File.separator
                + filesInProgress.get(inProgressKey).getDiskFilename());
        if (frame.getMessageType() == MessageType.MESSAGE_PUBLISH_FILE) {
            uploadDone(message);
        } else
            try (FileOutputStream fileOutputStream = new FileOutputStream(file, true)) {
                fileOutputStream.write(frame.getData());
            } catch (IOException e) {
                log.error(e.getMessage());
            }
    }

    public void uploadDone(Message message) throws MaxFilesExceededException {
        String[] fields = message.getPayload().split("[ ]+", 2);
        MessageInternetFrame frame;
        synchronized (frameTranscoder) {
            frame = frameTranscoder.decodeObject(message.getPayload(), MessageInternetFrame.class);
        }
        String inProgressKey = DiskFileStorageUtils.fileToKeyString(message.getSender(), frame.getDestinationName(), frame.getSourceName());
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
            FileStorageRecord file = filesUploaded.get(fileId);
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
        FileStorageRecord file = filesUploaded.get(fileId);
        if (file != null) {
            filesUploaded.remove(fileId);
            Files.delete(Paths.get(Constants.FILE_STORAGE_DIR + File.separator + file.getDiskFilename()));
        }
    }

    @Override
    public void deleteAllFilesOnChannel(Message message) {
        List<String> toDelete = new LinkedList<>();
        for (String fileId : filesUploaded.keySet()) {
            FileStorageRecord fileRecord = filesUploaded.get(fileId);
            if (fileRecord.getChannel().equals(message.getReceiver())) {
                toDelete.add(fileId);
            }
        }
        toDelete.forEach(this::deleteFile);
    }

    @Override
    public Map<String, FileStorageRecord> getFilesOnChannel(String channel) {
        return filesUploaded.keySet()
                .stream()
                .filter(fileKey -> filesUploaded.get(fileKey).getChannel().equals(channel))
                .collect(Collectors.toMap(fileKey -> fileKey, filesUploaded::get, (fileId, fileRecord) -> fileRecord));
    }

    @SneakyThrows
    @Override
    public InputStream getFile(Message message) {
        for (String fileKey : filesUploaded.keySet()) {
            if (fileKey.equals(message.getPayload().split("[ ]+")[0])) {
                return new FileInputStream(Constants.FILE_STORAGE_DIR + File.separator + filesUploaded.get(fileKey).getDiskFilename());
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
