#define WINVER 0x0600
#define _WIN32_WINNT 0x0600
#define UNICODE

#include <stdlib.h>
#include <windows.h>
#include "coursier_jniutils_DefaultNativeApi.h"

static const WCHAR *ENVIRONMENT_KEY = L"Environment";

/* Copies a Java string to a null-terminated UTF-16 buffer, which is what the *W registry
   functions expect. jchar and WCHAR are both UTF-16 code units, so nothing is re-encoded
   along the way. Going through the *A functions instead meant the value was encoded with
   the default charset on the Java side and decoded with the process' ANSI code page by
   Windows; whenever those two disagreed, non-ASCII characters were mangled or replaced
   by '?'. */
static WCHAR *toWide(JNIEnv *env, jstring s, jsize *lenOut) {
  const jsize len = (*env)->GetStringLength(env, s);
  WCHAR *buf = (WCHAR *) malloc(((size_t) len + 1) * sizeof(WCHAR));
  if (buf == NULL) {
    jclass cls = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
    if (cls != NULL)
      (*env)->ThrowNew(env, cls, "Cannot allocate environment variable buffer");
    return NULL;
  }
  (*env)->GetStringRegion(env, s, 0, len, (jchar *) buf);
  buf[len] = 0;
  if (lenOut != NULL)
    *lenOut = len;
  return buf;
}

static jstring emptyString(JNIEnv *env) {
  const jchar none = 0;
  return (*env)->NewString(env, &none, 0);
}

static jstring errorMessage(JNIEnv *env, LSTATUS status) {
  jchar digits[20];
  jchar buf[24];
  unsigned long value = (unsigned long) status;
  jsize digitCount = 0;
  jsize len = 0;

  do {
    digits[digitCount++] = (jchar) ('0' + (value % 10));
    value /= 10;
  } while (value != 0);

  buf[len++] = (jchar) 'E';
  while (digitCount > 0)
    buf[len++] = digits[--digitCount];

  return (*env)->NewString(env, buf, len);
}

/* Tells running applications, Explorer first, that the user environment changed, like the
   System Properties environment variable dialog does. Explorer then re-reads it from the
   registry, so that the processes it starts afterwards, like new terminals, see the change.
   SMTO_ABORTIFHUNG and the timeout keep hung top-level windows from blocking us; the
   timeout applies to each window. The registry was already updated when we get here, so
   failures and timeouts are ignored: at worst, the change is only picked up at the next
   logon, like before. */
static void broadcastEnvironmentChange(void) {
  SendMessageTimeoutW(
    HWND_BROADCAST,
    WM_SETTINGCHANGE,
    0,
    (LPARAM) ENVIRONMENT_KEY,
    SMTO_ABORTIFHUNG,
    1000,
    NULL
  );
}

/* Windows only expands references to other environment variables, like "%JAVA_HOME%\bin",
   in values stored as REG_EXPAND_SZ. Like the System Properties environment variable dialog,
   we store values containing a '%' that way, and the other ones as plain REG_SZ. */
static DWORD userEnvironmentVariableType(const WCHAR *value, jsize len) {
  jsize i;
  for (i = 0; i < len; i++) {
    if (value[i] == '%')
      return REG_EXPAND_SZ;
  }
  return REG_SZ;
}

JNIEXPORT jstring JNICALL Java_coursier_jniutils_DefaultNativeApi_SetUserEnvironmentVariableNative
  (JNIEnv *env, jclass class, jstring key, jstring value) {

  jsize valueLen = 0;
  WCHAR *keyStr = toWide(env, key, NULL);
  if (keyStr == NULL)
    return NULL;
  WCHAR *valueStr = toWide(env, value, &valueLen);
  if (valueStr == NULL) {
    free(keyStr);
    return NULL;
  }

  /* cbData counts bytes, and is expected to cover the terminating null character. */
  const DWORD size = (DWORD) (((size_t) valueLen + 1) * sizeof(WCHAR));

  LSTATUS status = RegSetKeyValueW(
    HKEY_CURRENT_USER,
    ENVIRONMENT_KEY,
    keyStr,
    userEnvironmentVariableType(valueStr, valueLen),
    valueStr,
    size
  );

  free(keyStr);
  free(valueStr);

  if (status != ERROR_SUCCESS)
    return errorMessage(env, status);
  broadcastEnvironmentChange();
  return emptyString(env);
}

JNIEXPORT jstring JNICALL Java_coursier_jniutils_DefaultNativeApi_GetUserEnvironmentVariableNative
  (JNIEnv *env, jclass class, jstring key) {

  WCHAR *keyStr = toWide(env, key, NULL);
  if (keyStr == NULL)
    return NULL;

  DWORD type = 0;

  /* RRF_NOEXPAND keeps REG_EXPAND_SZ values as they are stored, rather than expanding the
     references to other environment variables they contain. Callers that read a value, edit
     it, and write it back - like coursier's WindowsEnvVarUpdater - would otherwise replace
     those references by whatever they happened to expand to. */
  const DWORD flags = RRF_RT_REG_SZ | RRF_RT_REG_EXPAND_SZ | RRF_NOEXPAND;

  /* A null buffer asks for the size only, in bytes and including the terminating null.
     That call is documented to return ERROR_SUCCESS, but take ERROR_MORE_DATA - which is
     what a sizing call reports elsewhere in the registry API - to mean the same thing. */
  DWORD size = 0;
  LSTATUS status = RegGetValueW(
    HKEY_CURRENT_USER,
    ENVIRONMENT_KEY,
    keyStr,
    flags,
    &type,
    NULL,
    &size
  );

  if (status != ERROR_SUCCESS && status != ERROR_MORE_DATA) {
    free(keyStr);
    if (status == ERROR_FILE_NOT_FOUND)
      return NULL;
    return errorMessage(env, status);
  }

  /* One extra character for the 'V' prefix telling the Java side this is a value. */
  WCHAR *data = (WCHAR *) malloc((size_t) size + sizeof(WCHAR));
  if (data == NULL) {
    free(keyStr);
    jclass cls = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
    if (cls != NULL)
      (*env)->ThrowNew(env, cls, "Cannot allocate environment variable buffer");
    return NULL;
  }

  data[0] = (WCHAR) 'V';
  status = RegGetValueW(
    HKEY_CURRENT_USER,
    ENVIRONMENT_KEY,
    keyStr,
    flags,
    &type,
    &data[1],
    &size
  );
  free(keyStr);

  if (status != ERROR_SUCCESS) {
    free(data);
    if (status == ERROR_FILE_NOT_FOUND)
      return NULL;
    return errorMessage(env, status);
  }

  /* RegGetValueW null-terminates string values and counts that terminator in the size it
     reports back, so trim it rather than carry it over to the Java string. */
  jsize len = (jsize) (size / sizeof(WCHAR));
  while (len > 0 && data[len] == 0)
    len--;

  const jstring result = (*env)->NewString(env, (const jchar *) data, len + 1);
  free(data);
  return result;
}

JNIEXPORT jstring JNICALL Java_coursier_jniutils_DefaultNativeApi_DeleteUserEnvironmentVariableNative
  (JNIEnv *env, jclass class, jstring key) {

  WCHAR *keyStr = toWide(env, key, NULL);
  if (keyStr == NULL)
    return NULL;

  LSTATUS status = RegDeleteKeyValueW(
    HKEY_CURRENT_USER,
    ENVIRONMENT_KEY,
    keyStr
  );
  free(keyStr);

  /* Nothing changed if the variable wasn't there, so there's nothing to broadcast then. */
  if (status == ERROR_FILE_NOT_FOUND)
    return emptyString(env);
  if (status != ERROR_SUCCESS)
    return errorMessage(env, status);
  broadcastEnvironmentChange();
  return emptyString(env);
}
