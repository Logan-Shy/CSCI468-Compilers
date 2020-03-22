#!/bin/bash
#micro execution script

echo "running antlr on g4 file..."
antlr4 LittleLexer.g4

echo "compiling generated files..."
javac Little*.java

echo "executing driver file with given argument..."
java LittleLexerDriver $1