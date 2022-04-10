#!/bin/bash

docker run --rm -v "$PWD":"$PWD" -v "$HOME/.m2":/root/.m2 -w "$PWD" maven:3.8.4-jdk-8 mvn clean install