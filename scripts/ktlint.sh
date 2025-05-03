#!/bin/bash
if [ -x "$(command -v ktlint)" ]; then
    ktlint
else
    echo "Ktlint is not installed. Please install it following instructions from https://github.com/pinterest/ktlint."
    exit 1
fi

