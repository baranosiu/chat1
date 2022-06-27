package local.pbaranowski.chat.client;

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
        final String UPLOAD_FRAME_FORMAT = "/uf %s %s %s";
        String[] fields = line.split(" ");
        File file = new File(fields[2]);
        if (!file.canRead()) {
            System.out.printf("Can't read from file %s%n", file.getName());
            return;
        }
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            while (fileInputStream.available() > 0) {
                String base64Text = java.util.Base64.getEncoder().encodeToString(fileInputStream.readNBytes(256));
                write(String.format(UPLOAD_FRAME_FORMAT, fields[1], base64Text, fields[2]));
            }
        }
        write(String.format(UPLOAD_FRAME_FORMAT,fields[1],FILE_TRANSFER_COMPLETED,fields[2]));
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
        String[] fields = line.substring(2).split("[ ]+", 2);
        if (fields[0].equals(FILE_TRANSFER_COMPLETED)) {
            System.out.printf("File %s downloaded%n", fields[1]);
            requestedFiles.downloaded(fields[1]);
            return;
        }
        if (requestedFiles.isRequested(fields[1])) {
            File file = new File(fields[1]);
            try (DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(file, true))) {
                dataOutputStream.write(java.util.Base64.getDecoder().decode(fields[0]));
            }
        }
    }
}
