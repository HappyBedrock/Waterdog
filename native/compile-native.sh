#!/bin/bash

#Build embed using this fork here: https://github.com/yesdog/mbed-crypto
#This has 1 minor change to the CMake build scripts so that
#everything is generated with fPIC.

#Build mbed, then build these binaries with:
#MBED_CRYPTO_PATH=/your/mbed/path/here ./compile-native.sh

# Waterfall - rewrite below to extend platform support

if [ "$OSTYPE" == "darwin"* ]; then
  # brew install mbedtls zlib
  PREFIX="osx-"
  CXX_ARGS="-lmbedcrypto -lz -I$JAVA_HOME/include/ -I$JAVA_HOME/include/darwin/ \
    -I/usr/local/include -I$MBED_CRYPTO_PATH/include -L/usr/local/lib/ -L$MBED_CRYPTO_PATH/library/"
else
  # apt-get install libmbedtls-dev zlib1g-dev
  CXX_ARGS="-lmbedcrypto -lz -I$JAVA_HOME/include/ -I$JAVA_HOME/include/linux/ \
    -I$MBED_CRYPTO_PATH/include/ -L$MBED_CRYPTO_PATH/library/"
fi

arch=$(uname -i)

if [[ $AARCH64 ]]; then
  PREFIX="aarch64-"
elif [[ $arch == arm* ]]; then
  PREFIX="armhf-"
fi

CXX="g++ -shared -fPIC -O3 -Wall -Werror"

$CXX src/main/c/NativeCipherImpl.cpp -o src/main/resources/${PREFIX}native-cipher.so $CXX_ARGS
$CXX src/main/c/NativeCompressImpl.cpp -o src/main/resources/${PREFIX}native-compress.so $CXX_ARGS
