/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.pps.extensions.cmi;

import nl.esi.pps.tmsc.Dependency;

public class AnnotatedTextUtils {
    /**
     * @param dependency The input dependency.
     * @return {@code true} if {@code dependency} is an annotated text dependency, meaning that it has been assigned an
     *     execution type as a property. The execution type property can be specified for a function, and is then
     *     applied to each execution of that function. This implies only execution dependencies can be annotated text
     *     dependencies.
     */
    @SuppressWarnings("deprecation")
    public static boolean isAnnotatedTextDependency(Dependency dependency) {
        return dependency.getProperties().containsKey("execType");
    }
}
