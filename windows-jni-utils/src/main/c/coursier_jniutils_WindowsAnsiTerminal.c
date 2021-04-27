#include <stdio.h>
#include <windows.h>
#include "coursier_jniutils_NativeCalls.h"

jstring _Java_coursier_jniutils_NativeCalls_Terminal_lastError
  (JNIEnv *env, const char *origin) {

  DWORD err = GetLastError();
  char dummy[1];
  const int len = snprintf(dummy, 1, "error:%s error %lu", origin, err);
  char *buf = malloc(len + 1);
  snprintf(buf, len + 1, "error:%s error %lu", origin, err);
  const jstring result = (*env)->NewStringUTF(env, buf);
  free(buf);
  return result;
}

jstring _Java_coursier_jniutils_NativeCalls_Terminal_format
  (JNIEnv *env, const char *format, ...) {

  char dummy[1];
  va_list ap;
  va_start(ap, format);
  const int len = vsnprintf(dummy, 1, format, ap);
  va_end(ap);
  char *buf = malloc(len + 1);
  va_start(ap, format);
  vsnprintf(buf, len + 1, format, ap);
  va_end(ap);
  const jstring result = (*env)->NewStringUTF(env, buf);
  free(buf);
  return result;
}

JNIEXPORT jstring JNICALL Java_coursier_jniutils_NativeCalls_terminalSizeNative
  (JNIEnv *env, jclass class) {

  HANDLE handle = GetStdHandle(STD_OUTPUT_HANDLE);
  if (handle == INVALID_HANDLE_VALUE) {
    return _Java_coursier_jniutils_NativeCalls_Terminal_lastError(env, "GetStdHandle");
  }

  CONSOLE_SCREEN_BUFFER_INFO info;
  memset(&info, 0, sizeof(CONSOLE_SCREEN_BUFFER_INFO));
  BOOL res = GetConsoleScreenBufferInfo(handle, &info);
  if (!res) {
    return _Java_coursier_jniutils_NativeCalls_Terminal_lastError(env, "GetConsoleScreenBufferInfo");
  }

  return _Java_coursier_jniutils_NativeCalls_Terminal_format(env, "%hu:%hu", info.dwSize.X, info.dwSize.Y);
}

JNIEXPORT jstring JNICALL Java_coursier_jniutils_NativeCalls_enableAnsiOutputNative
  (JNIEnv *env, jclass class) {

  HANDLE handle = GetStdHandle(STD_OUTPUT_HANDLE);
  if (handle == INVALID_HANDLE_VALUE) {
    return _Java_coursier_jniutils_NativeCalls_Terminal_lastError(env, "GetStdHandle");
  }

  DWORD mode = 0;
  BOOL res = GetConsoleMode(handle, &mode);
  if (!res) {
    return _Java_coursier_jniutils_NativeCalls_Terminal_lastError(env, "GetConsoleMode");
  }

  char *ret = "false";
  if (!(mode & ENABLE_VIRTUAL_TERMINAL_PROCESSING)) {
    res = SetConsoleMode(handle, mode | ENABLE_VIRTUAL_TERMINAL_PROCESSING);
    if (!res) {
      return _Java_coursier_jniutils_NativeCalls_Terminal_lastError(env, "GetConsoleMode");
    }
    ret = "true";
  }

  return (*env)->NewStringUTF(env, ret);
}
