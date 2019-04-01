#!/bin/bash
set -e

echo "Start"
# updating
cd /etc/pipeline/PCM-Docker/org.pcm.automation.interface/
git pull
gradle bootJar

# running
cd cd /etc/pipeline/PCM-Docker/org.pcm.automation.interface/build/libs/

java -jar pcm-analysis-interface-0.1.0.jar --javaPath="java" --eclipsePath="/etc/eclipse/eclipse/"