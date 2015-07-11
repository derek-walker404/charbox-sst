#!/bin/bash


cd ~/Workspace/java/tpofof-core/;mvn clean install -DskipTests
cd ~/Workspace/java/charbox-domain/;mvn clean install -DskipTests
cd ~/Workspace/java/charbox-core/;mvn clean install -DskipTests
cd ~/Workspace/java/charbox-client/;mvn clean install -DskipTests
cd ~/Workspace/java/charbox-sst/;mvn clean install -DskipTests
