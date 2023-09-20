/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cmi;

import java.nio.file.Path;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application running CMI process.
 */
public class CmiApplication implements IApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(CmiApplication.class);

    @Override
    public Integer start(IApplicationContext context) throws Exception {
        // Get command line arguments.
        Map<?, ?> args = context.getArguments();
        String[] cmiArgs = (String[])args.get("application.args");

        ComponentExtractionOptions options = ComponentExtractionOptions.parse(cmiArgs);

        // No options were parsed, usage information was printed, and we're done.
        if (options == null) {
            return IApplication.EXIT_OK;
        }

        Path inputPath = options.getInput().getPath();
        LOGGER.info("Loading TMSC from " + inputPath.toString());
        LOGGER.info("Writing output to " + options.getOutput().getPath().toString());

        // Perform CMI.
        new ComponentExtraction().extract(inputPath, inputPath.getParent(), options, new NullProgressMonitor());

        return IApplication.EXIT_OK;
    }

    @Override
    public void stop() {
    }
}
