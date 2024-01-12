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

import com.google.common.base.Preconditions
import java.io.FileOutputStream
import java.io.IOException
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.ArrayList
import java.util.List
import java.util.Locale
import java.util.Map
import nl.esi.pps.tmsc.Event
import nl.esi.pps.tmsc.TMSC
import nl.tno.mids.cif.extensions.CIFOperations
import nl.tno.mids.cif.extensions.CifExtensions
import nl.tno.mids.cif.extensions.FileExtensions
import nl.tno.mids.cmi.api.protocol.CmiProtocolQueries
import nl.tno.mids.cmi.cmi2yed.CmiToYedTransformer
import nl.tno.mids.cmi.postprocessing.PostProcessing
import nl.tno.mids.cmi.protocol.InferProtocolModel
import nl.tno.mids.cmi.utils.TmscMetrics
import nl.tno.mids.pps.extensions.util.TmscFileHelper
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.SubMonitor
import org.eclipse.emf.common.util.WrappedException
import org.eclipse.escet.cif.metamodel.cif.Specification
import org.eclipse.escet.common.java.DateTimeUtils

import static org.eclipse.escet.common.java.Strings.fmt

class ComponentExtraction {
    /**
     * Component extraction using Constructive Model Inference.
     * 
     * @param relativeResolvePath The absolute path of the input file. Either the TMSC or options file.
     *      During model extraction, all paths are to be resolved against the parent folder of this file.
     * @param options The configuration options.
     * @param monitor The progress monitor.
     */
    def void extract(Path relativeResolvePath, ComponentExtractionOptions options, IProgressMonitor monitor) {

        val startTime = LocalDateTime.now()
        val warnings = new ArrayList
        val yEdWork = 10

        val tmscPath = options.input.path

        // Precondition checks.
        Preconditions.checkArgument(tmscPath.isAbsolute)
        Preconditions.checkArgument(relativeResolvePath.isAbsolute)

        // Progress monitor preparation.
        var work = 125
        work += options.output.saveYed ? yEdWork : 0

        val subMonitor = SubMonitor.convert(monitor, work)

        // Get TMSC name.
        val tmscName = getTmscName(tmscPath)

        // Save options.
        subMonitor.split(5)
        subMonitor.subTask("Saving model extraction options")

        saveOptions(options)

        // Load TMSC.
        subMonitor.split(10)
        subMonitor.subTask("Loading TMSC from " + tmscName)

        val tmsc = TmscFileHelper.loadAndPrepareTMSC(tmscPath, warnings)

        // Pre-processing.
        subMonitor.subTask("Pre-processing TMSC from " + tmscName)
        preProcess(tmsc, tmscName, options, subMonitor.split(10))

        // Get TMSC metrics.
        subMonitor.split(5)
        subMonitor.subTask("Calculation TMSC metrics")
        val tmscMetrics = getTmscMetrics(tmsc)

        // Model extraction.
        subMonitor.split(50)
        subMonitor.subTask("Extracting models from TMSC " + tmscName)
        val builder = new ComponentModelBuilder(options.extraction.synchronizeDependentTransitions)
        builder.insert(tmsc)
        var Map<String, Specification> modelsMap = builder.cifModels

        // Post-processing.
        subMonitor.subTask("Post-processing CIF models extracted from " + tmscName)

        if (!options.postProcessing.componentsInclusionRegEx.trim.isEmpty) {
            modelsMap = modelsMap.filter[k, v|k.matches(options.postProcessing.componentsInclusionRegEx)]
        }
        if (!options.postProcessing.componentsExclusionRegEx.trim.isEmpty) {
            modelsMap = modelsMap.filter[k, v|!k.matches(options.postProcessing.componentsExclusionRegEx)]
        }
        modelsMap = PostProcessing.postProcess(modelsMap, relativeResolvePath, options.postProcessing.operations,
            subMonitor.split(10))

        // Combine CIF component models into single specification, if desired.
        subMonitor.split(5)
        if (options.output.outputMode == OutputMode.COMPONENTS && !options.extraction.modelPerComponent) {
            subMonitor.subTask("Combining component models")
            combineComponentModels(modelsMap)
        }

        // Based on output mode, produce output models.
        val outputMode = options.output.outputMode

        switch (outputMode) {
            case COMPONENTS: {
                // If producing component models, save CIF models.
                subMonitor.subTask("Saving CIF models")
                saveCifModels(modelsMap, options, subMonitor.split(30))
            }
            case PROTOCOL: {
                // If producing protocol, generate protocol model.
                subMonitor.subTask("Inferring protocol")
                val protocolModel = InferProtocolModel.createProtocol(options.output.protocolName1,
                    options.output.protocolName2, options.output.scope, modelsMap, subMonitor.split(29))

                // Save generated protocol model.
                modelsMap.clear
                modelsMap.put(CmiProtocolQueries.getProtocolName(protocolModel), protocolModel)
                saveCifModels(modelsMap, options, subMonitor.split(1))
            }
        }

        if (options.output.saveYed) {
            // Save yEd representations of generated models.
            subMonitor.split(yEdWork)
            val outputFolderAbsolutePath = createOutputFolder(options)
            modelsMap.forEach [ modelName, spec |
                {
                    val outputFileAbsolutePath = outputFolderAbsolutePath.resolve(fmt("%s.graphml", modelName))
                    CmiToYedTransformer.transform(spec, outputFileAbsolutePath)
                }
            ]

        }

        // Save CMI report, e.g. containing running time.
        val endTime = LocalDateTime.now()
        saveReport(options, tmscMetrics, warnings, startTime.until(endTime, ChronoUnit.MILLIS))
    }

    protected def saveReport(ComponentExtractionOptions options, TmscMetrics tmscMetrics, List<String> warnings,
        long runtimeMs) {

        val targetFolder = createOutputFolder(options)
        val targetFile = targetFolder.resolve("component-extraction-report.txt")

        try (val writer = new PrintWriter(new FileOutputStream(targetFile.toFile))) {
            writer.format(Locale.US, "TMSC duration: %s\n",
                tmscMetrics.duration.toString().substring(2).replaceAll("(\\d[HMS])(?!$)", "$1 ").toLowerCase())
            writer.format(Locale.US, "TMSC events:   %,d\n", tmscMetrics.eventCount);
            writer.format(Locale.US, "Running time:  %s\n", DateTimeUtils.durationToString(runtimeMs, true))
            if (!warnings.isEmpty()) {
                writer.append("Warnings:\n");
                for (String warning : warnings) {
                    writer.format(Locale.US, " - %s\n", warning);
                }
            }
        } catch (IOException e) {
            throw new WrappedException("Failed to save report to file.", e)
        }
    }

    protected def saveOptions(ComponentExtractionOptions options) {
        val targetFolder = createOutputFolder(options)
        try {
            options.writeOptionsFile(targetFolder)
        } catch (IOException e) {
            throw new WrappedException("Failed to save options to file.", e)
        }
    }

    protected def Path createOutputFolder(ComponentExtractionOptions options) {
        val targetFolder = options.output.path
        Files.createDirectories(targetFolder)
        return targetFolder
    }

    private def void preProcess(TMSC tmsc, String tmscName, ComponentExtractionOptions options,
        IProgressMonitor monitor) {

        val subMonitor = SubMonitor.convert(monitor, 3)

        subMonitor.subTask("Excluding components: " + tmscName)
        subMonitor.split(1)
        val noEvts = tmsc.events.size
        subMonitor.split(1)

        subMonitor.split(1)
        if (tmsc.events.size != noEvts) {
            subMonitor.subTask("Saving pre-processed TMSC: " + tmscName)
            val targetFolder = createOutputFolder(options)
            val targetFile = targetFolder.resolve(tmscName + "-preprocessed.tmscz")
            TmscFileHelper.saveTMSC(tmsc, targetFile)
        }
    }

    private def combineComponentModels(Map<String, Specification> modelsMap) {
        val mergedSpec = CIFOperations.mergeSpecifications(modelsMap.values)
        modelsMap.clear
        modelsMap.put("allComponents", mergedSpec)
    }

    private def saveCifModels(Map<String, Specification> namedSpecs, ComponentExtractionOptions options,
        IProgressMonitor monitor) {

        val subMonitor = SubMonitor.convert(monitor, namedSpecs.size);
        val targetFolder = createOutputFolder(options)

        namedSpecs.forEach [ name, cif |
            subMonitor.split(1)
            val targetFile = targetFolder.resolve(name + ".cif")
            CifExtensions.normalizeOrder(cif)
            FileExtensions.saveCIF(cif, targetFile)
        ]
    }

    private def String getTmscName(Path tmscPath) {
        var tmscName = tmscPath.last.toString
        val idx = tmscName.lastIndexOf('.')
        if (idx >= 0) {
            tmscName = tmscName.substring(0, idx)
        }
        return tmscName
    }

    private def getTmscMetrics(TMSC tmsc) {
        val tmscMetrics = new TmscMetrics()

        var Long firstTimestamp = null
        var Long lastTimestamp = null

        for (Event event : tmsc.events) {
            tmscMetrics.eventCount++

            if (firstTimestamp === null) {
                firstTimestamp = event.timestamp
                lastTimestamp = event.timestamp
            } else {
                firstTimestamp = Long.min(firstTimestamp, event.timestamp)
                lastTimestamp = Long.max(lastTimestamp, event.timestamp)
            }
        }

        tmscMetrics.duration = Duration.ofNanos(lastTimestamp - firstTimestamp)

        return tmscMetrics
    }
}
