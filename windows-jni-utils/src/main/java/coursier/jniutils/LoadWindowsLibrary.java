package coursier.jniutils;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Locale;

/**
 * Loads the DLL of this library.
 *
 * It's looked for, in that order, directly on the class path, as a resource of the JAR
 * (extracted to a stable per-version and per-architecture location if needed), and in
 * java.library.path.
 *
 * DLLs extracted from the JAR go by default to {@code <java.io.tmpdir>/coursier-jni-utils/<arch>/},
 * and are re-used by later runs. This directory can be changed via the
 * {@code coursier.jniutils.dll-dir} Java property.
 *
 * Each {@link NativeApi} implementation loads the DLL under a name of its own (like
 * {@code csjniutils-<version>-bootstrap.dll}), see {@link #ensureInitialized(String)}. The JVM
 * doesn't allow a given DLL file to be loaded by several class loaders, so this allows the
 * different implementations (and copies of this library, possibly shaded) to load it from
 * different class loaders.
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

    static File fromResources(ClassLoader cl, String variant) throws IOException, URISyntaxException {
        if (cl == null)
            return null;
        URL url = cl.getResource(dllResourcePath());
        if (url == null)
          return null;
        if (url.getProtocol().equals("file") && variant.isEmpty())
            return new File(url.toURI());

        byte[] content;
        try (InputStream is = url.openStream()) {
            content = readAll(is);
        }
        return write(content, variant);
    }

    /**
     * Writes content to the file the DLL should be loaded from for variant: in the extraction
     * directory if possible, or in a temporary file else.
     */
    static File write(byte[] content, String variant) throws IOException {
        String baseName = dllBaseName(variant);
        File dir = extractionDir();
        if (dir != null) {
            try {
                return extract(content, dir, baseName);
            } catch (IOException | SecurityException ex) {
                // can't write to the stable location, fall back to a temporary file below
            }
        }

        File tmpFile = File.createTempFile(baseName + "-", ".dll");
        // deleteOnExit can't delete a loaded DLL on Windows, but that's the best we can do here
        tmpFile.deleteOnExit();
        try (FileOutputStream os = new FileOutputStream(tmpFile)) {
            os.write(content);
        }
        return tmpFile;
    }

    /** Where to extract the DLL from the JAR, or null if no such directory can be computed. */
    static File extractionDir() {
        String archDir = isArm64() ? "windows64-arm64" : "windows64";
        String dir = System.getProperty("coursier.jniutils.dll-dir");
        if (dir != null && !dir.isEmpty())
            return new File(dir, archDir);
        String tmpDir = System.getProperty("java.io.tmpdir");
        if (tmpDir == null || tmpDir.isEmpty())
            return null;
        return Paths.get(tmpDir, "coursier-jni-utils", archDir).toFile();
    }

    /**
     * Writes content to {@code dir/<baseName>.dll}, unless that file already exists with the same
     * content. The file is written atomically (written to a temporary file in the same directory,
     * then moved), so that concurrent JVMs never load a partially written DLL.
     *
     * If that file already exists with a different content (which can happen with snapshot
     * versions), and can't be replaced (because another JVM loaded it), a new uniquely named
     * file is written in the same directory instead.
     */
    static File extract(byte[] content, File dir, String baseName) throws IOException {
        File dest = new File(dir, baseName + ".dll");
        if (hasContent(dest, content))
            return dest;

        Files.createDirectories(dir.toPath());

        Path tmp = Files.createTempFile(dir.toPath(), baseName + "-", ".dll.part");
        try {
            Files.write(tmp, content);
            try {
                move(tmp, dest.toPath());
                return dest;
            } catch (IOException ex) {
                // Another JVM may have written the same content concurrently, or may have
                // the existing file loaded, which prevents replacing it on Windows.
                if (hasContent(dest, content))
                    return dest;
                Path unique = Files.createTempFile(dir.toPath(), baseName + "-", ".dll");
                move(tmp, unique);
                File uniqueFile = unique.toFile();
                uniqueFile.deleteOnExit();
                return uniqueFile;
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    private static void move(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static boolean hasContent(File f, byte[] content) {
        if (!f.isFile() || f.length() != content.length)
            return false;
        try {
            return Arrays.equals(Files.readAllBytes(f.toPath()), content);
        } catch (IOException | SecurityException ex) {
            return false;
        }
    }

    private static byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int read;
        while ((read = is.read(buf)) >= 0)
            baos.write(buf, 0, read);
        return baos.toByteArray();
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

    /** Loads the DLL under its default name, unless it's already loaded or assumed to be. */
    public static void ensureInitialized() {
        ensureInitialized("");
    }

    /**
     * Loads the DLL, unless it's already loaded or assumed to be.
     *
     * If variant isn't empty, the DLL is loaded from a file of its own, named like
     * {@code csjniutils-<version>-<variant>.dll}, so that it doesn't conflict with the
     * DLL loaded by other class loaders for other variants. Each {@link NativeApi}
     * implementation passes its own variant here.
     *
     * Only the first call actually loads the DLL. Later ones are no-ops, whatever their variant,
     * as all the {@link NativeApi} implementations of a given class loader can use the DLL
     * loaded by the first one.
     */
    public static void ensureInitialized(String variant) {
        if (variant == null)
            variant = "";
        if (!variant.matches("[A-Za-z0-9_.-]*"))
            throw new IllegalArgumentException("Invalid DLL variant: '" + variant + "'");
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
                    try {
                        if (dll != null && !variant.isEmpty())
                            // copy the DLL found on the class path to a file specific to this variant
                            dll = write(Files.readAllBytes(dll.toPath()), variant);
                        if (dll == null) {
                            dll = fromResources(contextCl, variant);
                            if (dll == null && ownCl != contextCl)
                                dll = fromResources(ownCl, variant);
                        }
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
    /** Name of the DLL file for variant, without its ".dll" extension */
    final static String dllBaseName(String variant) {
        return variant.isEmpty() ? dllName() : dllName() + "-" + variant;
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
