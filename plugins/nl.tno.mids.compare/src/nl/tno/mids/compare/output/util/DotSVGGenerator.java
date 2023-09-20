/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.compare.output.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import nl.tno.mids.cif.extensions.WindowsLongPathSupport;
import nl.tno.mids.common.MidsExecutableProvider;

/**
 * Utility for generating SVG images based on DOT files.
 */
public class DotSVGGenerator {
    /**
     * Generate a SVG image based on a given DOT input file.
     *
     * @param outputPath Path to main compare output folder.
     * @param dotInputPath Path to Dot input.
     * @param warnings List to collect warnings generated during the generation of the SVG image.
     * @param timeoutInSeconds The timeout in seconds for generating the SVG. This timeout should be positive.
     * @return {@code true} if the SVG image was successfully generated, {@code false} otherwise.
     * @throws IOException In case of an I/O error.
     * @throws InterruptedException In case the process for generating the SVG image got interrupted.
     */
    public static boolean generateDotSVG(Path outputPath, Path dotInputPath, List<String> warnings,
            int timeoutInSeconds)
    {
        // Resolve paths.
        Preconditions.checkArgument(0 < timeoutInSeconds);
        Path relativePath = outputPath.relativize(dotInputPath);
        File stdOutFile = dotInputPath.resolveSibling(dotInputPath.getFileName().toString() + ".out.tmp").toFile();

        // Set up the process for rendering the SVG.
        ProcessBuilder dotProcessBuilder = new ProcessBuilder(MidsExecutableProvider.getExecutablePath("dot"), "-q",
                "-Tsvg", "-O", WindowsLongPathSupport.ensureLongPathPrefix(dotInputPath.toAbsolutePath().toString()));
        dotProcessBuilder.redirectErrorStream(true);
        dotProcessBuilder.redirectOutput(stdOutFile);

        // Start the process for rendering the SVG.
        Process dotProcess;
        try {
            dotProcess = dotProcessBuilder.start();
        } catch (IOException e) {
            warnings.add("I/O error during dot processing of " + relativePath + ": " + e.getMessage());
            return false;
        }

        // Wait for the process to finish within the given timeout period.
        boolean dotProcessCompleted;
        try {
            dotProcessCompleted = dotProcess.waitFor(timeoutInSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            dotProcess.destroyForcibly();
            warnings.add("Dot interrupted during processing of " + relativePath + ".");

            try {
                Files.delete(stdOutFile.toPath());
            } catch (IOException ex) {
                warnings.add("I/O error while deleting temporary file from " + stdOutFile + ": " + ex.getMessage());
            }

            return false;
        }

        // Check whether the process timed out.
        if (!dotProcessCompleted) {
            dotProcess.destroyForcibly();
            warnings.add("Timeout during dot processing of " + relativePath);

            try {
                Files.delete(stdOutFile.toPath());
            } catch (IOException e) {
                warnings.add("I/O error while deleting temporary file from " + stdOutFile + ": " + e.getMessage());
            }

            return false;
        }

        // Read the error text from stdout/stderr.
        String errorText;
        try {
            errorText = Files.readAllLines(stdOutFile.toPath(), StandardCharsets.UTF_8).stream()
                    .collect(Collectors.joining(" "));
        } catch (IOException e) {
            warnings.add("I/O error during dot processing of " + relativePath + ": " + e.getMessage());

            try {
                Files.delete(stdOutFile.toPath());
            } catch (IOException ex) {
                warnings.add("I/O error while deleting temporary file from " + stdOutFile + ": " + ex.getMessage());
            }

            return false;
        }

        // Delete the temporary file with error text.
        stdOutFile.delete();

        // Check successful termination.
        if (dotProcess.exitValue() != 0) {
            warnings.add("Error during dot processing of " + relativePath + ", error code: " + dotProcess.exitValue()
                    + ", error text: " + errorText + ".");
            return false;
        }

        return true;
    }
}
