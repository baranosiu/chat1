package local.pbaranowski.chat;

import lombok.extern.slf4j.Slf4j;
import java.util.Arrays;

@Slf4j
public class Startup {
    public static void main(String[] args) {
        try {
            if (args.length > 0) {
                if (args[0].equals("server")) {
                    local.pbaranowski.chat.server.Application.main(Arrays.copyOfRange(args, 1, args.length));
                } else {
                    local.pbaranowski.chat.client.Application.main(args);
                }
            }
        } catch (Exception e) {
            log.error("{}", e.getMessage(), e);
        }
    }
}
