package local.pbaranowski.chat.commons.transportlayer;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
public class Base64Transcoder<T> implements Transcoder<T> {
    private final Gson gson = new Gson();

    @Override
    public String encodeObject(T ftpFrame, Class<T> transportClass) {
        return new String(Base64.getEncoder().encode(gson.toJson(ftpFrame, transportClass).getBytes()), StandardCharsets.UTF_8);
    }

    @Override
    public T decodeObject(String frame, Class<T> transportClass) {
        Object result = transportClass.cast(gson.fromJson(new String(Base64.getDecoder().decode(frame), StandardCharsets.UTF_8), transportClass));
        try {
            return transportClass.cast(result);
        } catch (ClassCastException e) {
            return null;
        }
    }

}
