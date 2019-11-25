#!/bin/bash

set -xe
mkdir -p out
javac src/me/barend/generateavatar/Main.java
cd src
jar cmf ../build/GenerateAvatar.mf ../out/GenerateAvatar.jar me/barend/generateavatar/Main.class
rm me/barend/generateavatar/Main.class