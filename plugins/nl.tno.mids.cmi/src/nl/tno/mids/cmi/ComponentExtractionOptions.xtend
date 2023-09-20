/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////
package nl.tno.mids.cmi

import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.ArrayList
import java.util.Arrays
import java.util.List
import java.util.Locale
import java.util.regex.Pattern
import java.util.stream.Collectors
import nl.tno.mids.cmi.postprocessing.PostProcessingFilterMode
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationOptions
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationProviders
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import org.apache.commons.cli.ParseException
import org.apache.commons.io.FilenameUtils
import org.eclipse.xtend.lib.annotations.Accessors

@Accessors
class ComponentExtractionOptions implements Cloneable {
    final static String FILE_NAME = "component-extraction-options.txt"

    boolean doHelp = false;

    ComponentExtractionInputOptions input = new ComponentExtractionInputOptions()
    ComponentExtractionOutputOptions output = new ComponentExtractionOutputOptions()
    ComponentExtractionPreProcessingOptions preProcessing = new ComponentExtractionPreProcessingOptions()
    ComponentExtractionExtractionOptions extraction = new ComponentExtractionExtractionOptions()
    ComponentExtractionPostProcessingOptions postProcessing = new ComponentExtractionPostProcessingOptions();

    @Accessors
    static class ComponentExtractionInputOptions implements Cloneable {
        /** Absolute path to input TMSC from which to extract models/ */
        Path path

        override clone() {
            return super.clone
        }
    }

    @Accessors
    static class ComponentExtractionOutputOptions implements Cloneable {
        /**
         * Absolute path of the output folder.
         */
        Path path

        /** Kind of output produced. */
        OutputMode outputMode = OutputMode.COMPONENTS

        /**
         * Name of first protocol component.
         * 
         * <p>Is {@code null} if and only if {@link #outputMode} is {@link OutputMode#COMPONENTS}.</p>
         */
        String protocolName1

        /**
         * Name of second protocol component.
         * 
         * <p>Is {@code null} if and only if {@link #outputMode} is {@link OutputMode#COMPONENTS}.</p>
         */
        String protocolName2

        /**
         * Names of additional components in scope. May be empty.
         * 
         * <p>Is {@code null} if and only if {@link #outputMode} is {@link OutputMode#COMPONENTS}.</p>
         */
        List<String> scope = newArrayList

        /** Whether to additionally render extracted models as yEd diagrams. */
        boolean saveYed

        override clone() {
            return super.clone
        }
    }

    @Accessors
    static class ComponentExtractionPreProcessingOptions implements Cloneable {
        /** Whether to convert events on untraced components to represent synchronously handled functions. */
        boolean untracedHandleSynchronously = true

        override clone() {
            return super.clone
        }
    }

    @Accessors
    static class ComponentExtractionPostProcessingOptions implements Cloneable {
        /** Include components matching the given regular expression, or all components on empty string. */
        String componentsInclusionRegEx = ""

        /** Exclude components matching the given regular expression, or no exclusion on empty string. */
        String componentsExclusionRegEx = ""

        /** Apply post-processing operations after component extraction. */
        List<PostProcessingOperationOptions> operations = newArrayList;

        override clone() {
            val rslt = super.clone as ComponentExtractionPostProcessingOptions

            for (i : 0 ..< rslt.operations.size) {
                rslt.operations.set(i, rslt.operations.get(i).clone as PostProcessingOperationOptions)
            }

            return rslt
        }
    }

    @Accessors
    static class ComponentExtractionExtractionOptions implements Cloneable {
        /** Synchronize dependent transitions ({@code true}) or keep them unsynchronized ({@code false}). */
        boolean synchronizeDependentTransitions = true

        /**
         * Whether to exact a CIF model per component ({@code true}) or a single CIF model for all components
         * ({@code false}).
         */
        boolean modelPerComponent = true

        override clone() {
            return super.clone
        }
    }

    override clone() {
        val rslt = super.clone as ComponentExtractionOptions
        rslt.input = rslt.input.clone as ComponentExtractionInputOptions
        rslt.output = rslt.output.clone as ComponentExtractionOutputOptions
        rslt.preProcessing = rslt.preProcessing.clone as ComponentExtractionPreProcessingOptions
        rslt.extraction = rslt.extraction.clone as ComponentExtractionExtractionOptions
        rslt.postProcessing = rslt.postProcessing.clone as ComponentExtractionPostProcessingOptions
        return rslt
    }

    /**
     * Create {@link ComponentExtractionOptions} instance based on command line arguments.
     * 
     * @param args Command line arguments describing configured options.
     * @return The parsed component extraction options, or {@code null} if the {@code help} argument is present.
     * @throws ParseException In case the provided arguments cannot be parsed.
     * @throws IOException In case of an I/O error.
     */
    static def ComponentExtractionOptions parse(String[] cmiArgs) throws IOException{

        val parser = new DefaultParser()

        val options = ComponentExtractionOptions.buildOptions
        val line = parser.parse(options, cmiArgs)

        var extractionOptions = new ComponentExtractionOptions

        if (line.hasOption("options-file")) {
            val configLines = Files.readAllLines(Paths.get(line.getOptionValue("options-file")));
            val String[] configArray = configLines
            val lineFromFile = parser.parse(options, configArray);

            processLine(extractionOptions, lineFromFile);
        }

        processLine(extractionOptions, line)

        if (extractionOptions.doHelp) {
            val formatter = new HelpFormatter
            formatter.printHelp("mids-cmi", options, true)
            return null
        }

        finalize(extractionOptions)

        extractionOptions.validate

        return extractionOptions
    }

    /**
     * Write options to a text file.
     * 
     * @param optionsFileOutputPath Path of folder to write output to.
     * @throws IOException In case of an I/O error.
     */
    def writeOptionsFile(Path optionsFileOutputPath) {
        var lines = new ArrayList

        lines.add("-input")
        lines.add(FilenameUtils.separatorsToUnix(input.path.toString))
        lines.add("-output")
        lines.add(FilenameUtils.separatorsToUnix(output.path.toString))

        if (output.outputMode == OutputMode.PROTOCOL) {
            lines.add("-protocol")
            lines.add(output.protocolName1 + "," + output.protocolName2)
            if (!output.scope.empty) {
                lines.add("-protocol-scope")
                lines.add(output.scope.stream.collect(Collectors.joining(",")))
            }
        }

        if (output.saveYed) {
            lines.add("-yed")
        }

        if (!preProcessing.untracedHandleSynchronously) {
            lines.add("-no-untraced-synchronous")
        }

        if (!extraction.modelPerComponent) {
            lines.add("-single-model")
        }

        if (!extraction.synchronizeDependentTransitions) {
            lines.add("-no-sync-dependent")
        }

        if (!postProcessing.componentsExclusionRegEx.empty) {
            lines.add("-component-exclusion")
            lines.add(postProcessing.componentsExclusionRegEx)
        }

        if (!postProcessing.componentsInclusionRegEx.empty) {
            lines.add("-component-inclusion")
            lines.add(postProcessing.componentsInclusionRegEx)
        }

        for (PostProcessingOperationOptions operationOptions : postProcessing.operations) {
            val provider = operationOptions.provider

            lines.add("-post-processing")
            val operationStringBuilder = new StringBuilder

            if (operationOptions.filterMode != PostProcessingFilterMode.NONE) {
                operationStringBuilder.append("<")
                operationStringBuilder.append(displayEnumValue(operationOptions.filterMode.toString))
                operationStringBuilder.append(",")
                operationStringBuilder.append(operationOptions.filterPattern)
                operationStringBuilder.append(">")
            }

            operationStringBuilder.append(provider.operationClass.simpleName)

            val ppOptions = provider.writeOptions(operationOptions)
            if (!ppOptions.empty) {
                operationStringBuilder.append("(")
                operationStringBuilder.append(ppOptions)
                operationStringBuilder.append(")")
            }

            lines.add(operationStringBuilder.toString)
        }

        val optionsFilePath = optionsFileOutputPath.resolve(FILE_NAME)
        Files.createDirectories(optionsFilePath.getParent())
        Files.write(optionsFilePath, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE)
    }

    /**
     * Finalize extraction options by adding remaining defaults.
     * 
     * @param extractionOptions Compare options to finalize.
     */
    private static def finalize(ComponentExtractionOptions extractionOptions) {
        if (extractionOptions.input.path !== null && extractionOptions.output.path === null) {
            extractionOptions.output.path = extractionOptions.input.path.resolveSibling("cmi")
        }
    }

    /**
     * Validate selected component extraction options.
     * 
     * @param extractionOptions Component extraction options to validate.
     */
    private static def validate(ComponentExtractionOptions extractionOptions) {
        if (extractionOptions.input.path === null) {
            throw new RuntimeException("Missing required path to input data.")
        }

        val inputPath = extractionOptions.input.path
        if (!Files.exists(inputPath)) {
            throw new RuntimeException("Input path must refer to an existing file.");
        }

        if (!Files.isRegularFile(inputPath)) {
            throw new RuntimeException("Input path must refer to an input file, not a folder.");
        }

        val outputPath = extractionOptions.output.path
        if (Files.exists(outputPath) && !Files.isDirectory(outputPath)) {
            throw new RuntimeException("Output path may not refer to an existing file.");
        }
    }

    /**
     * Create {@link ComponentExtractionOptions} instance based on command line arguments.
     * 
     * @param extractionOptions The current component extraction options, to be updated.
     * @param line Command line with parsed arguments.
     * @throws ParseException In case the provided arguments cannot be parsed.
     */
    private static def processLine(ComponentExtractionOptions extractionOptions, CommandLine line) {
        if (line.hasOption("help")) {
            extractionOptions.doHelp = true
            return
        }

        if (line.hasOption("input")) {
            extractionOptions.input.path = Paths.get(line.getOptionValue("input")).toAbsolutePath.normalize
        }

        if (line.hasOption("output")) {
            extractionOptions.output.path = Paths.get(line.getOptionValue("output")).toAbsolutePath.normalize
        }

        if (line.hasOption("p")) {
            extractionOptions.output.outputMode = OutputMode.PROTOCOL
            extractionOptions.output.protocolName1 = line.getOptionValues("p").get(0)
            extractionOptions.output.protocolName2 = line.getOptionValues("p").get(1)
        } else {
            extractionOptions.output.outputMode = OutputMode.COMPONENTS
        }

        if (line.hasOption("protocol-scope")) {
            extractionOptions.output.scope.addAll(line.getOptionValues("protocol-scope"))
        }

        extractionOptions.output.saveYed = extractionOptions.output.saveYed || line.hasOption("yed")

        extractionOptions.preProcessing.untracedHandleSynchronously = extractionOptions.preProcessing.
            untracedHandleSynchronously && !line.hasOption("no-untraced-synchronous")

        extractionOptions.extraction.synchronizeDependentTransitions = extractionOptions.extraction.
            synchronizeDependentTransitions && !line.hasOption("no-sync-dependent")

        extractionOptions.extraction.modelPerComponent = extractionOptions.extraction.modelPerComponent &&
            !line.hasOption("single-model")

        extractionOptions.postProcessing.componentsInclusionRegEx = line.getOptionValue("component-inclusion",
            extractionOptions.postProcessing.componentsInclusionRegEx)

        extractionOptions.postProcessing.componentsExclusionRegEx = line.getOptionValue("component-exclusion",
            extractionOptions.postProcessing.componentsExclusionRegEx)

        if (line.hasOption("post-processing")) {
            val postProcessingPattern = Pattern.compile(
                "(\\<(?<filtermode>\\w*),(?<filterpattern>[^\\>]*)\\>)?(?<name>\\w*)(\\((?<args>.*)\\))?")
            val postProcessings = Arrays.asList(line.getOptionValues("post-processing"))
            for (postProcessing : postProcessings) {
                val match = postProcessingPattern.matcher(postProcessing)
                if (!match.matches) {
                    throw new ParseException("Post-processing operation does not fit expected pattern: " +
                        postProcessing)
                }
                val operationProvider = PostProcessingOperationProviders.
                    getPostProcessingOperationProvider(match.group("name"))
                if (operationProvider === null) {
                    throw new ParseException("Unknown post-processing operation: " + match.group("name"))
                }

                val argsGroup = match.group("args")
                var PostProcessingOperationOptions operation
                if (argsGroup !== null) {
                    operation = operationProvider.getOptions(argsGroup)

                } else {
                    operation = operationProvider.getOptions("")
                }

                val filterMode = match.group("filtermode")
                if (filterMode !== null) {
                    operation.filterMode = PostProcessingFilterMode.valueOf(normalizeEnumValue(filterMode))
                    operation.filterPattern = match.group("filterpattern")
                }

                extractionOptions.postProcessing.operations.add(operation)
            }
        }

        return
    }

    /**
     * Construct CLI options.
     * 
     * @return Constructed options object.
     */
    private static def buildOptions() {
        val options = new Options

        val helpOption = Option.builder("h").longOpt("help").desc("Print help information").build
        options.addOption(helpOption)

        val inputTMSCOption = Option.builder("i").longOpt("input").argName("path").hasArg().desc("Input TMSC file").
            build
        options.addOption(inputTMSCOption)

        val outputOption = Option.builder("o").longOpt("output").argName("path").hasArg().desc(
            "Output folder path").build
        options.addOption(outputOption)

        val optionsFileOption = Option.builder("f").longOpt("options-file").argName("path").hasArg.desc("Options file").
            build
        options.addOption(optionsFileOption)

        val protocolNameOption = Option.builder("p").longOpt("protocol").argName("names").hasArgs().numberOfArgs(2).
            valueSeparator(",").desc("Infer protocol between two components").build
        options.addOption(protocolNameOption)

        val protocolScopeOption = Option.builder("ps").longOpt("protocol-scope").argName("names").hasArgs().
            valueSeparator(",").desc("Additional protocol scope components").build
        options.addOption(protocolScopeOption)

        val saveYedOption = Option.builder("y").longOpt("yed").desc("Save yEd diagrams").build
        options.addOption(saveYedOption)

        val noUntracedHandledSynchronouslyOption = Option.builder("u").longOpt("no-untraced-synchronous").desc(
            "Do not convert events on untraced components to synchronous functions").build
        options.addOption(noUntracedHandledSynchronouslyOption)

        val noSynchronizeDependentOption = Option.builder("d").longOpt("no-sync-dependent").desc(
            "Do not synchronize dependent transitions").build
        options.addOption(noSynchronizeDependentOption)

        val componentInclusionRegexOption = Option.builder("ci").longOpt("component-inclusion").argName("regex").
            hasArg().desc("Component inclusion regex").build
        options.addOption(componentInclusionRegexOption)

        val componentExclusionRegexOption = Option.builder("ce").longOpt("component-exclusion").argName("regex").
            hasArg().desc("Component exclusion regex").build
        options.addOption(componentExclusionRegexOption)

        val postProcessingOption = Option.builder("c").longOpt("post-processing").argName("operation").hasArg().desc(
            "Perform post-processing operation").build
        options.addOption(postProcessingOption)

        val singleModelOption = Option.builder("s").longOpt("single-model").desc("Save single model").build
        options.addOption(singleModelOption)

        return options
    }

    /**
     * Create a user-friendly representation of an enum value.
     * 
     * @param value Enumeration value string.
     * @return {@code value} in lowercase and with {@code _} replaced by {@code -}.
     */
    private static def displayEnumValue(String value) {
        return value.replace("_", "-").toLowerCase(Locale.US)
    }

    /**
     * Normalize the string representation of an enumeration value.
     * 
     * @param value Enumeration value string.
     * @return {@code value} trimmed, in uppercase and with {@code -} replaced by {@code _}.
     */
    private static def normalizeEnumValue(String value) {
        return value.trim.replace("-", "_").toUpperCase(Locale.US)
    }
}
