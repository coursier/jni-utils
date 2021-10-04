#define WINVER 0x0600
#define _WIN32_WINNT 0x0600

#include <windows.h>
#include <libloaderapi.h>
#include "coursier_jniutils_DefaultNativeApi.h"

jstring _Java_coursier_jniutils_DefaultNativeApi_Terminal_lastError(JNIEnv *, const char *);


JNIEXPORT jstring JNICALL Java_coursier_jniutils_DefaultNativeApi_GetModuleFileNameNative
  (JNIEnv *env, jclass cls) {

  WCHAR szPath[MAX_PATH];

  if (!GetModuleFileNameW(NULL, szPath, MAX_PATH)) {
    return _Java_coursier_jniutils_DefaultNativeApi_Terminal_lastError(env, "GetModuleFileName");
  }

  return (*env)->NewString(env, szPath, wcslen(szPath));
}

