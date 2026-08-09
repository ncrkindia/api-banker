#!/bin/bash
# Set Java21 or above for running the application
# export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
# export PATH=$JAVA_HOME/bin:$PATH
java -jar apibanker-${project.version}.jar &

