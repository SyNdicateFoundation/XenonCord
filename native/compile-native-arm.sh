#!/usr/bin/env bash

set -eu

CWD=$(pwd)

echo "Compiling mbedtls"
#Waterfall start - rewrite to add support for macOS
pushd mbedtls
if [[ "$OSTYPE" == "darwin"* ]]; then
  CFLAGS="-fPIC -I$CWD/src/main/c -DMBEDTLS_USER_CONFIG_FILE='<mbedtls_custom_config.h>'" make no_test
else
  CFLAGS="-fPIC -I$CWD/src/main/c -DMBEDTLS_USER_CONFIG_FILE='<mbedtls_custom_config.h>'" make CC=aarch64-linux-gnu-gcc AR=aarch64-linux-gnu-ar no_test
fi
popd
#Waterfall end

echo "Compiling zlib"
pushd zlib
if [[ "$OSTYPE" == "darwin"* ]]; then
  CFLAGS="-fPIC -DNO_GZIP" ./configure --static && make CFLAGS="-fPIC"
else
  CFLAGS="-fPIC -DNO_GZIP" CC=aarch64-linux-gnu-gcc CHOST=arm64 ./configure --target="aarch64" --static && make CFLAGS="-fPIC -march=armv8-a+crc" CC=aarch64-linux-gnu-gcc AR=aarch64-linux-gnu-ar
fi
popd

CC="aarch64-linux-gnu-gcc"
if [[ "$OSTYPE" == "darwin"* ]]; then
  CC="aarch64-apple-darwin24-gcc-14" # Brew GCC
  PREFIX="osx-"
  CFLAGS="-c -fPIC -O3 -Wall -Werror -I$JAVA_HOME/include/ -I$JAVA_HOME/include/darwin/"
else
  CFLAGS="-c -fPIC -O3 -Wall -Werror -I$JAVA_HOME/include/ -I$JAVA_HOME/include/linux/"
fi
LDFLAGS="-shared"

echo "Compiling bungee"
$CC $CFLAGS -o shared.o src/main/c/shared.c
$CC $CFLAGS -Imbedtls/include -o NativeCipherImpl.o src/main/c/NativeCipherImpl.c
$CC $CFLAGS -Izlib -o NativeCompressImpl.o src/main/c/NativeCompressImpl.c

echo "Linking native-cipher-arm.so"
$CC $LDFLAGS -o src/main/resources/"${PREFIX:-}"native-cipher-arm.so shared.o NativeCipherImpl.o mbedtls/library/libmbedcrypto.a

echo "Linking native-compress-arm.so"
$CC $LDFLAGS -o src/main/resources/"${PREFIX:-}"native-compress-arm.so shared.o NativeCompressImpl.o zlib/libz.a

echo "Cleaning up"
rm shared.o NativeCipherImpl.o NativeCompressImpl.o
