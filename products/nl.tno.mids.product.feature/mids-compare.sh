#!/bin/sh
set -e
case "$OSTYPE" in
  linux*) ECLIPSE_PATH=$(dirname -- "$(realpath -- "$BASH_SOURCE")")/../eclipse ;;
  msys* | cygwin*) ECLIPSE_PATH=$(dirname -- "$(realpath -- "$BASH_SOURCE")")/../eclipsec.exe ;;
  *) echo "Operating system not supported"
     return 1 ;;
esac
"$ECLIPSE_PATH" -application nl.tno.mids.compare.MidsCompareApplication -consoleLog -nosplash "$@"