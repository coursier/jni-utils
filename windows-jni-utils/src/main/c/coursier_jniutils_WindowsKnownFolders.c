#define WINVER 0x0600
#define _WIN32_WINNT 0x0600

#include <shlobj.h>
#include "coursier_jniutils_NativeCalls.h"

JNIEXPORT jstring JNICALL Java_coursier_jniutils_NativeCalls_GetKnownFolderPathNative
  (JNIEnv *env, jclass cls, jstring rfid) {

  long res = 0;

  const WCHAR* rfidStr = (*env)->GetStringChars(env, rfid, NULL);

  GUID id;
  memset(&id, 0, sizeof(GUID));
  res = CLSIDFromString(rfidStr, &id);
  (*env)->ReleaseStringChars(env, rfid, rfidStr);
  if (res != 0) {
    return NULL;
  }

  WCHAR *ppszPath = NULL;
  res = SHGetKnownFolderPath(&id, 0, NULL, &ppszPath);
  if (res != 0) {
    return NULL;
  }

  const jstring result = (*env)->NewString(env, ppszPath, wcslen(ppszPath));
  CoTaskMemFree(ppszPath);

  return result;
}

