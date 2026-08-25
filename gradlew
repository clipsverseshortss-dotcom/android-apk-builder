#!/bin/sh
APP_BASE_NAME=`basename "$0"`
DIRNAME=`dirname "$0"`
if [ "x$DIRNAME" = "x" ]; then
    DIRNAME=.
fi
CLASSPATH=$DIRNAME/gradle/wrapper/gradle-wrapper.jar
exec java -jar "$CLASSPATH" "$@"
