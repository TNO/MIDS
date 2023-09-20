/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.compare.options;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.cli.ParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class CompareOptionsTest {
    CompareOptions defaultOptions = new CompareOptions();

    @Test
    public void performHelpTest() throws ParseException, IOException {
        String[] args = {"-h"};

        assertNull(CompareOptions.parse(args));
    }

    @Test
    public void performNoInputParseTest() throws IOException {
        Path outputPath = Paths.get("path/to/other/folder");
        String[] args = {"-output", outputPath.toString()};

        assertThrows(RuntimeException.class, () -> CompareOptions.parse(args));
    }

    @Test
    public void performInputNotExistsValidateTest() throws IOException {
        Path inputPath = Paths.get("test/notExist");
        Path outputPath = Paths.get("path/to/other/folder");
        String[] args = {"-input", inputPath.toString(), "-output", outputPath.toString()};

        assertThrows(RuntimeException.class, () -> CompareOptions.parse(args));
    }

    @Test
    public void performInputNotFolderValidateTest() throws IOException {
        Path inputPath = Paths.get("test/testOptions/baseConfig.txt");
        Path outputPath = Paths.get("path/to/other/folder");
        String[] args = {"-input", inputPath.toString(), "-output", outputPath.toString()};

        assertThrows(RuntimeException.class, () -> CompareOptions.parse(args));
    }

    @Test
    public void performNoOutputParseTest() throws ParseException, IOException {
        Path inputPath = Paths.get("test/testOptions");
        Path outputPath = inputPath.resolveSibling("output");
        String[] args = {"-input", inputPath.toString()};

        CompareOptions compareOptions = CompareOptions.parse(args);

        assertEquals(inputPath.toAbsolutePath(), compareOptions.inputPath);
        assertEquals(outputPath.toAbsolutePath(), compareOptions.outputPath);
    }

    @Test
    public void performOutputOverlapValidateTest() throws IOException {
        Path inputPath = Paths.get("test/testOptions");
        Path outputPath = Paths.get("test/testOptions/output");
        String[] args = {"-input", inputPath.toString(), "-output", outputPath.toString()};

        assertThrows(RuntimeException.class, () -> CompareOptions.parse(args));
    }

    @Test
    public void performOutputNotFolderValidateTest() throws IOException {
        Path inputPath = Paths.get("test/testOptions");
        Path outputPath = Paths.get("test/testOptions/baseConfig.txt");
        String[] args = {"-input", inputPath.toString(), "-output", outputPath.toString()};

        assertThrows(RuntimeException.class, () -> CompareOptions.parse(args));
    }

    @Test
    public void performBasicParseTest() throws ParseException, IOException {
        Path inputPath = Paths.get("test/testOptions");
        Path outputPath = Paths.get("path/to/other/folder");
        String[] args = {"-input", inputPath.toString(), "-output", outputPath.toString()};

        CompareOptions compareOptions = CompareOptions.parse(args);

        assertEquals(inputPath.toAbsolutePath(), compareOptions.inputPath);
        assertEquals(outputPath.toAbsolutePath(), compareOptions.outputPath);
        assertEquals(defaultOptions.applyPostprocessing, compareOptions.applyPostprocessing);
        assertEquals(defaultOptions.cmiCompareMode, compareOptions.cmiCompareMode);
        assertEquals(defaultOptions.entityType, compareOptions.entityType);
        assertEquals(defaultOptions.colorScheme, compareOptions.colorScheme);
        assertEquals(defaultOptions.compareAlgorithm, compareOptions.compareAlgorithm);
        assertEquals(defaultOptions.completeLattices, compareOptions.completeLattices);
        assertEquals(defaultOptions.showComputedVariants, compareOptions.showComputedVariants);
        assertEquals(defaultOptions.modelType, compareOptions.modelType);
        assertEquals(defaultOptions.structuralCompareSizeLimit, compareOptions.structuralCompareSizeLimit);
        assertEquals(defaultOptions.svgGenerationTimeout, compareOptions.svgGenerationTimeout);
        assertEquals(defaultOptions.unionIntersectionSizeLimit, compareOptions.unionIntersectionSizeLimit);
    }

    @Test
    public void performNoPostprocessingParseTest() throws ParseException, IOException {
        Path inputPath = Paths.get("test/testOptions");
        Path outputPath = Paths.get("path/to/other/folder");
        String[] args = {"-input", inputPath.toString(), "-output", outputPath.toString(), "-no-post-process"};

        CompareOptions compareOptions = CompareOptions.parse(args);

        assertEquals(inputPath.toAbsolutePath(), compareOptions.inputPath);
        assertEquals(outputPath.toAbsolutePath(), compareOptions.outputPath);
        assertEquals(false, compareOptions.applyPostprocessing);
        assertEquals(defaultOptions.cmiCompareMode, compareOptions.cmiCompareMode);
        assertEquals(defaultOptions.entityType, compareOptions.entityType);
        assertEquals(defaultOptions.colorScheme, compareOptions.colorScheme);
        assertEquals(defaultOptions.compareAlgorithm, compareOptions.compareAlgorithm);
        assertEquals(defaultOptions.completeLattices, compareOptions.completeLattices);
        assertEquals(defaultOptions.showComputedVariants, compareOptions.showComputedVariants);
        assertEquals(defaultOptions.modelType, compareOptions.modelType);
        assertEquals(defaultOptions.structuralCompareSizeLimit, compareOptions.structuralCompareSizeLimit);
        assertEquals(defaultOptions.svgGenerationTimeout, compareOptions.svgGenerationTimeout);
        assertEquals(defaultOptions.unionIntersectionSizeLimit, compareOptions.unionIntersectionSizeLimit);
    }

    @ParameterizedTest
    @EnumSource()
    public void performCmiCompareModeParseTest(CmiCompareMode compareMode) throws ParseException, IOException {
        Path inputPath = Paths.get("test/testOptions");
        Path outputPath = Paths.get("path/to/other/folder");
        String[] args = {"-input", inputPath.toString(), "-output", outputPath.toString(), "-mode",
                compareMode.toString()};

        CompareOptions compareOptions = CompareOptions.parse(args);

        assertEquals(inputPath.toAbsolutePath(), compareOptions.inputPath);
        assertEquals(outputPath.toAbsolutePath(), compareOptions.outputPath);
        assertEquals(defaultOptions.applyPostprocessing, compareOptions.applyPostprocessing);
        assertEquals(compareMode, compareOptions.cmiCompareMode);
        assertEquals(defaultOptions.entityType, compareOptions.entityType);
        assertEquals(defaultOptions.colorScheme, compareOptions.colorScheme);
        assertEquals(defaultOptions.compareAlgorithm, compareOptions.compareAlgorithm);
        assertEquals(defaultOptions.completeLattices, compareOptions.completeLattices);
        assertEquals(defaultOptions.showComputedVariants, compareOptions.showComputedVariants);
        assertEquals(defaultOptions.modelType, compareOptions.modelType);
        assertEquals(defaultOptions.structuralCompareSizeLimit, compareOptions.structuralCompareSizeLimit);
        assertEquals(defaultOptions.svgGenerationTimeout, compareOptions.svgGenerationTimeout);
        assertEquals(defaultOptions.unionIntersectionSizeLimit, compareOptions.unionIntersectionSizeLimit);
    }

    @Test
    public void performEntityTypeParseNoPluralTest() throws ParseException, IOException {
        Path inputPath = Paths.get("test/testOptions");
        Path outputPath = Paths.get("path/to/other/folder");
        String[] args = {"-input", inputPath.toString(), "-output", outputPath.toString(), "-entity-type", "cow"};

        CompareOptions compareOptions = CompareOptions.parse(args);

        assertEquals(inputPath.toAbsolutePath(), compareOptions.inputPath);
        assertEquals(outputPath.toAbsolutePath(), compareOptions.outputPath);
        assertEquals(defaultOptions.applyPostprocessing, compareOptions.applyPostprocessing);
        assertEquals(defaultOptions.cmiCompareMode, compareOptions.cmiCompareMode);
        assertEquals("cow", compareOptions.entityType.getName());
        assertEquals("cows", compareOptions.entityType.getPlural());
        assertEquals(defaultOptions.colorScheme, compareOptions.colorScheme);
        assertEquals(defaultOptions.compareAlgorithm, compareOptions.compareAlgorithm);
        assertEquals(defaultOptions.completeLattices, compareOptions.completeLattices);
        assertEquals(defaultOptions.showComputedVariants, compareOptions.showComputedVariants);
        assertEquals(defaultOptions.modelType, compareOptions.modelType);
        assertEquals(defaultOptions.structuralCompareSizeLimit, compareOptions.structuralCompareSizeLimit);
        assertEquals(defaultOptions.svgGenerationTimeout, compareOptions.svgGenerationTimeout);
        assertEquals(defaultOptions.unionIntersectionSizeLimit, compareOptions.unionIntersectionSizeLimit);
    }

    @Test
    public void performEntityTypeParsePluralTest() throws ParseException, IOException {
        Path inputPath = Paths.get("test/testOptions");
        Path outputPath = Paths.get("path/to/other/folder");
        String[] args = {"-input", inputPath.toString(), "-output", outputPath.toString(), "-entity-type", "box,boxen"};

        CompareOptions compareOptions = CompareOptions.parse(args);

        assertEquals(inputPath.toAbsolutePath(), compareOptions.inputPath);
        assertEquals(outputPath.toAbsolutePath(), compareOptions.outputPath);
        assertEquals(defaultOptions.applyPostprocessing, compareOptions.applyPostprocessing);
        assertEquals(defaultOptions.cmiCompareMode, compareOptions.cmiCompareMode);
        assertEquals("box", compareOptions.entityType.getName());
        assertEquals("boxen", compareOptions.entityType.getPlural());
        assertEquals(defaultOptions.colorScheme, compareOptions.colorScheme);
        assertEquals(defaultOptions.compareAlgorithm, compareOptions.compareAlgorithm);
        assertEquals(defaultOptions.completeLattices, compareOptions.completeLattices);
        assertEquals(defaultOptions.showComputedVariants, compareOptions.showComputedVariants);
        assertEquals(defaultOptions.modelType, compareOptions.modelType);
        assertEquals(defaultOptions.structuralCompareSizeLimit, compareOptions.structuralCompareSizeLimit);
        assertEquals(defaultOptions.svgGenerationTimeout, compareOptions.svgGenerationTimeout);
        assertEquals(defaultOptions.unionIntersectionSizeLimit, compareOptions.unionIntersectionSizeLimit);
    }

    @ParameterizedTest
    @EnumSource()
    public void performColorSchemeParseTest(HslColorScheme scheme) throws ParseException, IOException {
        Path inputPath = Paths.get("test/testOptions");
        Path outputPath = Paths.get("path/to/other/folder");
        String[] args = {"-input", inputPath.toString(), "-output", outputPath.toString(), "-color", scheme.toString()};

        CompareOptions compareOptions = CompareOptions.parse(args);

        assertEquals(inputPath.toAbsolutePath(), compareOptions.inputPath);
        assertEquals(outputPath.toAbsolutePath(), compareOptions.outputPath);
        assertEquals(defaultOptions.applyPostprocessing, compareOptions.applyPostprocessing);
        assertEquals(defaultOptions.cmiCompareMode, compareOptions.cmiCompareMode);
        assertEquals(defaultOptions.entityType, compareOptions.entityType);
        assertEquals(scheme, compareOptions.colorScheme);
        assertEquals(defaultOptions.compareAlgorithm, compareOptions.compareAlgorithm);
        assertEquals(defaultOptions.completeLattices, compareOptions.completeLattices);
        assertEquals(defaultOptions.showComputedVariants, compareOptions.showComputedVariants);
        assertEquals(defaultOptions.modelType, compareOptions.modelType);
        assertEquals(defaultOptions.structuralCompareSizeLimit, compareOptions.structuralCompareSizeLimit);
        assertEquals(defaultOptions.svgGenerationTimeout, compareOptions.svgGenerationTimeout);
        assertEquals(defaultOptions.unionIntersectionSizeLimit, compareOptions.unionIntersectionSizeLimit);
    }

    @ParameterizedTest
    @EnumSource()
    public void performCompareAlgorithmParseTest(CompareAlgorithm algorithm) throws ParseException, IOException {
        Path inputPath = Paths.get("test/testOptions");
        Path outputPath = Paths.get("path/to/other/folder");
        String[] args = {"-input", inputPath.toString(), "-output", outputPath.toString(), "-algorithm",
                algorithm.toString()};

        CompareOptions compareOptions = CompareOptions.parse(args);

        assertEquals(inputPath.toAbsolutePath(), compareOptions.inputPath);
        assertEquals(outputPath.toAbsolutePath(), compareOptions.outputPath);
        assertEquals(defaultOptions.applyPostprocessing, compareOptions.applyPostprocessing);
        assertEquals(defaultOptions.cmiCompareMode, compareOptions.cmiCompareMode);
        assertEquals(defaultOptions.entityType, compareOptions.entityType);
        assertEquals(defaultOptions.colorScheme, compareOptions.colorScheme);
        assertEquals(algorithm, compareOptions.compareAlgorithm);
        assertEquals(defaultOptions.completeLattices, compareOptions.completeLattices);
        assertEquals(defaultOptions.showComputedVariants, compareOptions.showComputedVariants);
        assertEquals(defaultOptions.modelType, compareOptions.modelType);
        assertEquals(defaultOptions.structuralCompareSizeLimit, compareOptions.structuralCompareSizeLimit);
        assertEquals(defaultOptions.svgGenerationTimeout, compareOptions.svgGenerationTimeout);
        assertEquals(defaultOptions.unionIntersectionSizeLimit, compareOptions.unionIntersectionSizeLimit);
    }

    @ParameterizedTest
    @ValueSource(strings =
    {"full", "partial", "none"})
    public void performExtendLatticeParseTest(String arg) throws ParseException, IOException {
        Path inputPath = Paths.get("test/testOptions");
        Path outputPath = Paths.get("path/to/other/folder");
        String[] args = {"-input", inputPath.toString(), "-output", outputPath.toString(), "-extend-lattice", arg};

        CompareOptions compareOptions = CompareOptions.parse(args);

        assertEquals(inputPath.toAbsolutePath(), compareOptions.inputPath);
        assertEquals(outputPath.toAbsolutePath(), compareOptions.outputPath);
        assertEquals(defaultOptions.applyPostprocessing, compareOptions.applyPostprocessing);
        assertEquals(defaultOptions.cmiCompareMode, compareOptions.cmiCompareMode);
        assertEquals(defaultOptions.entityType, compareOptions.entityType);
        assertEquals(defaultOptions.colorScheme, compareOptions.colorScheme);
        assertEquals(defaultOptions.compareAlgorithm, compareOptions.compareAlgorithm);
        assertEquals(!arg.equals("none"), compareOptions.completeLattices);
        assertEquals(arg.equals("full"), compareOptions.showComputedVariants);
        assertEquals(defaultOptions.modelType, compareOptions.modelType);
        assertEquals(defaultOptions.structuralCompareSizeLimit, compareOptions.structuralCompareSizeLimit);
        assertEquals(defaultOptions.svgGenerationTimeout, compareOptions.svgGenerationTimeout);
        assertEquals(defaultOptions.unionIntersectionSizeLimit, compareOptions.unionIntersectionSizeLimit);
    }

    @ParameterizedTest
    @EnumSource()
    public void performModelTypeParseTest(ModelType type) throws ParseException, IOException {
        Path inputPath = Paths.get("test/testOptions");
        Path outputPath = Paths.get("path/to/other/folder");
        String[] args = {"-input", inputPath.toString(), "-output", outputPath.toString(), "-type", type.toString()};

        CompareOptions compareOptions = CompareOptions.parse(args);

        assertEquals(inputPath.toAbsolutePath(), compareOptions.inputPath);
        assertEquals(outputPath.toAbsolutePath(), compareOptions.outputPath);
        assertEquals(defaultOptions.applyPostprocessing, compareOptions.applyPostprocessing);
        assertEquals(defaultOptions.cmiCompareMode, compareOptions.cmiCompareMode);
        assertEquals(defaultOptions.entityType, compareOptions.entityType);
        assertEquals(defaultOptions.colorScheme, compareOptions.colorScheme);
        assertEquals(defaultOptions.compareAlgorithm, compareOptions.compareAlgorithm);
        assertEquals(defaultOptions.completeLattices, compareOptions.completeLattices);
        assertEquals(defaultOptions.showComputedVariants, compareOptions.showComputedVariants);
        assertEquals(type, compareOptions.modelType);
        assertEquals(defaultOptions.structuralCompareSizeLimit, compareOptions.structuralCompareSizeLimit);
        assertEquals(defaultOptions.svgGenerationTimeout, compareOptions.svgGenerationTimeout);
        assertEquals(defaultOptions.unionIntersectionSizeLimit, compareOptions.unionIntersectionSizeLimit);
    }

    @ParameterizedTest
    @ValueSource(ints =
    {0, 500, Integer.MAX_VALUE})
    public void performStructuralCompareSizeLimitParseTest(int sizeLimit) throws ParseException, IOException {
        Path inputPath = Paths.get("test/testOptions");
        Path outputPath = Paths.get("path/to/other/folder");
        String[] args = {"-input", inputPath.toString(), "-output", outputPath.toString(), "-size-limit-level6",
                String.valueOf(sizeLimit)};

        CompareOptions compareOptions = CompareOptions.parse(args);

        assertEquals(inputPath.toAbsolutePath(), compareOptions.inputPath);
        assertEquals(outputPath.toAbsolutePath(), compareOptions.outputPath);
        assertEquals(defaultOptions.applyPostprocessing, compareOptions.applyPostprocessing);
        assertEquals(defaultOptions.cmiCompareMode, compareOptions.cmiCompareMode);
        assertEquals(defaultOptions.entityType, compareOptions.entityType);
        assertEquals(defaultOptions.colorScheme, compareOptions.colorScheme);
        assertEquals(defaultOptions.compareAlgorithm, compareOptions.compareAlgorithm);
        assertEquals(defaultOptions.completeLattices, compareOptions.completeLattices);
        assertEquals(defaultOptions.showComputedVariants, compareOptions.showComputedVariants);
        assertEquals(defaultOptions.modelType, compareOptions.modelType);
        assertEquals(sizeLimit, compareOptions.structuralCompareSizeLimit);
        assertEquals(defaultOptions.svgGenerationTimeout, compareOptions.svgGenerationTimeout);
        assertEquals(defaultOptions.unionIntersectionSizeLimit, compareOptions.unionIntersectionSizeLimit);
    }

    @ParameterizedTest
    @ValueSource(ints =
    {0, 500, Integer.MAX_VALUE})
    public void performsvgGenerationTimeoutParseTest(int timeout) throws ParseException, IOException {
        Path inputPath = Paths.get("test/testOptions");
        Path outputPath = Paths.get("path/to/other/folder");
        String[] args = {"-input", inputPath.toString(), "-output", outputPath.toString(), "-timeout-svg",
                String.valueOf(timeout)};

        CompareOptions compareOptions = CompareOptions.parse(args);

        assertEquals(inputPath.toAbsolutePath(), compareOptions.inputPath);
        assertEquals(outputPath.toAbsolutePath(), compareOptions.outputPath);
        assertEquals(defaultOptions.applyPostprocessing, compareOptions.applyPostprocessing);
        assertEquals(defaultOptions.cmiCompareMode, compareOptions.cmiCompareMode);
        assertEquals(defaultOptions.entityType, compareOptions.entityType);
        assertEquals(defaultOptions.colorScheme, compareOptions.colorScheme);
        assertEquals(defaultOptions.compareAlgorithm, compareOptions.compareAlgorithm);
        assertEquals(defaultOptions.completeLattices, compareOptions.completeLattices);
        assertEquals(defaultOptions.showComputedVariants, compareOptions.showComputedVariants);
        assertEquals(defaultOptions.modelType, compareOptions.modelType);
        assertEquals(defaultOptions.structuralCompareSizeLimit, compareOptions.structuralCompareSizeLimit);
        assertEquals(timeout, compareOptions.svgGenerationTimeout);
        assertEquals(defaultOptions.unionIntersectionSizeLimit, compareOptions.unionIntersectionSizeLimit);
    }

    @ParameterizedTest
    @ValueSource(ints =
    {0, 500, Integer.MAX_VALUE})
    public void performUnionIntersectionSizeLimitParseTest(int sizeLimit) throws ParseException, IOException {
        Path inputPath = Paths.get("test/testOptions");
        Path outputPath = Paths.get("path/to/other/folder");
        String[] args = {"-input", inputPath.toString(), "-output", outputPath.toString(), "-size-limit-level5",
                String.valueOf(sizeLimit)};

        CompareOptions compareOptions = CompareOptions.parse(args);

        assertEquals(inputPath.toAbsolutePath(), compareOptions.inputPath);
        assertEquals(outputPath.toAbsolutePath(), compareOptions.outputPath);
        assertEquals(defaultOptions.applyPostprocessing, compareOptions.applyPostprocessing);
        assertEquals(defaultOptions.cmiCompareMode, compareOptions.cmiCompareMode);
        assertEquals(defaultOptions.entityType, compareOptions.entityType);
        assertEquals(defaultOptions.colorScheme, compareOptions.colorScheme);
        assertEquals(defaultOptions.compareAlgorithm, compareOptions.compareAlgorithm);
        assertEquals(defaultOptions.completeLattices, compareOptions.completeLattices);
        assertEquals(defaultOptions.showComputedVariants, compareOptions.showComputedVariants);
        assertEquals(defaultOptions.modelType, compareOptions.modelType);
        assertEquals(defaultOptions.structuralCompareSizeLimit, compareOptions.structuralCompareSizeLimit);
        assertEquals(defaultOptions.svgGenerationTimeout, compareOptions.svgGenerationTimeout);
        assertEquals(sizeLimit, compareOptions.unionIntersectionSizeLimit);
    }

    @Test
    public void performBaseConfigFileTest() throws ParseException, IOException {
        Path inputPath = Paths.get("test/testOptions");
        Path outputPath = Paths.get("path/to/other/folder");
        Path configPath = Paths.get("test/testOptions/baseConfig.txt");
        String[] args = {"-input", inputPath.toString(), "-output", outputPath.toString(), "-options-file",
                configPath.toString()};

        CompareOptions compareOptions = CompareOptions.parse(args);

        assertEquals(inputPath.toAbsolutePath(), compareOptions.inputPath);
        assertEquals(outputPath.toAbsolutePath(), compareOptions.outputPath);
        assertEquals(defaultOptions.applyPostprocessing, compareOptions.applyPostprocessing);
        assertEquals(defaultOptions.cmiCompareMode, compareOptions.cmiCompareMode);
        assertEquals(defaultOptions.entityType, compareOptions.entityType);
        assertEquals(defaultOptions.colorScheme, compareOptions.colorScheme);
        assertEquals(defaultOptions.compareAlgorithm, compareOptions.compareAlgorithm);
        assertEquals(defaultOptions.completeLattices, compareOptions.completeLattices);
        assertEquals(defaultOptions.showComputedVariants, compareOptions.showComputedVariants);
        assertEquals(defaultOptions.modelType, compareOptions.modelType);
        assertEquals(defaultOptions.structuralCompareSizeLimit, compareOptions.structuralCompareSizeLimit);
        assertEquals(defaultOptions.svgGenerationTimeout, compareOptions.svgGenerationTimeout);
        assertEquals(defaultOptions.unionIntersectionSizeLimit, compareOptions.unionIntersectionSizeLimit);
    }

    @Test
    public void performOtherConfigFileTest() throws ParseException, IOException {
        Path inputPath = Paths.get("test/testOptions");
        Path outputPath = Paths.get("path/to/other/folder");
        Path configPath = Paths.get("test/testOptions/otherConfig.txt");
        String[] args = {"-input", inputPath.toString(), "-output", outputPath.toString(), "-options-file",
                configPath.toString()};

        CompareOptions compareOptions = CompareOptions.parse(args);

        assertEquals(inputPath.toAbsolutePath(), compareOptions.inputPath);
        assertEquals(outputPath.toAbsolutePath(), compareOptions.outputPath);
        assertEquals(defaultOptions.applyPostprocessing, compareOptions.applyPostprocessing);
        assertEquals(defaultOptions.cmiCompareMode, compareOptions.cmiCompareMode);
        assertEquals(defaultOptions.entityType, compareOptions.entityType);
        assertEquals(defaultOptions.colorScheme, compareOptions.colorScheme);
        assertEquals(defaultOptions.compareAlgorithm, compareOptions.compareAlgorithm);
        assertEquals(true, compareOptions.completeLattices);
        assertEquals(true, compareOptions.showComputedVariants);
        assertEquals(defaultOptions.modelType, compareOptions.modelType);
        assertEquals(Integer.MAX_VALUE, compareOptions.structuralCompareSizeLimit);
        assertEquals(defaultOptions.svgGenerationTimeout, compareOptions.svgGenerationTimeout);
        assertEquals(Integer.MAX_VALUE, compareOptions.unionIntersectionSizeLimit);
    }

    @Test
    public void performNonDefaultConfigFileTest() throws ParseException, IOException {
        Path inputPath = Paths.get("test/testOptions");
        Path outputPath = Paths.get("path/to/other/folder");
        Path configPath = Paths.get("test/testOptions/nonDefaultConfig.txt");
        String[] args = {"-input", inputPath.toString(), "-output", outputPath.toString(), "-options-file",
                configPath.toString()};

        CompareOptions compareOptions = CompareOptions.parse(args);

        assertEquals(inputPath.toAbsolutePath(), compareOptions.inputPath);
        assertEquals(outputPath.toAbsolutePath(), compareOptions.outputPath);
        assertEquals(false, compareOptions.applyPostprocessing);
        assertEquals(CmiCompareMode.PROTOCOLS, compareOptions.cmiCompareMode);
        assertEquals("nonDefault", compareOptions.entityType.getName());
        assertEquals("nonDefaults", compareOptions.entityType.getPlural());
        assertEquals(true, compareOptions.completeLattices);
        assertEquals(false, compareOptions.showComputedVariants);
        assertEquals(HslColorScheme.LARGE_RANGE, compareOptions.colorScheme);
        assertEquals(CompareAlgorithm.LIGHTWEIGHT, compareOptions.compareAlgorithm);
        assertEquals(ModelType.CIF, compareOptions.modelType);
        assertEquals(150, compareOptions.structuralCompareSizeLimit);
        assertEquals(150, compareOptions.svgGenerationTimeout);
        assertEquals(150, compareOptions.unionIntersectionSizeLimit);
    }
}
