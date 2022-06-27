package local.pbaranowski.chat.transportlayer;

public interface Transcoder<T> {
    String encodeObject(T frame, Class<T> transportClass);

    T decodeObject(String frame, Class<T> transportClass);
}
