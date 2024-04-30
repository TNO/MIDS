/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.compare.input;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.common.app.framework.AppEnv;
import org.eclipse.escet.common.app.framework.AppEnvData;
import org.eclipse.escet.common.app.framework.exceptions.InvalidInputException;
import org.eclipse.escet.common.app.framework.io.NullAppStream;
import org.eclipse.escet.common.app.framework.output.IOutputComponent;
import org.eclipse.escet.common.app.framework.output.OutputProvider;
import org.eclipse.escet.common.app.framework.output.StreamOutputComponent;
import org.eclipse.escet.setext.runtime.exceptions.SyntaxException;

import nl.tno.mids.cif.extensions.CIFOperations;
import nl.tno.mids.compare.data.ModelSet;
import nl.tno.mids.compare.options.CompareOptions;

/**
 * Load set of CIF specifications that form a model set.
 */
public class ModelSetLoader {
    /**
     * For each folder under {@code options.inputPath} the models will be loaded as a {@link ModelSet}.
     * 
     * @param options The comparison options, containing information on how the models are to be loaded.
     * @param warnings List to collect warnings generated during comparison.
     * @return The loaded model sets.
     */
    public static List<ModelSet> load(CompareOptions options, List<String> warnings) {
        // Get model set paths.
        List<Path> modelSetPaths = new ArrayList<>();
        try (DirectoryStream<Path> compareRootStream = Files.newDirectoryStream(options.inputPath)) {
            for (Path modelSetPath: compareRootStream) {
                modelSetPaths.add(modelSetPath);
            }
        } catch (IOException | IllegalArgumentException | SecurityException e) {
            throw new RuntimeException("Error finding model sets.", e);
        }

        // Sort model sets, using Java string sorting for the most consistent result across platforms.
        modelSetPaths.sort(Comparator.comparing(p -> p.toString()));

        // Get model sets.
        List<ModelSet> modelSets = new ArrayList<>();
        for (Path modelSetPath: modelSetPaths) {
            // Create model set builder.
            BaseModelSetBuilder modelSetBuilder = options.modelType
                    .getModelSetBuilder(modelSetPath.getFileName().toString(), options);

            // Skip files.
            if (!Files.isDirectory(modelSetPath)) {
                continue;
            }

            // Get model paths.
            List<Path> modelPaths;
            try {
                modelPaths = collectCIFPaths(modelSetPath);
            } catch (IOException e) {
                throw new RuntimeException("Error collecting model paths at: " + modelSetPath, e);
            }

            // Load models.
            for (Path modelPath: modelPaths) {
                // Get specification name.
                String specificationName = modelPath.getFileName().toString();
                int ext = specificationName.lastIndexOf(".");
                if (ext > 0) {
                    specificationName = specificationName.substring(0, ext);
                }

                // Load specification.
                Specification specification;
                try {
                    AppEnv.registerApplication(new AppEnvData(null));
                    NullAppStream stream = new NullAppStream();
                    IOutputComponent output = new StreamOutputComponent(stream, stream);
                    OutputProvider.register(output);
                    try {
                        specification = CIFOperations.loadCIFSpec(modelPath);
                    } finally {
                        AppEnv.unregisterApplication();
                    }
                } catch (SyntaxException e) {
                    throw new RuntimeException("Parse error reading CIF file " + modelPath.toString() + ".", e);
                } catch (InvalidInputException e) {
                    throw new RuntimeException("Type checker error reading CIF file " + modelPath.toString() + ".", e);
                }

                // Add model to model set.
                modelSetBuilder.add(specification, specificationName, warnings);
            }

            // Validate model set.
            modelSetBuilder.validate();

            // Find model set descriptions.
            List<Path> descriptionPaths;
            try {
                descriptionPaths = collectDescriptionPaths(modelSetPath);
            } catch (IOException e) {
                throw new RuntimeException("Error finding description files: " + modelSetPath, e);
            }

            // Read model set descriptions.
            Map<Path, List<String>> modelSetDescriptions = new HashMap<>();
            for (Path descriptionPath: descriptionPaths) {
                try {
                    modelSetDescriptions.put(descriptionPath, Files.readAllLines(descriptionPath));
                } catch (IOException e) {
                    throw new RuntimeException("Error reading description file: " + descriptionPath, e);
                }
            }

            // Add model set.
            ModelSet modelSet = modelSetBuilder.createModelSet(modelSetDescriptions);
            modelSets.add(modelSet);
        }

        // Return the loaded model sets.
        return modelSets;
    }

    /**
     * Collect paths to CIF files in given path.
     * 
     * @param basePath {@link Path} to directory containing CIF files for a model set.
     * @return Paths of the CIF files.
     * @throws IOException In case of an I/O error.
     */
    static List<Path> collectCIFPaths(Path basePath) throws IOException {
        List<Path> cifPaths;
        PathMatcher matcher = basePath.getFileSystem().getPathMatcher("glob:**.cif");
        try (Stream<Path> baseStream = Files.find(basePath, Integer.MAX_VALUE,
                (p, bfa) -> bfa.isRegularFile() && matcher.matches(p)))
        {
            cifPaths = baseStream.collect(Collectors.toList());
        }
        return cifPaths;
    }

    /**
     * Collect paths to the files describing a model set in given path.
     * 
     * @param basePath {@link Path} to directory containing description file(s) for a model set.
     * @return Paths of the description files.
     * @throws IOException In case of an I/O error.
     */
    static List<Path> collectDescriptionPaths(Path basePath) throws IOException {
        List<Path> descriptionPaths;
        PathMatcher matcher = basePath.getFileSystem().getPathMatcher("glob:**.txt");
        try (Stream<Path> baseStream = Files.find(basePath, Integer.MAX_VALUE,
                (p, bfa) -> bfa.isRegularFile() && matcher.matches(p)))
        {
            descriptionPaths = baseStream.collect(Collectors.toList());
        }

        return descriptionPaths;
    }
}
