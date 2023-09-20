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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.ParseException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubMonitor;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import nl.tno.mids.compare.compute.CompleteModelLattice;
import nl.tno.mids.compare.compute.CompleteModelSetLattice;
import nl.tno.mids.compare.compute.ComputeModelLattice;
import nl.tno.mids.compare.compute.ComputeModelSetDifferences;
import nl.tno.mids.compare.compute.ComputeModelSetLattice;
import nl.tno.mids.compare.compute.ComputeStructuralDifferences;
import nl.tno.mids.compare.compute.ComputeVariants;
import nl.tno.mids.compare.data.ComparisonData;
import nl.tno.mids.compare.data.ModelSet;
import nl.tno.mids.compare.input.ModelSetLoader;
import nl.tno.mids.compare.options.CompareOptions;
import nl.tno.mids.compare.output.AboutInfoWriter;
import nl.tno.mids.compare.output.IndexWriter;
import nl.tno.mids.compare.output.Level1Writer;
import nl.tno.mids.compare.output.Level2Writer;
import nl.tno.mids.compare.output.Level3Writer;
import nl.tno.mids.compare.output.Level4Writer;
import nl.tno.mids.compare.output.Level5Writer;
import nl.tno.mids.compare.output.VariantModelWriter;
import nl.tno.mids.compare.output.util.AssetController;

/**
 * Implementation of full compare sequence.
 */
public class MidsCompare {
    private static final Logger LOGGER = LoggerFactory.getLogger(MidsCompare.class);

    /** Version number of the tool. */
    public static final String VERSION = getAppVersion();

    /**
     * Compare sets of CIF models.
     * 
     * @param args Command line arguments:
     */
    public static void main(String[] args) {
        CompareOptions compareOptions;
        try {
            compareOptions = CompareOptions.parse(args);
        } catch (ParseException e) {
            throw new RuntimeException("Error parsing comand line arguments.", e);
        } catch (IOException e) {
            throw new RuntimeException("Error reading options file.", e);
        }
        // No options were parsed, usage information was printed, and we're done.
        if (compareOptions == null) {
            return;
        }

        performCompare(compareOptions, Clock.systemDefaultZone(), new NullProgressMonitor());
    }

    /**
     * Perform compare on CIF models from a given path.
     * 
     * @param compareOptions {@link CompareOptions}
     * @param clock Clock to use to determine start and end time of the comparison.
     * @param monitor Progress monitor used to report progress.
     */
    public static void performCompare(CompareOptions compareOptions, Clock clock, IProgressMonitor monitor) {
        SubMonitor subMonitor = SubMonitor.convert(monitor, 60 + (compareOptions.completeLattices ? 2 : 0));
        Instant startTime = clock.instant();

        Preconditions.checkArgument(Files.exists(compareOptions.inputPath), "The inputPath '%s' does not exist",
                compareOptions.inputPath);
        Preconditions.checkNotNull(compareOptions.outputPath, "The output path is mandatory");
        Preconditions.checkNotNull(compareOptions.modelType, "The model type is mandatory");
        Preconditions.checkNotNull(compareOptions.colorScheme, "The color scheme is mandatory");
        Preconditions.checkNotNull(compareOptions.compareAlgorithm, "The compare algorithm is mandatory");

        // Clear output left over from previous a run.
        subMonitor.split(1);
        LOGGER.info("Clearing output");
        if (Files.exists(compareOptions.outputPath)) {
            clearOutput(compareOptions.outputPath);
        }

        List<String> warnings = new ArrayList<>();

        // Output assets supporting the generated HTML files.
        subMonitor.split(1);
        try {
            LOGGER.info("Writing HTML assets");
            AssetController.generateHTMLAssets(compareOptions.outputPath);
        } catch (Exception e) {
            throw new RuntimeException("Error writing HTML assets to disk.", e);
        }

        // Load the model sets with input models.
        subMonitor.split(1);
        LOGGER.info("Loading input models");
        List<ModelSet> modelSets = ModelSetLoader.load(compareOptions, warnings);

        // Initialize comparison data.
        LOGGER.info("Initializing comparison");
        ComparisonData comparisonData = new ComparisonData(modelSets);

        // Compute model variants.
        subMonitor.split(1);
        LOGGER.info("Computing model variants");
        ComputeVariants.computeEntityVariants(comparisonData);

        // Compute model set variants.
        subMonitor.split(1);
        LOGGER.info("Computing model set variants");
        ComputeVariants.computeModelSetVariants(comparisonData);

        // Compute model lattices.
        LOGGER.info("Computing model variant relations");
        ComputeModelLattice computeLattice = new ComputeModelLattice(comparisonData);
        computeLattice.computeLattices(subMonitor.split(1));

        if (compareOptions.completeLattices) {
            // Complete model lattices.
            LOGGER.info("Computing extended model variant relations");
            CompleteModelLattice completeModelLattice = new CompleteModelLattice(comparisonData, compareOptions);
            completeModelLattice.completeLattices(subMonitor.split(1), warnings);
        } else {
            comparisonData.getEntities().forEach(e -> e.setLatticeIncomplete());
        }

        // Write representations per entity variant.
        try {
            LOGGER.info("Writing variant models");
            VariantModelWriter.write(comparisonData, compareOptions, warnings, subMonitor.split(4));
        } catch (IOException e) {
            throw new RuntimeException("Error writing variant models to disk.", e);
        }

        // Compute model set variant lattices.
        LOGGER.info("Computing model set relations");
        ComputeModelSetLattice computeModelSetLattice = new ComputeModelSetLattice(comparisonData);
        computeModelSetLattice.computeModelSetLattice(subMonitor.split(1));

        if (compareOptions.completeLattices) {
            // Complete model set lattice.
            subMonitor.split(1);
            LOGGER.info("Computing extended model set relations");
            new CompleteModelSetLattice(comparisonData, compareOptions).completeModelSetLattice(warnings);
        } else {
            comparisonData.setModelSetLatticeIncomplete();
        }

        // Compute differences between model sets.
        subMonitor.split(1);
        LOGGER.info("Computing model set differences");
        ComputeModelSetDifferences computeModelSetDifferences = new ComputeModelSetDifferences(comparisonData);
        computeModelSetDifferences.computeDifferences();

        // Output level 1 view, showing the mapping of model sets to variants.
        subMonitor.split(1);
        try {
            LOGGER.info("Writing level 1 (model set variants)");
            Level1Writer.write(comparisonData, compareOptions.showComputedVariants, compareOptions.outputPath);
        } catch (IOException e) {
            throw new RuntimeException("Error writing level 1 results to disk.", e);
        }

        // Output level 2 view, showing model set variant relations.
        subMonitor.split(1);
        try {
            LOGGER.info("Writing level 2 (model set relations)");
            Level2Writer.write(comparisonData, compareOptions, warnings);
        } catch (IOException e) {
            throw new RuntimeException("Error writing level 2 results to disk.", e);
        }

        // Output level 3 view, showing quantitative differences between model set variants.
        subMonitor.split(1);
        try {
            LOGGER.info("Writing level 3 (model set differences)");
            Level3Writer.write(comparisonData, compareOptions);
        } catch (IOException e) {
            throw new RuntimeException("Error writing level 3 results to disk.", e);
        }

        // Output level 4 view, showing entity variants per model set.
        subMonitor.split(1);
        try {
            LOGGER.info("Writing level 4 (model variants)");
            Level4Writer.write(comparisonData, compareOptions.showComputedVariants, compareOptions.outputPath);
        } catch (IOException e) {
            throw new RuntimeException("Error writing level 4 results to disk.", e);
        }

        // Compute and output level 6 views, showing the differences between model variants.
        try {
            LOGGER.info("Computing and writing level 6 (model variant differences)");
            ComputeStructuralDifferences computeStructuralDifferences = new ComputeStructuralDifferences(comparisonData,
                    compareOptions);
            computeStructuralDifferences.computeAndWriteDifferences(subMonitor.split(40), warnings);
        } catch (IOException e) {
            throw new RuntimeException("Error writing level 6 results to disk.", e);
        }

        // Output level 5 views, showing model variant relations for each entity.
        subMonitor.split(2);
        try {
            LOGGER.info("Writing level 5 (model variant relations)");
            Level5Writer.write(comparisonData, compareOptions, warnings);
        } catch (IOException e) {
            throw new RuntimeException("Error writing level 5 results to disk.", e);
        }

        // Output compare overview.
        subMonitor.split(1);
        try {
            LOGGER.info("Writing compare overview");
            IndexWriter.write(comparisonData, compareOptions.showComputedVariants, compareOptions.outputPath, warnings);
        } catch (Exception e) {
            throw new RuntimeException("Error writing compare overview to disk.", e);
        }

        Instant endTime = clock.instant();

        // Output compare info.
        subMonitor.split(1);
        LOGGER.info("Writing compare about information");
        try {
            AboutInfoWriter.write(comparisonData, compareOptions, startTime, endTime, compareOptions.outputPath);
        } catch (IOException e) {
            throw new RuntimeException("Error writing compare about information to disk.", e);
        }

        LOGGER.info("Writing compare options file");
        try {
            compareOptions.writeOptionsFile(compareOptions.outputPath);
        } catch (IOException e) {
            throw new RuntimeException("Error writing compare options file to disk.", e);
        }

        LOGGER.info("Compare done");
    }

    static void clearOutput(Path outputPath) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(outputPath)) {
            for (Path p: Files.walk(outputPath).sorted((a, b) -> b.compareTo(a)). // reverse; files before dirs
                    toArray(Path[]::new)) {
                Files.delete(p);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not clear output folders.", e);
        }
    }

    /**
     * Returns the version of the application.
     *
     * @return The version of the application.
     * @exception UnsupportedOperationException If OSGI platform is not running, or the application class is not defined
     *     by a bundle class loader.
     */
    private static String getAppVersion() {
        // Get application class.
        Class<?> appClass = MidsCompare.class;

        // Get bundle that contains the application class.
        if (!Platform.isRunning()) {
            String msg = "OSGi Platform is not running.";
            throw new UnsupportedOperationException(msg);
        }
        Bundle bundle = FrameworkUtil.getBundle(appClass);
        if (bundle == null) {
            String msg = "Application class not defined by bundle class loader.";
            throw new UnsupportedOperationException(msg);
        }

        // Return bundle version.
        Version version = bundle.getVersion();
        return String.format("%d.%d.%d", version.getMajor(), version.getMinor(), version.getMicro());
    }
}
