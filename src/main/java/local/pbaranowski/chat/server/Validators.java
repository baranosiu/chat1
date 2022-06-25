package local.pbaranowski.chat.server;

public class Validators {
    public static boolean isNameValid(String name) {
        return name.matches("\\w{2,16}");
    }

    public static boolean isChannelNameValid(String name) {
        return name.matches("#\\w{2,16}");
    }

    public static boolean isChannelSpecial(String name) {
        return name.matches("@\\w{2,16}");
    }

    public static boolean isNameOrChannelValid(String name) {
        return isNameValid(name) || isChannelNameValid(name);
    }
}
