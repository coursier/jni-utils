#define WINVER 0x0600
#define _WIN32_WINNT 0x0600
#define UNICODE

#include <stdio.h>
#include <windows.h>
#include "coursier_jniutils_DefaultNativeApi.h"

/* Windows only expands references to other environment variables, like "%JAVA_HOME%\bin",
   in values stored as REG_EXPAND_SZ. Like the System Properties environment variable dialog,
   we store values containing a '%' that way, and the other ones as plain REG_SZ. */
static DWORD userEnvironmentVariableType(const jbyte *value, size_t len) {
  size_t i;
  for (i = 0; i < len && value[i] != '\0'; i++) {
    if (value[i] == '%')
      return REG_EXPAND_SZ;
  }
  return REG_SZ;
}

JNIEXPORT jbyteArray JNICALL Java_coursier_jniutils_DefaultNativeApi_SetUserEnvironmentVariableNative
  (JNIEnv *env, jclass class, jbyteArray key, jbyteArray value) {

  jbyte *keyStr = (*env)->GetByteArrayElements(env, key, NULL);
  jbyte *valueStr = (*env)->GetByteArrayElements(env, value, NULL);
  const size_t valueLen = (*env)->GetArrayLength(env, value);

  LSTATUS status = RegSetKeyValueA(
    HKEY_CURRENT_USER,
    "Environment",
    keyStr,
    userEnvironmentVariableType(valueStr, valueLen),
    valueStr,
    valueLen
  );

  (*env)->ReleaseByteArrayElements(env, key, keyStr, JNI_ABORT);
  (*env)->ReleaseByteArrayElements(env, value, valueStr, JNI_ABORT);
  if (status != ERROR_SUCCESS) {
    char dummy = 0;
    int len = snprintf(&dummy, 1, "E%lu", status);
    jbyteArray arr = (*env)->NewByteArray(env, len + 1);
    jbyte *data = (*env)->GetByteArrayElements(env, arr, NULL);
    snprintf(data, len + 1, "E%lu", status);
    data[len] = '\0';
    (*env)->ReleaseByteArrayElements(env, arr, data, 0);
    return arr;
  }
  return (*env)->NewByteArray(env, 0);
}

JNIEXPORT jbyteArray JNICALL Java_coursier_jniutils_DefaultNativeApi_GetUserEnvironmentVariableNative
  (JNIEnv *env, jclass class, jbyteArray key) {

  jbyte *keyStr = (*env)->GetByteArrayElements(env, key, NULL);

  DWORD type = 0;

  /* RRF_NOEXPAND keeps REG_EXPAND_SZ values as they are stored, rather than expanding the
     references to other environment variables they contain. Callers that read a value, edit
     it, and write it back - like coursier's WindowsEnvVarUpdater - would otherwise replace
     those references by whatever they happened to expand to. */
  const DWORD flags = RRF_RT_REG_SZ | RRF_RT_REG_EXPAND_SZ | RRF_NOEXPAND;

  DWORD size = 1;
  char dummy = 0;
  LSTATUS status = RegGetValueA(
    HKEY_CURRENT_USER,
    "Environment",
    keyStr,
    flags,
    &type,
    &dummy,
    &size
  );

  jbyteArray arr = NULL;
  jbyte *data = NULL;

  if (status == ERROR_SUCCESS || status == ERROR_MORE_DATA) {
    if (status == ERROR_MORE_DATA) {
      size = size + 1; /* ??? */
    }
    arr = (*env)->NewByteArray(env, size + 1);
    data = (*env)->GetByteArrayElements(env, arr, NULL);

    data[0] = 'V';
    status = RegGetValueA(
      HKEY_CURRENT_USER,
      "Environment",
      keyStr,
      flags,
      &type,
      &data[1],
      &size
    );
    (*env)->ReleaseByteArrayElements(env, arr, data, 0);
  }
  (*env)->ReleaseByteArrayElements(env, key, keyStr, JNI_ABORT);

  if (status == ERROR_FILE_NOT_FOUND) {
    if (arr != NULL) {
      (*env)->DeleteLocalRef(env, arr);
    }
    return NULL;
  } else if (status != ERROR_SUCCESS) {
    char dummy = 0;
    int len = snprintf(&dummy, 1, "E%lu", status);
    jbyteArray arr0 = (*env)->NewByteArray(env, len + 1);
    jbyte *data = (*env)->GetByteArrayElements(env, arr0, NULL);
    snprintf(data, len + 1, "E%lu", status);
    data[len] = '\0';
    (*env)->ReleaseByteArrayElements(env, arr0, data, 0);
    return arr0;
  }

  // TODO Check type?
  return arr;
}

JNIEXPORT jbyteArray JNICALL Java_coursier_jniutils_DefaultNativeApi_DeleteUserEnvironmentVariableNative
  (JNIEnv *env, jclass class, jbyteArray key) {

  jbyte *keyStr = (*env)->GetByteArrayElements(env, key, NULL);

  LSTATUS status = RegDeleteKeyValueA(
    HKEY_CURRENT_USER,
    "Environment",
    keyStr
  );
  (*env)->ReleaseByteArrayElements(env, key, keyStr, JNI_ABORT);
  if (status != ERROR_SUCCESS && status != ERROR_FILE_NOT_FOUND) {
    char dummy = 0;
    int len = snprintf(&dummy, 1, "E%lu", status);
    jbyteArray arr = (*env)->NewByteArray(env, len + 1);
    jbyte *data = (*env)->GetByteArrayElements(env, arr, NULL);
    snprintf(data, len + 1, "E%lu", status);
    (*env)->ReleaseByteArrayElements(env, arr, data, 0);
    return arr;
  }
  return (*env)->NewByteArray(env, 0);
}
