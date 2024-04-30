/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.compare.options;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FilenameUtils;

import com.google.common.collect.Sets;

import nl.tno.mids.compare.MidsCompare;
import nl.tno.mids.compare.output.util.TemplateUtils;

/**
 * Options to control comparison.
 * 
 * @see MidsCompare#performCompare
 */
public class CompareOptions {
    private final static Set<String> EXTENDED_LATTICE_OPTION_VALUES = Collections
            .unmodifiableSet(Sets.newHashSet("full", "partial", "none"));

    private static final String FILE_NAME = "compare-options.txt";

    private boolean doHelp = false;

    /**
     * The input path to the folder containing the model folders to compare.
     */
    public Path inputPath;

    /**
     * The output path to the folder to write the compare results to.
     */
    public Path outputPath;

    /**
     * Apply post processing to the comparison results (e.g., rewriting of fork/join patterns, tau reduction, etc.).
     */
    public boolean applyPostprocessing = true;

    /**
     * The type of model to compare.
     */
    public ModelType modelType = ModelType.CMI;

    /** The CMI compare mode. */
    public CmiCompareMode cmiCompareMode = CmiCompareMode.AUTOMATIC;

    /**
     * The type of entity to compare.
     * <p>
     * Can be configured with an optional parameter by the user. If not configured, a type will be selected by the model
     * set builder.
     * </p>
     */
    public EntityType entityType = null;

    /**
     * The color scheme to be used in the HTML version of the model set matrix
     */
    public HslColorScheme colorScheme = HslColorScheme.INTUITIVE;

    /**
     * The choice of the algorithm to perform structural comparison with.
     */
    public CompareAlgorithm compareAlgorithm = CompareAlgorithm.DYNAMIC;

    /**
     * Compute models and model sets to complete the lattices of level 2 and 5.
     */
    public boolean completeLattices = false;

    /**
     * Show computed models and model sets in other levels, besides levels 2 and 5.
     */
    public boolean showComputedVariants = false;

    /**
     * The timeout in seconds for generating SVG files.
     */
    public int svgGenerationTimeout = 60;

    /**
     * The maximum size of an automaton (measured in the number of states) to be considered for union and intersection.
     */
    public int unionIntersectionSizeLimit = 100;

    /**
     * The maximum size of an automaton (measured in the number of states) to be considered for structural comparison.
     */
    public int structuralCompareSizeLimit = 5000;

    /**
     * Construct default {@link CompareOptions} instance.
     */
    public CompareOptions() {
    }

    /**
     * Create {@link CompareOptions} instance based on command line arguments.
     * 
     * @param args Command line arguments describing configured options.
     * @return The parsed compare options, or {@code null} if the {@code help} argument is present.
     * @throws ParseException In case the provided arguments cannot be parsed.
     * @throws IOException In case of an I/O error.
     */
    public static CompareOptions parse(String[] args) throws ParseException, IOException {
        DefaultParser parser = new DefaultParser();

        Options options = buildOptions();
        CommandLine line = parser.parse(options, args);

        CompareOptions compareOptions = new CompareOptions();

        if (line.hasOption("options-file")) {
            List<String> configLines = Files.readAllLines(Paths.get(line.getOptionValue("options-file")));
            CommandLine lineFromFile = parser.parse(options, configLines.toArray(new String[0]));

            processLine(compareOptions, lineFromFile);
        }

        processLine(compareOptions, line);

        if (compareOptions.doHelp) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("mids-compare", options, true);
            return null;
        }

        finalize(compareOptions);

        validate(compareOptions);

        return compareOptions;
    }

    /**
     * Write options to a text file.
     * 
     * @param optionsFileOutputPath Path of folder to write output to.
     * @throws IOException In case of an I/O error.
     */
    public void writeOptionsFile(Path optionsFileOutputPath) throws IOException {
        List<String> lines = new ArrayList<>();

        lines.add("-input");
        lines.add(FilenameUtils.separatorsToUnix(inputPath.toString()));
        lines.add("-output");
        lines.add(FilenameUtils.separatorsToUnix(outputPath.toString()));

        if (!applyPostprocessing) {
            lines.add("-no-post-process");
        }

        lines.add("-type");
        lines.add(displayEnumValue(modelType.toString()));
        lines.add("-mode");
        lines.add(displayEnumValue(cmiCompareMode.toString()));

        if (entityType != null) {
            lines.add("-entity");
            lines.add(entityType.getName() + "," + entityType.getPlural());
        }

        lines.add("-color");
        lines.add(displayEnumValue(colorScheme.toString()));
        lines.add("-algorithm");
        lines.add(displayEnumValue(compareAlgorithm.toString()));

        lines.add("-extend-lattice");
        if (showComputedVariants) {
            lines.add("full");
        } else if (completeLattices) {
            lines.add("partial");
        } else {
            lines.add("none");
        }

        lines.add("-timeout-svg");
        lines.add(String.valueOf(svgGenerationTimeout));
        lines.add("-size-limit-level5");
        lines.add(String.valueOf(unionIntersectionSizeLimit));
        lines.add("-size-limit-level6");
        lines.add(String.valueOf(structuralCompareSizeLimit));

        TemplateUtils.write(optionsFileOutputPath, FILE_NAME, String.join("\n", lines));
    }

    /**
     * Finalize compare options by adding remaining defaults.
     * 
     * @param compareOptions Compare options to finalize.
     */
    private static void finalize(CompareOptions compareOptions) {
        if (compareOptions.inputPath != null && compareOptions.outputPath == null) {
            compareOptions.outputPath = compareOptions.inputPath.resolveSibling("output");
        }
    }

    /**
     * Validate selected compare options.
     * 
     * @param compareOptions Compare options to validate.
     */
    private static void validate(CompareOptions compareOptions) {
        if (compareOptions.inputPath == null) {
            throw new RuntimeException("Missing required path to input folder.");
        }

        if (!Files.exists(compareOptions.inputPath)) {
            throw new RuntimeException("Input path must refer to an existing input folder.");
        }

        if (!Files.isDirectory(compareOptions.inputPath)) {
            throw new RuntimeException("Input path must refer to an input folder.");
        }

        if (compareOptions.outputPath.startsWith(compareOptions.inputPath)) {
            throw new RuntimeException("Output folder may not be contained in input folder.");
        }

        if (Files.exists(compareOptions.outputPath) && !Files.isDirectory(compareOptions.outputPath)) {
            throw new RuntimeException("Output path may not refer to an existing file.");
        }
    }

    /**
     * Create {@link CompareOptions} instance based on command line arguments.
     * 
     * @param compareOptions The current compare options, to be updated.
     * @param line Command line with parsed arguments.
     * @throws ParseException In case the provided arguments cannot be parsed.
     */
    private static void processLine(CompareOptions compareOptions, CommandLine line) throws ParseException {
        if (line.hasOption("help")) {
            compareOptions.doHelp = true;
            return;
        }

        if (line.hasOption("input")) {
            compareOptions.inputPath = Paths.get(line.getOptionValue("input")).toAbsolutePath().normalize();
        }

        if (line.hasOption("output")) {
            compareOptions.outputPath = Paths.get(line.getOptionValue("output")).toAbsolutePath().normalize();
        }

        compareOptions.applyPostprocessing = compareOptions.applyPostprocessing && !line.hasOption("no-post-process");

        compareOptions.modelType = ModelType
                .valueOf(normalizeEnumValue(line.getOptionValue("type", compareOptions.modelType.toString())));

        compareOptions.cmiCompareMode = CmiCompareMode
                .valueOf(normalizeEnumValue(line.getOptionValue("mode", compareOptions.cmiCompareMode.toString())));

        if (line.hasOption("entity-type")) {
            String entityType = line.getOptionValue("entity-type");

            Pattern entityTypePattern = Pattern.compile("(?<name>[^,]+)(,(?<plural>[^,]+))?");
            Matcher entityTypeMatcher = entityTypePattern.matcher(entityType);

            if (entityTypeMatcher.matches()) {
                String name = entityTypeMatcher.group("name").trim();
                String plural = entityTypeMatcher.group("plural");
                if (plural == null) {
                    compareOptions.entityType = new EntityType(name);
                } else {
                    compareOptions.entityType = new EntityType(name, plural.trim());
                }
            } else {
                throw new ParseException("Cannot extract entity type from " + entityType);
            }
        }

        compareOptions.colorScheme = HslColorScheme
                .valueOf(normalizeEnumValue(line.getOptionValue("color", compareOptions.colorScheme.toString())));

        compareOptions.compareAlgorithm = CompareAlgorithm.valueOf(
                normalizeEnumValue(line.getOptionValue("algorithm", compareOptions.compareAlgorithm.toString())));

        if (line.hasOption("extend-lattice")) {
            String extendLatticeValue = line.getOptionValue("extend-lattice");

            if (!EXTENDED_LATTICE_OPTION_VALUES.contains(extendLatticeValue)) {
                throw new ParseException("Argument " + extendLatticeValue + "not allowed for extended lattice option");
            }

            compareOptions.completeLattices = !extendLatticeValue.equals("none");

            compareOptions.showComputedVariants = extendLatticeValue.equals("full");
        }

        compareOptions.svgGenerationTimeout = Integer
                .valueOf(line.getOptionValue("timeout-svg", Integer.toString(compareOptions.svgGenerationTimeout)));

        compareOptions.unionIntersectionSizeLimit = Integer.valueOf(
                line.getOptionValue("size-limit-level5", Integer.toString(compareOptions.unionIntersectionSizeLimit)));

        compareOptions.structuralCompareSizeLimit = Integer.valueOf(
                line.getOptionValue("size-limit-level6", Integer.toString(compareOptions.structuralCompareSizeLimit)));
    }

    /**
     * Construct CLI options.
     * 
     * @return Constructed options object.
     */
    private static Options buildOptions() {
        Options options = new Options();

        Option helpOption = Option.builder("h").longOpt("help").desc("Print help information").build();
        options.addOption(helpOption);

        Option inputOption = Option.builder("i").longOpt("input").argName("path").hasArg().desc("Input folder").build();
        options.addOption(inputOption);

        Option outputOption = Option.builder("o").longOpt("output").argName("path").hasArg().desc("Output folder")
                .build();
        options.addOption(outputOption);

        Option optionsFileOption = Option.builder("f").longOpt("options-file").argName("path").hasArg()
                .desc("Options file").build();
        options.addOption(optionsFileOption);

        Option postprocessOption = Option.builder("p").longOpt("no-post-process")
                .desc("Skip post-processing level 6 compare results").build();
        options.addOption(postprocessOption);

        Option typeOption = Option.builder("t").longOpt("type").argName("type").hasArg().desc("Input data type")
                .build();
        options.addOption(typeOption);

        Option modeOption = Option.builder("m").longOpt("mode").argName("mode").hasArg().desc("CMI compare mode")
                .build();
        options.addOption(modeOption);

        Option entityTypeOption = Option.builder("e").longOpt("entity-type").argName("type").hasArg()
                .desc("Entity type").build();
        options.addOption(entityTypeOption);

        Option colorOption = Option.builder("c").longOpt("color").argName("scheme").hasArg()
                .desc("Color scheme (level 3)").build();
        options.addOption(colorOption);

        Option algorithmOption = Option.builder("a").longOpt("algorithm").argName("algorithm").hasArg()
                .desc("Compare algorithm (level 6)").build();
        options.addOption(algorithmOption);

        Option extendLatticeOption = Option.builder("x").longOpt("extend-lattice").argName("mode").hasArg()
                .desc("Extended lattice mode (none, partial, full)").build();
        options.addOption(extendLatticeOption);

        Option timeoutOption = Option.builder("ts").longOpt("timeout-svg").argName("time").hasArg()
                .desc("SVG generation timeout in seconds").build();
        options.addOption(timeoutOption);

        Option unionSizeLimitOption = Option.builder("l5").longOpt("size-limit-level5").argName("limit").hasArg()
                .desc("Union/intersection size limit (level 5)").build();
        options.addOption(unionSizeLimitOption);

        Option structuralSizeLimitOption = Option.builder("l6").longOpt("size-limit-level6").argName("limit").hasArg()
                .desc("Structural compare size limit (level 6)").build();
        options.addOption(structuralSizeLimitOption);

        return options;
    }

    /**
     * Create a user-friendly representation of an enum value.
     * 
     * @param value Enumeration value string.
     * @return {@code value} in lowercase and with {@code _} replaced by {@code -}.
     */
    private static String displayEnumValue(String value) {
        return value.replace("_", "-").toLowerCase(Locale.US);
    }

    /**
     * Normalize the string representation of an enumeration value.
     * 
     * @param value Enumeration value string.
     * @return {@code value} trimmed, in uppercase and with {@code -} replaced by {@code _}.
     */
    private static String normalizeEnumValue(String value) {
        return value.trim().replace("-", "_").toUpperCase(Locale.US);
    }
}
