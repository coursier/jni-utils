/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class coursier_jniutils_DefaultNativeApi */

#ifndef _Included_coursier_jniutils_DefaultNativeApi
#define _Included_coursier_jniutils_DefaultNativeApi
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     coursier_jniutils_DefaultNativeApi
 * Method:    terminalSizeNative
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_coursier_jniutils_DefaultNativeApi_terminalSizeNative
  (JNIEnv *, jclass);

/*
 * Class:     coursier_jniutils_DefaultNativeApi
 * Method:    enableAnsiOutputNative
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_coursier_jniutils_DefaultNativeApi_enableAnsiOutputNative
  (JNIEnv *, jclass);

/*
 * Class:     coursier_jniutils_DefaultNativeApi
 * Method:    GetUserEnvironmentVariableNative
 * Signature: ([B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_coursier_jniutils_DefaultNativeApi_GetUserEnvironmentVariableNative
  (JNIEnv *, jclass, jbyteArray);

/*
 * Class:     coursier_jniutils_DefaultNativeApi
 * Method:    SetUserEnvironmentVariableNative
 * Signature: ([B[B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_coursier_jniutils_DefaultNativeApi_SetUserEnvironmentVariableNative
  (JNIEnv *, jclass, jbyteArray, jbyteArray);

/*
 * Class:     coursier_jniutils_DefaultNativeApi
 * Method:    DeleteUserEnvironmentVariableNative
 * Signature: ([B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_coursier_jniutils_DefaultNativeApi_DeleteUserEnvironmentVariableNative
  (JNIEnv *, jclass, jbyteArray);

/*
 * Class:     coursier_jniutils_DefaultNativeApi
 * Method:    GetKnownFolderPathNative
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_coursier_jniutils_DefaultNativeApi_GetKnownFolderPathNative
  (JNIEnv *, jclass, jstring);

/*
 * Class:     coursier_jniutils_DefaultNativeApi
 * Method:    GetModuleFileNameNative
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_coursier_jniutils_DefaultNativeApi_GetModuleFileNameNative
  (JNIEnv *, jclass);

#ifdef __cplusplus
}
#endif
#endif
