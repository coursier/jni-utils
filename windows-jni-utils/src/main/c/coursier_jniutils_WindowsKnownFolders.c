#define WINVER 0x0600
#define _WIN32_WINNT 0x0600

#include <stdlib.h>
#include <shlobj.h>
#include "coursier_jniutils_DefaultNativeApi.h"

/* Copies a Java string to a null-terminated UTF-16 buffer. GetStringChars doesn't guarantee
   a terminating null character, and CLSIDFromString needs one. The caller frees the buffer. */
static WCHAR *toWide(JNIEnv *env, jstring s) {
  const jsize len = (*env)->GetStringLength(env, s);
  WCHAR *buf = (WCHAR *) malloc(((size_t) len + 1) * sizeof(WCHAR));
  if (buf == NULL) {
    jclass cls = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
    if (cls != NULL)
      (*env)->ThrowNew(env, cls, "Cannot allocate known folder ID buffer");
    return NULL;
  }
  (*env)->GetStringRegion(env, s, 0, len, (jchar *) buf);
  buf[len] = 0;
  return buf;
}

JNIEXPORT jstring JNICALL Java_coursier_jniutils_DefaultNativeApi_GetKnownFolderPathNative
  (JNIEnv *env, jclass cls, jstring rfid) {

  long res = 0;

  if (rfid == NULL)
    return NULL;

  WCHAR *rfidStr = toWide(env, rfid);
  if (rfidStr == NULL)
    return NULL;

  GUID id;
  memset(&id, 0, sizeof(GUID));
  res = CLSIDFromString(rfidStr, &id);
  free(rfidStr);
  if (res != 0) {
    return NULL;
  }

  /* ppszPath has to be freed with CoTaskMemFree even if SHGetKnownFolderPath fails. */
  WCHAR *ppszPath = NULL;
  res = SHGetKnownFolderPath(&id, 0, NULL, &ppszPath);
  if (res != 0) {
    CoTaskMemFree(ppszPath);
    return NULL;
  }

  const jstring result = (*env)->NewString(env, ppszPath, wcslen(ppszPath));
  CoTaskMemFree(ppszPath);

  return result;
}

