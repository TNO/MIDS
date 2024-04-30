/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cif.extensions;

import org.apache.commons.lang.SystemUtils;

/**
 * Provide support to enable long paths on Windows.
 */
public class WindowsLongPathSupport {
    /**
     * Prefix enabling long paths on Windows.
     */
    public static final String LONG_PATH_PREFIX = "\\\\?\\";

    /**
     * @param path Path to check for prefix.
     * @return {@code true} if path has prefix enabling long paths on Windows, {@code false} otherwise.
     */
    public static boolean hasLongPathPrefix(String path) {
        return path.startsWith(LONG_PATH_PREFIX);
    }

    /**
     * @param path Path to add prefix to.
     * @return Path with prefix enabling long paths on Windows, if it was not already present.
     */
    public static String ensureLongPathPrefix(String path) {
        return (!hasLongPathPrefix(path) && SystemUtils.IS_OS_WINDOWS) ? LONG_PATH_PREFIX + path : path;
    }
}
