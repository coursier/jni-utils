package coursier.jniutils;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public final class LoadWindowsLibrary {

    static File fromClassPath(ClassLoader cl) {
        if (cl == null)
            return null;

        if (cl instanceof URLClassLoader) {
            URLClassLoader ucl = (URLClassLoader) cl;
            for (URL url: ucl.getURLs()) {
                if (url.getProtocol().equals("file")) {
                    File f;
                    try {
                        f = new File(url.toURI()).getAbsoluteFile();
                    } catch (URISyntaxException ex) {
                        throw new RuntimeException(ex);
                    }
                    if (f.getName().equals(dllName() + ".dll") && f.isFile())
                        return f;
                }
            }
        } else if (cl.getClass().getName().startsWith("jdk.internal.loader.ClassLoaders$AppClassLoader")) {
            String strCp = System.getProperty("java.class.path");
            for (String elem: strCp.split(File.pathSeparator)) {
                File f = new File(elem).getAbsoluteFile();
                if (f.getName().equals(dllName() + ".dll") && f.isFile())
                    return f;
            }
        }

        return fromClassPath(cl.getParent());
    }

    static File fromResources(ClassLoader cl) throws IOException, URISyntaxException {
        URL url = cl.getResource(dllResourcePath());
        if (url == null)
          return null;
        if (url.getProtocol().equals("file"))
            return new File(url.toURI());
        try (InputStream is = url.openStream(); ReadableByteChannel ic = Channels.newChannel(is)) {
            File tmpFile = File.createTempFile(dllName(), ".dll");
            tmpFile.deleteOnExit();
            try (FileOutputStream os = new FileOutputStream(tmpFile)) {
                os.getChannel().transferFrom(ic, 0, Long.MAX_VALUE);
            }
            return tmpFile;
        }
    }

    final static Object lock = new Object();
    static boolean initialized = false;
    public static void ensureInitialized() {
        if (!initialized) {
            synchronized (lock) {
                if (!initialized) {
                    ClassLoader cl = Thread.currentThread().getContextClassLoader();
                    File dll = fromClassPath(cl);
                    if (dll == null)
                        try {
                            dll = fromResources(cl);
                        } catch (URISyntaxException | IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    if (dll == null)
                        System.loadLibrary(dllName());
                    else
                        System.load(dll.getAbsolutePath());
                }
            }
        }
    }

    final static String dllName() {
        return DllName.name;
    }
    final static String dllResourcePath() {
        return "META-INF/native/windows64/" + dllName() + ".dll";
    }

}
