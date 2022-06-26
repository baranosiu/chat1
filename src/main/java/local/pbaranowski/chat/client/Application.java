package local.pbaranowski.chat.client;

import lombok.SneakyThrows;

import java.io.*;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

import static java.util.Collections.synchronizedList;

public class Application implements Runnable {

    class RequestedFiles {
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

    private static final int DEFAULT_PORT = 9000;
    private static final String DEFAULT_HOST = "127.0.0.1";
    private Socket socket;
    private BufferedReader socketReader;
    private BufferedWriter socketWriter;
    private RequestedFiles requestedFiles = new RequestedFiles();

    public Application(String host, int port) throws IOException {
        var socket = new Socket(host, port);
        socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        socketWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    @SneakyThrows
    private void consoleLoop() {
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            String line = console.readLine();
            if (line.equals("/q")) {
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
        String fields[] = line.split(" ");
        File file = new File(fields[2]);
        if (!file.canRead()) {
            System.out.println(String.format("Can't read from file %s", file.getName()));
            return;
        }
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            while (fileInputStream.available() > 0) {
                String base64Text = java.util.Base64.getEncoder().encodeToString(fileInputStream.readNBytes(256));
                line = "/uf " + fields[1] + " " + base64Text + " " + fields[2];
                write(line);
            }
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
            port = Integer.valueOf(args[1]);
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
            if (line.startsWith("m:")) {
                System.out.println(line.substring(2));
            } else if (line.startsWith("f:")) {
                receiveFile(line);
            }
        }
    }

    @SneakyThrows
    private void receiveFile(String line) {
        String fields[] = line.substring(2).split("[ ]+", 2);
        if (fields[0].equals("C")) {
            System.out.println(String.format("File %s downloaded", fields[1]));
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
