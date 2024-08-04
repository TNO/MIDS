#!/usr/bin/env sh
#
# Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
#
# This program and the accompanying materials are made available
# under the terms of the MIT License which is available at
# https://opensource.org/licenses/MIT
#
# SPDX-License-Identifier: MIT
#

QUALIFIER_POSTFIX="dev"

if [ $# -gt 0 ]
then
    QUALIFIER_POSTFIX=$(echo $1 | cut -s -d '-' -f2-)
fi

# Get Git last commit date.
GIT_DATE_EPOCH=$(git log -1 --format=%cd --date=raw | cut -d ' ' -f 1)
GIT_DATE=$(date -d @$GIT_DATE_EPOCH -u +%Y%m%d-%H%M%S)

if [ -z "$QUALIFIER_POSTFIX" ]
then
    echo "v$GIT_DATE"
else
    echo "v$GIT_DATE-$QUALIFIER_POSTFIX"
fi
