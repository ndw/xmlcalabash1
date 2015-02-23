#!/bin/bash

# This script populates the lib directory with a number of commercial
# jar files necessary to compile extension steps.

if [ "$XMLC_BASE" != "" ] && [ "$XMLC_NAME" != "" ] && [ "XMLC_PASS" != "" ]
then
    echo "Updating lib..."
    cd lib
    curl -s -o filelist -u "$XMLC_NAME:$XMLC_PASS" "$XMLC_BASE/filelist"
    for f in `cat filelist`; do
        curl -s -o $f -u "$XMLC_NAME:$XMLC_PASS" "$XMLC_BASE/$f"
    done
    mkdir -p AH
    cd AH
    curl -s -o archive.tar.gz -u "$XMLC_NAME:$XMLC_PASS" "$XMLC_BASE/AH/archive.tar.gz"
    tar zxf archive.tar.gz
    rm archive.tar.gz
fi
