#!/bin/bash

# Dumb little utility script for switching back and forth between
# Saxon 9.3 and Saxon 9.4. I keep both sets of jars in this directory
# with names that end in .93 and .94

if [ "$1" = "93" ]; then
    echo "Switching to Saxon 9.3"
    cp saxon9ee.93 saxon9ee.jar
    cp saxon9he.93 saxon9he.jar
else if [ "$1" = "94" ]; then
    echo "Switching to Saxon 9.4"
    cp saxon9ee.94 saxon9ee.jar
    cp saxon9he.94 saxon9he.jar
else
    echo "Unknown saxon version: $1"
fi
fi
