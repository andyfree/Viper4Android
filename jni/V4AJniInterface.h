/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_vipercn_viper4android_v2_activity_V4AJniInterface */

#ifndef _Included_com_vipercn_viper4android_v2_activity_V4AJniInterface
#define _Included_com_vipercn_viper4android_v2_activity_V4AJniInterface

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class: com_vipercn_viper4android_v2_activity_V4AJniInterface
 * Method: checkLibraryUsable
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_vipercn_viper4android_1v2_activity_V4AJniInterface_checkLibraryUsable(
                JNIEnv *, jclass);

/*
 * Class:     com_vipercn_viper4android_v2_activity_V4AJniInterface
 * Method:    checkCPUHasNEON
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_vipercn_viper4android_1v2_activity_V4AJniInterface_checkCPUHasNEON(
                JNIEnv *, jclass);

/*
 * Class:     com_vipercn_viper4android_v2_activity_V4AJniInterface
 * Method:    checkCPUHasVFP
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_vipercn_viper4android_1v2_activity_V4AJniInterface_checkCPUHasVFP(
                JNIEnv *, jclass);

/*
 * Class:     com_vipercn_viper4android_v2_activity_V4AJniInterface
 * Method:    getImpulseResponseInfo
 * Signature: ([B)[I
 */
JNIEXPORT jintArray JNICALL Java_com_vipercn_viper4android_1v2_activity_V4AJniInterface_getImpulseResponseInfo(
                JNIEnv *, jclass, jbyteArray);

/*
 * Class:     com_vipercn_viper4android_v2_activity_V4AJniInterface
 * Method:    readImpulseResponse
 * Signature: ([B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_vipercn_viper4android_1v2_activity_V4AJniInterface_readImpulseResponse(
                JNIEnv *, jclass, jbyteArray);

/*
 * Class:     com_vipercn_viper4android_v2_activity_V4AJniInterface
 * Method:    hashImpulseResponse
 * Signature: ([BI)[I
 */
JNIEXPORT jintArray JNICALL Java_com_vipercn_viper4android_1v2_activity_V4AJniInterface_hashImpulseResponse(
                JNIEnv *, jclass, jbyteArray, jint);

#ifdef __cplusplus
}
#endif

#endif
