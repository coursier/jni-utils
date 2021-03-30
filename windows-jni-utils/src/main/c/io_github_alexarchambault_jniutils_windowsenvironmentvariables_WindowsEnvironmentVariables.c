#define WINVER 0x0600
#define _WIN32_WINNT 0x0600
#define UNICODE

#include <stdio.h>
#include <windows.h>
#include "coursier_jniutils_windowsenvironmentvariables_WindowsEnvironmentVariables.h"

JNIEXPORT jstring JNICALL Java_coursier_jniutils_windowsenvironmentvariables_WindowsEnvironmentVariables_SetUserEnvironmentVariableNative
  (JNIEnv *env, jclass class, jstring key, jbyteArray value) {

  const char *keyStr = (*env)->GetStringUTFChars(env, key, NULL);
  const jbyte *valueStr = (*env)->GetByteArrayElements(env, value, NULL);
  const size_t valueLen = (*env)->GetArrayLength(env, value);

  LSTATUS status = RegSetKeyValueA(
    HKEY_CURRENT_USER,
    "Environment",
    keyStr,
    REG_SZ,
    valueStr,
    valueLen
  );

  (*env)->ReleaseStringUTFChars(env, key, keyStr);
  (*env)->ReleaseByteArrayElements(env, value, valueStr, 0);
  if (status != ERROR_SUCCESS) {
    char data[100+1];
    snprintf(data, 101, "E%lu", status);
    data[100] = '\0';
    return (*env)->NewStringUTF(env, data);
  }
  return (*env)->NewStringUTF(env, "");
}

JNIEXPORT jbyteArray JNICALL Java_coursier_jniutils_windowsenvironmentvariables_WindowsEnvironmentVariables_GetUserEnvironmentVariableNative
  (JNIEnv *env, jclass class, jstring key) {

  const char *keyStr = (*env)->GetStringUTFChars(env, key, NULL);

  DWORD size = 100;
  DWORD type = 0;

  jbyteArray arr = (*env)->NewByteArray(env, size + 1);
  jbyte *data = (*env)->GetByteArrayElements(env, arr, NULL);

  data[0] = 'V';
  LSTATUS status = RegGetValueA(
    HKEY_CURRENT_USER,
    "Environment",
    keyStr,
    RRF_RT_REG_SZ,
    &type,
    &data[1],
    &size
  );
  (*env)->ReleaseStringUTFChars(env, key, keyStr);
  if (status != ERROR_SUCCESS) {
    snprintf(data, 101, "E%lu", status);
    data[100] = '\0';
  }

  (*env)->ReleaseByteArrayElements(env, arr, data, 0);

  return arr;
}
