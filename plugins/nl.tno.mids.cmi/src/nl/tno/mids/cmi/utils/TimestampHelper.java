/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cmi.utils;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.util.TimeZone;

import nl.esi.pps.common.text.BigDecimalFormat;
import nl.esi.pps.common.text.EpochSecondFormat;

/** Utilities for formatting time stamps in readable date/time representations. */
public class TimestampHelper {
    private TimestampHelper() {
    }

    /**
     * @param timestamp The time stamp to format.
     * @return A readable formatted date/time representation of the given {@code timestamp}.
     */
    public static String readable(Long timestamp) {
        StringBuffer buffer = new StringBuffer();
        BigDecimalFormat epoch = new EpochSecondFormat(0, TimeZone.getTimeZone("Europe/Amsterdam"));
        epoch.format(timestamp, buffer, new FieldPosition(DateFormat.FULL));
        return buffer.toString();
    }
}
