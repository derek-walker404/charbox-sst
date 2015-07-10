#!/bin/bash

rm -rf temp;mkdir temp
cp target/sst*.jar temp/
cp app.properties temp/
cp app.yaml temp/
cd temp
zip charbot-sst.zip ./*
