package local.pbaranowski.chat.server;

import java.io.IOException;

import static local.pbaranowski.chat.constants.Constants.*;

public class Application {

    public static void main(String[] args) throws IOException {
        int port = DEFAULT_PORT;
        if(args.length > 0) {
            port = Integer.parseInt(args[0]);
        }
        new Server(port).start();
    }
}
