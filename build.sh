#!/bin/bash


cd ../tpofof-core/;mvn clean install; cd ../charbox-dataapi/
cd ../charbox-domain/;mvn clean install; cd ../charbox-dataapi/
cd ../charbox-core/;mvn clean install; cd ../charbox-dataapi/
cd ../charbox-client/;mvn clean install;cd ../charbox-sst/
cd ../charbox-sst/;mvn clean install;cd ../charbox-sst/
