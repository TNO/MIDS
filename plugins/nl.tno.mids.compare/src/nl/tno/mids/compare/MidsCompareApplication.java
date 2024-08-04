/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.compare;

import java.util.Map;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

/**
 * Application wrapper for {@link MidsCompare}.
 */
public class MidsCompareApplication implements IApplication {
    @Override
    public Integer start(IApplicationContext context) throws Exception {
        // Get command line arguments.
        Map<?, ?> args = context.getArguments();
        String[] compareArgs = (String[])args.get("application.args");

        MidsCompare.main(compareArgs);

        return IApplication.EXIT_OK;
    }

    @Override
    public void stop() {
    }
}
