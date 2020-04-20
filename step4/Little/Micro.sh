#!/bin/bash
#micro execution script

antlr4 Little.g4

javac Little*.java

java LittleDriver $1