#!/bin/bash
#micro execution script

echo "running antlr on g4 file..."
antlr4 Little.g4

echo "compiling generated files..."
javac Little*.java

echo "executing driver file with given argument..."
java LittleDriver $1