package coursier.jniutils;

import java.io.IOException;

/**
 * The persistent environment variables of the current user, stored in the registry under
 * HKEY_CURRENT_USER\Environment.
 *
 * These are not the environment variables of the current process: changes made here don't
 * affect it, nor already running processes.
 *
 * After each successful {@link #set} and each {@link #delete} that actually removed a variable,
 * a WM_SETTINGCHANGE message is broadcast to all top-level windows, like the System Properties
 * environment variable dialog does. Explorer then reloads the user environment, so that the
 * processes it starts afterwards (new terminals, say) see the change, without the user having
 * to log off. Windows that don't process that message in time (one second each) are skipped.
 * The outcome of that broadcast is ignored: the registry was updated by then, so a failed or
 * timed out broadcast doesn't make these methods throw.
 */
public final class WindowsEnvironmentVariables {

    /**
     * The value of a user environment variable, or null if it's not set.
     *
     * Values of type REG_EXPAND_SZ are returned as is, without expanding the variables they
     * reference.
     *
     * @throws IOException if the registry can't be read
     */
    public static String get(String key) throws IOException {
        String value = NativeApi.get().GetUserEnvironmentVariable(key);
        if (value == null)
            return null;
        if (value.startsWith("E"))
            throw new IOException(
                    "Error getting user environment variable " + key + ": " + value.substring("E".length()));
        return value.substring(1);
    }

    /**
     * Sets a user environment variable, creating it if needed.
     *
     * Values containing a '%' are stored as REG_EXPAND_SZ, other ones as REG_SZ.
     *
     * @throws IOException if the registry can't be written
     */
    public static void set(String key, String value) throws IOException {
        String ret = NativeApi.get().SetUserEnvironmentVariable(key, value);
        if (ret.startsWith("E"))
            throw new IOException(
                    "Error setting user environment variable " + key + ": " + ret.substring("E".length()));
    }

    /**
     * Deletes a user environment variable. Does nothing if it's not set.
     *
     * @throws IOException if the variable can't be deleted
     */
    public static void delete(String key) throws IOException {
        String ret = NativeApi.get().DeleteUserEnvironmentVariable(key);
        if (ret.startsWith("E"))
            throw new IOException(
                    "Error deleting user environment variable " + key + ": " + ret.substring("E".length()));
    }
}
