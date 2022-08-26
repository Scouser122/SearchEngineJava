#!/usr/bin/env sh

java -XX:+UseContainerSupport -Xmx256m -Xss512k -XX:MetaspaceSize=100m -jar /apps/app.jar