/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cmi.postprocessing.operations;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.escet.cif.io.emf.CifResourceFactory;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.common.app.framework.AppEnv;
import org.junit.jupiter.api.BeforeAll;

import nl.tno.mids.cif.extensions.CIFOperations;
import nl.tno.mids.cif.extensions.CifExtensions;
import nl.tno.mids.cif.extensions.FileExtensions;
import nl.tno.mids.cmi.postprocessing.PostProcessingModel;
import nl.tno.mids.cmi.postprocessing.PostProcessingModelCifSpec;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperation;
import nl.tno.mids.cmi.postprocessing.PostProcessingStatusExtensions;
import nl.tno.mids.cmi.postprocessing.status.PostProcessingStatus;
import nl.tno.mids.common.unittest.FileCompare;

/**
 * Base class implementing general functionality for testing post-processing operations.
 *
 * @param <PPO> Post-processing operation to test.
 */
public abstract class BasePostProcessingOperationTest<PPO extends PostProcessingOperation<?>> {
    private static String CIF_FILE_EXTENSION = "cif";

    /**
     * Name of test class.
     */
    protected String testClass = this.getClass().getSimpleName();

    /**
     * Root path of test data for this test class.
     */
    protected Path testClassRootPath;

    /**
     * {@link Path} to input for current test.
     */
    protected Path testInputPath;

    /**
     * {@link Path} to actual output for current test.
     */
    protected Path testActualOutputPath;

    /**
     * {@link Path} to expected output for current test.
     */
    protected Path testExpectedOutputPath;

    /**
     * {@link Path} to resolve additional data for current test.
     */
    protected Path relativeResolvePath = null;

    /**
     * Monitor to handle progress reports, not checked for tests.
     */
    protected IProgressMonitor monitor = new NullProgressMonitor();

    /**
     * Map containing test models to process.
     */
    protected Map<String, PostProcessingModel> postProcessingModels;

    @BeforeAll
    protected static void beforeAll() {
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(CIF_FILE_EXTENSION, new CifResourceFactory());
    }

    protected PostProcessingModel createModel(String cifSpecString) {
        Specification cifSpec = CIFOperations.read(cifSpecString);
        return new PostProcessingModelCifSpec(cifSpec, cifSpec.getName(),
                PostProcessingStatusExtensions.getPostProcessingStatus(cifSpec));
    }

    protected PostProcessingModel createModel(String cifSpecString, String cifSpecName) {
        Specification cifSpec = CIFOperations.read(cifSpecString);
        return new PostProcessingModelCifSpec(cifSpec, cifSpecName,
                PostProcessingStatusExtensions.getPostProcessingStatus(cifSpec));
    }

    private PostProcessingModel createModel(Specification specification, String modelName) {
        return new PostProcessingModelCifSpec(specification, modelName,
                PostProcessingStatusExtensions.getPostProcessingStatus(specification));
    }

    protected Map<String, PostProcessingModel> getInputModels() {
        Map<String, PostProcessingModel> postProcessingModels = new TreeMap<>();

        AppEnv.registerSimple();
        try (DirectoryStream<Path> inputCifFolder = Files.newDirectoryStream(testInputPath)) {
            for (Path inputCifFile: inputCifFolder) {
                PostProcessingModel postProcessingModel = createModel(CIFOperations.loadCIFSpec(inputCifFile),
                        removeFileExtension(inputCifFile.getFileName()));
                postProcessingModels.put(postProcessingModel.name, postProcessingModel);
            }
        } catch (IOException e) {
            throw new RuntimeException("Exception while reading input CIF files for: " + testInputPath, e);
        } finally {
            AppEnv.unregisterThread();
        }
        return postProcessingModels;
    }

    private String removeFileExtension(Path fileNamePath) {
        String fileName = fileNamePath.toString();
        int fileNameLastIndexOfPeriod = fileName.lastIndexOf(".");
        return fileNameLastIndexOfPeriod < 0 ? fileName : fileName.substring(0, fileNameLastIndexOfPeriod);
    }

    protected void writeModels(Map<String, PostProcessingModel> postProcessingModels) {
        postProcessingModels.entrySet().stream().forEach(m -> {
            Specification cifSpec = m.getValue().getCifSpec();
            Path rootOutputPath = testActualOutputPath.resolve(m.getKey());
            Path cifFilePath = Paths.get(rootOutputPath.toString() + "." + CIF_FILE_EXTENSION);
            CifExtensions.normalizeOrder(cifSpec);
            try {
                Files.createDirectories(rootOutputPath.getParent());
                FileExtensions.saveCIF(cifSpec, cifFilePath);
            } catch (IOException e) {
                throw new RuntimeException("Exception while writing CIF file: " + cifFilePath, e);
            }
        });
    }

    protected void performTest(String testName) throws IOException {
        testClassRootPath = Paths.get("testData").resolve("PostProcessing").resolve(testClass);
        Path testRootPath = testClassRootPath.resolve(testName);
        testInputPath = testRootPath.resolve("input");
        testActualOutputPath = testRootPath.resolve("output_actual");
        testExpectedOutputPath = testRootPath.resolve("output_expected");

        postProcessingModels = getInputModels();

        PPO operation = getOperation();

        Map<String, PostProcessingStatus> initialStatuses = postProcessingModels.entrySet().stream()
                .map(Entry::getValue)
                .collect(Collectors.toMap(ppm -> ppm.name, ppm -> operation.getPreconditionSubset().apply(ppm.status)));

        operation.applyOperation(postProcessingModels, postProcessingModels.keySet(), relativeResolvePath, monitor);

        Map<String, PostProcessingStatus> resultStatuses = initialStatuses.entrySet().stream()
                .collect(Collectors.toMap(Entry::getKey, e -> operation.getResultSubset().apply(e.getValue())));

        postProcessingModels.entrySet().stream().forEach(e -> {
            PostProcessingStatus expectedResultStatus = resultStatuses.get(e.getKey());
            if (expectedResultStatus != null) {
                assertEquals(expectedResultStatus.dataIsPresent(), e.getValue().status.dataIsPresent(),
                        "Result status data not met");
                assertEquals(expectedResultStatus.tauIsPresent(), e.getValue().status.tauIsPresent(),
                        "Result status tau not met");
            }
        });

        writeModels(postProcessingModels);

        FileCompare.checkDirectoriesEqual(testExpectedOutputPath, testActualOutputPath, p -> true);
    }

    protected abstract PPO getOperation();
}
