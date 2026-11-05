package coursier.jniutils;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Locale;

/**
 * Loads the DLL of this library.
 *
 * It's looked for, in that order, directly on the class path, as a resource of the JAR
 * (extracted to a temporary file if needed), and in java.library.path.
 */
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
                    } catch (URISyntaxException | IllegalArgumentException ex) {
                        continue;
                    }
                    if (f.getName().equals(dllName() + ".dll") && f.isFile())
                        return f;
                }
            }
        } else if (cl.getClass().getName().startsWith("jdk.internal.loader.ClassLoaders$AppClassLoader")) {
            String strCp = System.getProperty("java.class.path");
            if (strCp != null)
                for (String elem: strCp.split(File.pathSeparator)) {
                    if (elem.isEmpty())
                        continue;
                    File f = new File(elem).getAbsoluteFile();
                    if (f.getName().equals(dllName() + ".dll") && f.isFile())
                        return f;
                }
        }

        return fromClassPath(cl.getParent());
    }

    static File fromResources(ClassLoader cl) throws IOException, URISyntaxException {
        if (cl == null)
            return null;
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

    /** Assumes the DLL is already loaded, or its native code is otherwise available. */
    public static void assumeInitialized() {
        assumeInitialized(true);
    }
    /** Sets whether the DLL should be assumed to be loaded already. */
    public static void assumeInitialized(boolean initialized) {
        LoadWindowsLibrary.initialized = initialized;
    }

    final static Object lock = new Object();
    static volatile boolean initialized = false;
    /** Loads the DLL, unless it's already loaded or assumed to be. */
    public static void ensureInitialized() {
        if (!initialized) {
            synchronized (lock) {
                if (!initialized) {
                    // Look for the DLL via the thread context class loader first (if it isn't
                    // null), so that users can override the DLL via it, and via our own class
                    // loader (the one of the JAR having the DLL) as a fallback.
                    ClassLoader contextCl = Thread.currentThread().getContextClassLoader();
                    ClassLoader ownCl = LoadWindowsLibrary.class.getClassLoader();
                    File dll = fromClassPath(contextCl);
                    if (dll == null && ownCl != contextCl)
                        dll = fromClassPath(ownCl);
                    if (dll == null)
                        try {
                            dll = fromResources(contextCl);
                            if (dll == null && ownCl != contextCl)
                                dll = fromResources(ownCl);
                        } catch (URISyntaxException | IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    if (dll == null)
                        System.loadLibrary(dllName());
                    else
                        System.load(dll.getAbsolutePath());
                    initialized = true;
                }
            }
        }
    }

    final static String dllName() {
        return DllName.name;
    }
    final static boolean isArm64() {
        String arch = System.getProperty("os.arch");
        if (arch == null)
            return false;
        arch = arch.toLowerCase(Locale.ROOT);
        return arch.equals("aarch64") || arch.equals("arm64");
    }
    final static String dllResourcePath() {
        String archDir = isArm64() ? "windows64-arm64" : "windows64";
        return "META-INF/native/" + archDir + "/" + dllName() + ".dll";
    }

}
