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
 *
 * Several copies of a given implementation can still end up in different class loaders of the
 * same JVM (like when a build tool loads several copies of a library depending on this one). When
 * the JVM refuses to load the DLL because another class loader loaded the same file already, it's
 * copied to {@code <name>-1.dll}, {@code <name>-2.dll}, etc. in the same directory, and loaded from
 * there, see {@link #load(File, String, PathLoader)}.
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
        return writeAs(content, dllBaseName(variant));
    }

    /**
     * Writes content to the file named {@code <baseName>.dll} in the extraction directory if
     * possible, or to a temporary file else.
     */
    static File writeAs(byte[] content, String baseName) throws IOException {
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
                        load(dll, variant, System::load);
                    initialized = true;
                }
            }
        }
    }

    /** Something loading a DLL from its absolute path, {@link System#load} in practice. */
    interface PathLoader {
        void load(String path);
    }

    /** Default value of {@link #maxCopies}. */
    final static int defaultMaxCopies = 16;

    /** Java property overriding {@link #defaultMaxCopies}. */
    final static String maxCopiesProperty = "coursier.jniutils.max-dll-copies";

    /**
     * Maximum number of copies {@link #load(File, String, PathLoader)} writes, when the DLL
     * can't be loaded because other class loaders loaded it already.
     *
     * Read once, from the {@code coursier.jniutils.max-dll-copies} Java property if it's set,
     * when this class is initialized. 0 disables the copies. Values that aren't non-negative
     * integers are ignored, in favor of {@link #defaultMaxCopies}.
     */
    final static int maxCopies = parseMaxCopies(System.getProperty(maxCopiesProperty));

    static int parseMaxCopies(String value) {
        if (value == null)
            return defaultMaxCopies;
        value = value.trim();
        if (value.isEmpty())
            return defaultMaxCopies;
        try {
            int parsed = Integer.parseInt(value);
            return parsed >= 0 ? parsed : defaultMaxCopies;
        } catch (NumberFormatException ex) {
            return defaultMaxCopies;
        }
    }

    /**
     * Whether ex is the error the JVM throws when the DLL file was already loaded by another
     * class loader. The JVM throws it itself (it's not an error from the OS), with a message
     * made of a hard-coded string, that isn't localized, and is the same on Java 8 and later
     * versions.
     */
    static boolean isLoadedByAnotherClassLoader(UnsatisfiedLinkError ex) {
        String message = ex.getMessage();
        return message != null && message.contains("already loaded in another classloader");
    }

    /**
     * Loads dll via loader.
     *
     * If the JVM refuses to load it because another class loader loaded the same file already,
     * this copies it to {@code <name>-1.dll}, {@code <name>-2.dll}, etc. (at most
     * {@link #maxCopies} of them), in the same directory as the other DLLs extracted from the JAR
     * (a temporary file if that directory can't be written), and loads the first of those that
     * isn't loaded by another class loader. These copies are re-used by later runs, like the
     * extracted DLLs.
     *
     * Any other error is thrown as is, without trying copies.
     */
    static void load(File dll, String variant, PathLoader loader) {
        load(dll, variant, loader, maxCopies);
    }

    /** Same as {@link #load(File, String, PathLoader)}, trying at most maxCopies copies. */
    static void load(File dll, String variant, PathLoader loader, int maxCopies) {
        UnsatisfiedLinkError firstError;
        try {
            loader.load(dll.getAbsolutePath());
            return;
        } catch (UnsatisfiedLinkError ex) {
            if (!isLoadedByAnotherClassLoader(ex) || maxCopies <= 0)
                throw ex;
            firstError = ex;
        }

        byte[] content;
        try {
            content = Files.readAllBytes(dll.toPath());
        } catch (IOException | SecurityException ex) {
            firstError.addSuppressed(ex);
            throw firstError;
        }

        String baseName = dllBaseName(variant);
        for (int i = 1; i <= maxCopies; i++) {
            File copy;
            try {
                copy = writeAs(content, baseName + "-" + i);
            } catch (IOException | SecurityException ex) {
                firstError.addSuppressed(ex);
                throw firstError;
            }
            try {
                loader.load(copy.getAbsolutePath());
                return;
            } catch (UnsatisfiedLinkError ex) {
                if (!isLoadedByAnotherClassLoader(ex)) {
                    ex.addSuppressed(firstError);
                    throw ex;
                }
            }
        }

        UnsatisfiedLinkError error = new UnsatisfiedLinkError(
            "Could not load " + dll + " or any of its " + maxCopies +
            " copies, as they're all loaded by other class loaders"
        );
        error.initCause(firstError);
        throw error;
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
