/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.compare;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.ValueSource;

import nl.tno.mids.common.unittest.FileCompare;
import nl.tno.mids.compare.options.CmiCompareMode;
import nl.tno.mids.compare.options.CompareAlgorithm;
import nl.tno.mids.compare.options.CompareOptions;
import nl.tno.mids.compare.options.HslColorScheme;
import nl.tno.mids.compare.options.ModelType;

class MidsCompareTest {
    private static final String SVG_FILE_EXTENSION = "svg";

    /**
     * Apply compare tool to a given input set and check that output matches expected results.
     *
     * @param testName Name of the root folder of all test input, expected and testOutput data.
     * @param modelType {@link ModelType} to compare.
     * @param applyPostprocessing If {@code true}, post-processing will be performed on compare results.
     * @throws IOException when loading files fails.
     */
    @ParameterizedTest
    @CsvFileSource(resources = "/testCaseList.csv")
    public void performCompareTest(String testName, ModelType modelType, boolean applyPostprocessing)
            throws IOException
    {
        Path basePath = Paths.get("test").resolve(testName);
        Path inputPath = basePath.resolve("input");
        Path expectedPath = basePath.resolve("output_expected");
        CompareOptions compareOptions = new CompareOptions();
        compareOptions.inputPath = inputPath;
        compareOptions.outputPath = basePath.resolve("output_actual");
        compareOptions.compareAlgorithm = CompareAlgorithm.DYNAMIC;
        compareOptions.completeLattices = true;
        compareOptions.showComputedVariants = true;
        compareOptions.applyPostprocessing = applyPostprocessing;
        compareOptions.svgGenerationTimeout = 60;
        compareOptions.modelType = modelType;
        compareOptions.cmiCompareMode = CmiCompareMode.AUTOMATIC;
        compareOptions.colorScheme = HslColorScheme.INTUITIVE;
        compareOptions.unionIntersectionSizeLimit = Integer.MAX_VALUE;
        compareOptions.structuralCompareSizeLimit = Integer.MAX_VALUE;

        MidsCompare.performCompare(compareOptions,
                Clock.fixed(Instant.parse("2021-01-01T00:00:00.00Z"), ZoneId.of("UTC")), new NullProgressMonitor());

        assertTrue(Files.exists(compareOptions.outputPath));
        assertTrue(Files.isDirectory(compareOptions.outputPath));

        FileCompare.checkDirectoriesEqual(expectedPath, compareOptions.outputPath, MidsCompareTest::isCompareFile);
    }

    /**
     * Test compare with lattice computation disabled.
     * 
     * @throws IOException when loading files fails.
     */
    @Test
    public void performNoLatticeTest() throws IOException {
        Path basePath = Paths.get("test").resolve("testNoLattice");
        Path inputPath = basePath.resolve("input");
        Path expectedPath = basePath.resolve("output_expected");
        CompareOptions compareOptions = new CompareOptions();
        compareOptions.inputPath = inputPath;
        compareOptions.outputPath = basePath.resolve("output_actual");
        compareOptions.compareAlgorithm = CompareAlgorithm.DYNAMIC;
        compareOptions.showComputedVariants = true;
        compareOptions.applyPostprocessing = true;
        compareOptions.svgGenerationTimeout = 60;
        compareOptions.modelType = ModelType.CMI;
        compareOptions.cmiCompareMode = CmiCompareMode.AUTOMATIC;
        compareOptions.colorScheme = HslColorScheme.INTUITIVE;
        compareOptions.unionIntersectionSizeLimit = Integer.MAX_VALUE;
        compareOptions.structuralCompareSizeLimit = Integer.MAX_VALUE;

        MidsCompare.performCompare(compareOptions,
                Clock.fixed(Instant.parse("2021-01-01T00:00:00.00Z"), ZoneId.of("UTC")), new NullProgressMonitor());

        assertTrue(Files.exists(compareOptions.outputPath));
        assertTrue(Files.isDirectory(compareOptions.outputPath));

        FileCompare.checkDirectoriesEqual(expectedPath, compareOptions.outputPath, MidsCompareTest::isCompareFile);
    }

    /**
     * Test compare with computed models hidden.
     * 
     * @param testName Name of the root folder of test input.
     * 
     * @throws IOException when loading files fails.
     */
    @ParameterizedTest
    @ValueSource(strings =
    {"testHideComputed", "testDontHideInput"})
    public void performHideComputedOptionTest(String testName) throws IOException {
        Path basePath = Paths.get("test").resolve(testName);
        Path inputPath = basePath.resolve("input");
        Path expectedPath = basePath.resolve("output_expected");
        CompareOptions compareOptions = new CompareOptions();
        compareOptions.inputPath = inputPath;
        compareOptions.outputPath = basePath.resolve("output_actual");
        compareOptions.compareAlgorithm = CompareAlgorithm.DYNAMIC;
        compareOptions.completeLattices = true;
        compareOptions.applyPostprocessing = true;
        compareOptions.svgGenerationTimeout = 60;
        compareOptions.modelType = ModelType.CMI;
        compareOptions.cmiCompareMode = CmiCompareMode.AUTOMATIC;
        compareOptions.colorScheme = HslColorScheme.INTUITIVE;
        compareOptions.unionIntersectionSizeLimit = Integer.MAX_VALUE;
        compareOptions.structuralCompareSizeLimit = Integer.MAX_VALUE;

        MidsCompare.performCompare(compareOptions,
                Clock.fixed(Instant.parse("2021-01-01T00:00:00.00Z"), ZoneId.of("UTC")), new NullProgressMonitor());

        assertTrue(Files.exists(compareOptions.outputPath));
        assertTrue(Files.isDirectory(compareOptions.outputPath));

        FileCompare.checkDirectoriesEqual(expectedPath, compareOptions.outputPath, MidsCompareTest::isCompareFile);
    }

    /**
     * Apply compare tool to invalid input and check that correct exception is thrown.
     * 
     * @param testName Name of the root folder of test input.
     * 
     * @note There is no expected output data for these tests, as the compare process will fail before output is
     *     produced.
     */
    @ParameterizedTest
    @ValueSource(strings =
    {"testInvalidCifParse", "testInvalidCifType"})
    public void testInvalidCifInputModels(String testName) {
        Path basePath = Paths.get("test").resolve(testName);
        Path inputPath = basePath.resolve("input");
        CompareOptions compareOptions = new CompareOptions();
        compareOptions.inputPath = inputPath;
        compareOptions.outputPath = basePath.resolve("output_actual");
        compareOptions.compareAlgorithm = CompareAlgorithm.DYNAMIC;
        compareOptions.applyPostprocessing = true;
        compareOptions.svgGenerationTimeout = 60;
        compareOptions.modelType = ModelType.CIF;
        compareOptions.cmiCompareMode = CmiCompareMode.AUTOMATIC;
        compareOptions.colorScheme = HslColorScheme.INTUITIVE;
        compareOptions.unionIntersectionSizeLimit = Integer.MAX_VALUE;
        compareOptions.structuralCompareSizeLimit = Integer.MAX_VALUE;

        assertThrows(RuntimeException.class, () -> {
            MidsCompare.performCompare(compareOptions,
                    Clock.fixed(Instant.parse("2021-01-01T00:00:00.00Z"), ZoneId.of("UTC")), new NullProgressMonitor());
        }, "Expected RuntimeException.");
    }

    /**
     * Determine if a given path refers to a file that should be compared.
     *
     * @param p {@link Path} to check.
     * @return {@code false} if the path refers to a SVG file, {@code true} otherwise.
     */
    private static boolean isCompareFile(Path p) {
        // Don't compare SVG files.
        if (FilenameUtils.isExtension(p.getFileName().toString(), SVG_FILE_EXTENSION)) {
            return false;
        }

        return true;
    }
}
