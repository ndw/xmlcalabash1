#!/bin/bash

# Dumb little utility script for switching back and forth between
# Saxon 9.3 and Saxon 9.4. I keep both sets of jars in this directory
# with names that end in .93 and .94

if [ "$1" = "96" ]; then
    echo "Switching to Saxon 9.6"
    cp saxon9ee.96 saxon9ee.jar
    cp saxon9he.96 saxon9he.jar
else if [ "$1" = "95" ]; then
    echo "Switching to Saxon 9.5"
    cp saxon9ee.95 saxon9ee.jar
    cp saxon9he.95 saxon9he.jar
else
    echo "Unknown saxon version: $1"
fi
fi
