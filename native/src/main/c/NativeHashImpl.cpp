// Support for CentOS 6
#if __linux__ && !__arm__ // Waterfall
__asm__(".symver memcpy,memcpy@GLIBC_2.2.5");
#endif // Waterfall

#include <stdlib.h>
#include <string.h>

#include <mbedtls/sha256.h>

#include "network_ycc_waterdog_jni_NativeHashImpl.h"

#ifdef __APPLE__
    #define htole64(x) x
#else
    #include <endian.h>
#endif

typedef unsigned char byte;

jint throwException(JNIEnv *env, const char* message, int err) {
    // These can't be static for some unknown reason
    jclass exceptionClass = env->FindClass("net/md_5/bungee/jni/NativeCodeException");
    jmethodID exceptionInitID = env->GetMethodID(exceptionClass, "<init>", "(Ljava/lang/String;I)V");

    jstring jMessage = env->NewStringUTF(message);

    jthrowable throwable = (jthrowable) env->NewObject(exceptionClass, exceptionInitID, jMessage, err);
    return env->Throw(throwable);
}

//do everything in one call using stack allocation
void JNICALL Java_network_ycc_waterdog_jni_NativeHashImpl_staticPEHash(JNIEnv *env, jclass clazz,
        jlong counter, jlong in, jint inLength, jlong key, jlong out) {
    int ret;
    mbedtls_sha256_context ctx;

    counter = htole64(counter);

    mbedtls_sha256_init(&ctx);
    if((ret = mbedtls_sha256_starts_ret(&ctx, 0)) != 0) goto err;
    if((ret = mbedtls_sha256_update_ret(&ctx, (byte *)&counter, 8)) != 0) goto err;
    if((ret = mbedtls_sha256_update_ret(&ctx, (byte *)in, inLength)) != 0) goto err;
    if((ret = mbedtls_sha256_update_ret(&ctx, (byte *)key, 32)) != 0) goto err;
    if((ret = mbedtls_sha256_finish_ret(&ctx, (byte *)out)) != 0) goto err;

    return;

err:
    throwException(env, "Could not complete static SHA256 hash: ", ret);
}

jlong JNICALL Java_network_ycc_waterdog_jni_NativeHashImpl_init(JNIEnv *env, jobject thiz) {
    mbedtls_sha256_context *ctx = new mbedtls_sha256_context;

    mbedtls_sha256_init(ctx);

    int ret = mbedtls_sha256_starts_ret(ctx, 0);
    if (ret != 0) goto err;

    return (jlong)ctx;

err:
    throwException(env, "Could not start SHA256 hash: ", ret);
    return 0;
}

void JNICALL Java_network_ycc_waterdog_jni_NativeHashImpl_free(JNIEnv *env, jobject thiz, jlong ctx) {
    mbedtls_sha256_free((mbedtls_sha256_context *)ctx);
    delete (mbedtls_sha256_context *)ctx;
}

void JNICALL Java_network_ycc_waterdog_jni_NativeHashImpl_update__JB(JNIEnv *env, jobject thiz, jlong ctx, jbyte in) {
    int ret = mbedtls_sha256_update_ret((mbedtls_sha256_context *)ctx, (byte *)&in, 1);
    if (ret != 0) goto err;

    return;

err:
    throwException(env, "Could not update SHA256 hash: ", ret);
}

void JNICALL Java_network_ycc_waterdog_jni_NativeHashImpl_updateLongLE(JNIEnv *env, jobject thiz, jlong ctx, jlong in) {
    in = htole64(in);

    int ret = mbedtls_sha256_update_ret((mbedtls_sha256_context *)ctx, (byte *)&in, 8);
    if (ret != 0) goto err;

    return;

err:
    throwException(env, "Could not update SHA256 hash: ", ret);
}

void JNICALL Java_network_ycc_waterdog_jni_NativeHashImpl_update__JJI(JNIEnv *env, jobject thiz, jlong ctx, jlong in, jint len) {
    int ret = mbedtls_sha256_update_ret((mbedtls_sha256_context *)ctx, (byte *)in, len);
    if (ret != 0) goto err;

    return;

err:
    throwException(env, "Could not update SHA256 hash: ", ret);
}

void JNICALL Java_network_ycc_waterdog_jni_NativeHashImpl_digest(JNIEnv *env, jobject thiz, jlong ctx, jlong out) {
    int ret;

    ret = mbedtls_sha256_finish_ret((mbedtls_sha256_context *)ctx, (byte *)out);
    if (ret != 0) goto err;

    ret = mbedtls_sha256_starts_ret((mbedtls_sha256_context *)ctx, 0);
    if (ret != 0) goto err;

    return;

err:
    throwException(env, "Could not finish SHA256 hash: ", ret);
}
