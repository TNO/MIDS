/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.compare.output.util;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.stream.Collectors;

/** Shared utilities for output methods. */
public class TemplateUtils {
    /**
     * Retrieve the template with a given name from the plugin resources.
     * 
     * @param fileName Name of template file to retrieve.
     * @return The template in string form.
     */
    public static String getTemplate(String fileName) {
        String templateName = "/"
                + TemplateUtils.class.getPackage().getName().replace('.', '/').replaceAll("/util$", "/resources/")
                + fileName;

        try (InputStream src = TemplateUtils.class.getResourceAsStream(templateName)) {
            return new BufferedReader(new InputStreamReader(src)).lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new RuntimeException("Error while loading " + templateName + ".", e);
        }
    }

    /**
     * Write a string to a new output file.
     * 
     * @param outputPath {@link Path} to folder to contain output file.
     * @param fileName Name of output file to create.
     * @param contentsToWrite Contents to write to file.
     * @throws IOException In case of an I/O error.
     */
    public static void write(Path outputPath, String fileName, String contentsToWrite) throws IOException {
        Path path = outputPath.resolve(fileName);
        Files.createDirectories(path.getParent());
        Files.write(path, contentsToWrite.getBytes(UTF_8), StandardOpenOption.CREATE);
    }

    /**
     * Compute {@link String} representation of variant identifier.
     * 
     * @param variant Variant to convert.
     * @return Computed {@String} representation for this variant.
     */
    public static String renderVariant(int variant) {
        if (variant == 0) {
            return "-";
        } else {
            return renderInternal(variant - 1);
        }
    }

    /**
     * Compute {@link String} representation of variant identifier.
     * 
     * @param variant Variant to convert.
     * @return Computed {@String} representation for this variant.
     */
    private static String renderInternal(int variant) {
        if (variant < 26) {
            return String.valueOf((char)(variant + (int)'A'));
        } else {
            return renderInternal((variant / 26) - 1) + renderInternal(variant % 26);
        }
    }
}
