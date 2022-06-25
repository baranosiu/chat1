package local.pbaranowski.chat.server;

import java.io.IOException;

public class Application {

    public static void main(String[] args) throws IOException {
        int port = 9000;
        if(args.length > 0) {
            port = Integer.valueOf(args[0]);
        }
        new Server(port).start();
    }
}
