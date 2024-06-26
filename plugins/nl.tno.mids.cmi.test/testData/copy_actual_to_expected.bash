#!/usr/bin/env bash

#########################################################################
# Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
#
# This program and the accompanying materials are made available
# under the terms of the MIT License which is available at
# https://opensource.org/licenses/MIT
#
# SPDX-License-Identifier: MIT
#########################################################################

for f in `find . -type d -name output_actual`; do
    rm -rf $f/../output_expected
    mv $f $f/../output_expected
done
