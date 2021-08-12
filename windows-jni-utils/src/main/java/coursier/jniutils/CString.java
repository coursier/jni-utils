package coursier.jniutils;

import java.nio.charset.Charset;
import java.util.Arrays;

public final class CString {

    public static String fromC(byte[] data) {
        if (data == null)
            return null;
        int remove = 0;
        while (data.length > remove && data[data.length - remove - 1] == '\0') {
            remove += 1;
        }
        byte[] data0 = data;
        if (remove > 0)
            data0 = Arrays.copyOf(data, data.length - remove);
        return new String(data0, Charset.defaultCharset());
    }

    public static byte[] toC(String c) {
        if (c == null)
            return null;
        byte[] b = c.getBytes(Charset.defaultCharset());
        return Arrays.copyOf(b, b.length + 1);
    }

}
