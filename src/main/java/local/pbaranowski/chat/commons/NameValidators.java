package local.pbaranowski.chat.commons;

public class NameValidators {
    public static boolean isNameValid(String name) {
        return name.matches("\\w{2,16}");
    }

    public static boolean isChannelName(String name) {
        return name.matches("#\\w{2,16}");
    }

    public static boolean isChannelSpecial(String name) {
        return name.matches("@\\w{2,16}");
    }

    public static boolean isNameOrChannelValid(String name) {
        return isNameValid(name) || isChannelName(name);
    }
}
