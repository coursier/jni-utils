/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class lmcoursier_internal_jniutils_LmNativeApi */

#ifndef _Included_lmcoursier_internal_jniutils_LmNativeApi
#define _Included_lmcoursier_internal_jniutils_LmNativeApi
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     lmcoursier_internal_jniutils_LmNativeApi
 * Method:    terminalSizeNative
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_lmcoursier_internal_jniutils_LmNativeApi_terminalSizeNative
  (JNIEnv *, jclass);

/*
 * Class:     lmcoursier_internal_jniutils_LmNativeApi
 * Method:    enableAnsiOutputNative
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_lmcoursier_internal_jniutils_LmNativeApi_enableAnsiOutputNative
  (JNIEnv *, jclass);

/*
 * Class:     lmcoursier_internal_jniutils_LmNativeApi
 * Method:    GetUserEnvironmentVariableNative
 * Signature: ([B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_lmcoursier_internal_jniutils_LmNativeApi_GetUserEnvironmentVariableNative
  (JNIEnv *, jclass, jbyteArray);

/*
 * Class:     lmcoursier_internal_jniutils_LmNativeApi
 * Method:    SetUserEnvironmentVariableNative
 * Signature: ([B[B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_lmcoursier_internal_jniutils_LmNativeApi_SetUserEnvironmentVariableNative
  (JNIEnv *, jclass, jbyteArray, jbyteArray);

/*
 * Class:     lmcoursier_internal_jniutils_LmNativeApi
 * Method:    DeleteUserEnvironmentVariableNative
 * Signature: ([B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_lmcoursier_internal_jniutils_LmNativeApi_DeleteUserEnvironmentVariableNative
  (JNIEnv *, jclass, jbyteArray);

/*
 * Class:     lmcoursier_internal_jniutils_LmNativeApi
 * Method:    GetKnownFolderPathNative
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_lmcoursier_internal_jniutils_LmNativeApi_GetKnownFolderPathNative
  (JNIEnv *, jclass, jstring);

/*
 * Class:     lmcoursier_internal_jniutils_LmNativeApi
 * Method:    GetModuleFileNameNative
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_lmcoursier_internal_jniutils_LmNativeApi_GetModuleFileNameNative
  (JNIEnv *, jclass);

#ifdef __cplusplus
}
#endif
#endif
