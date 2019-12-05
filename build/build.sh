#!/bin/bash

set -xe
mkdir -p out
javac src/me/barend/generateavatar/*.java
cd src
jar cmf ../build/GenerateAvatar.mf ../out/GenerateAvatar.jar me/barend/generateavatar/*.class
find me/barend/generateavatar -name '*.class' -delete
