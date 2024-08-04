/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cmi.utils;

import java.time.Duration;

/** TMSC metrics. */
public class TmscMetrics {
    /** The duration of the TMSC, from the first event to the last event. */
    public Duration duration;

    /** The number of events in the TMSC. */
    public long eventCount;
}
