package local.pbaranowski.chat.client;

import local.pbaranowski.chat.server.MessageType;
import local.pbaranowski.chat.transportlayer.Base64Transcoder;
import local.pbaranowski.chat.transportlayer.MessageInternetFrame;
import local.pbaranowski.chat.transportlayer.Transcoder;
import lombok.SneakyThrows;

import java.io.*;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

import static java.util.Collections.synchronizedList;
import static local.pbaranowski.chat.constants.Constants.*;

public class Application implements Runnable {

    static class RequestedFiles {
        private final List<String> fileList = synchronizedList(new LinkedList<>());

        public void request(String file) {
            fileList.add(file);
        }

        public void downloaded(String file) {
            fileList.remove(file);
        }

        public boolean isRequested(String file) {
            return fileList.contains(file);
        }
    }

    private final BufferedReader socketReader;
    private final BufferedWriter socketWriter;
    private final RequestedFiles requestedFiles = new RequestedFiles();
    private final Transcoder<MessageInternetFrame> transcoder = new Base64Transcoder<>();
    private final Socket socket;

    public Application(String host, int port) throws IOException {
        socket = new Socket(host, port);
        socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        socketWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    @SneakyThrows
    private void consoleLoop() {
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            String line = console.readLine();
            if (line.equals("/q")) {
                socket.close();
                Runtime.getRuntime().exit(0);
                return;
            }
            if (line.startsWith("/uf ")) {
                uploadFile(line);
                continue;
            }
            if (line.startsWith("/df ")) {
                String[] fields = line.split("[ ]+");
                if (fields.length == 3) {
                    requestedFiles.request(fields[2]);
                }
            }
            write(line);
        }
    }

    private void uploadFile(String line) throws IOException {
        String[] fields = line.split("[ ]+", 3);
        if (fields.length != 3) {
            System.out.println("ERROR: Syntax error");
            return;
        }
        String channel = fields[1];
        String filename = fields[2];
        File file = new File(filename);
        if (!file.canRead()) {
            System.out.printf("Can't read from file %s%n", file.getName());
            return;
        }
        MessageInternetFrame frame = new MessageInternetFrame();
        frame.setMessageType(MessageType.MESSAGE_APPEND_FILE);
        frame.setSourceName(filename);
        frame.setDestinationName(channel);
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            while (fileInputStream.available() > 0) {
                byte[] data = fileInputStream.readNBytes(256);
                frame.setData(data);
                synchronized (transcoder) {
                    write("/uf " + transcoder.encodeObject(frame, MessageInternetFrame.class));
                }
            }
        }
        frame.setData(null);
        frame.setMessageType(MessageType.MESSAGE_PUBLISH_FILE);
        synchronized (transcoder) {
            write("/uf " + transcoder.encodeObject(frame, MessageInternetFrame.class));
        }
        System.out.println(file.getName() + " done");
    }

    @SneakyThrows
    private void write(String text) {
        socketWriter.write(text);
        socketWriter.newLine();
        socketWriter.flush();
    }

    public static void main(String[] args) throws IOException {
        int port = DEFAULT_PORT;
        String host = DEFAULT_HOST;
        if (args.length == 2) {
            host = args[0];
            port = Integer.parseInt(args[1]);
        }
        Application application = new Application(host, port);
        new Thread(application).start();
        application.consoleLoop();
    }

    @SneakyThrows
    @Override
    public void run() {
        while (true) {
            String line = socketReader.readLine();
            if (line.startsWith(MESSAGE_TEXT_PREFIX)) {
                System.out.println(line.substring(2));
            } else if (line.startsWith(MESSAGE_FILE_PREFIX)) {
                receiveFile(line);
            }
        }
    }

    @SneakyThrows
    private void receiveFile(String line) {
        String receiverText = line.substring(2);
        MessageInternetFrame frame;
        synchronized (transcoder) {
            frame = transcoder.decodeObject(receiverText, MessageInternetFrame.class);
        }
        if (frame.getMessageType() == MessageType.MESSAGE_PUBLISH_FILE) {
            System.out.printf("File %s downloaded%n", frame.getDestinationName());
            requestedFiles.downloaded(frame.getDestinationName());
            return;
        }
        if (requestedFiles.isRequested(frame.getDestinationName())) {
            File file = new File(frame.getDestinationName());
            try (DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(file, true))) {
                dataOutputStream.write(frame.getData());
            }
        }
    }
}
