/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cmi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.lsat.common.emf.common.util.URIHelper;
import org.eclipse.lsat.common.emf.ecore.resource.Persistor;
import org.eclipse.lsat.common.emf.ecore.resource.PersistorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import nl.esi.pps.tmsc.FullScopeTMSC;
import nl.esi.pps.tmsc.xtext.TmscXtextStandaloneSetup;
import nl.esi.pps.tmsc.xtext.generator.TmscXtextToTmscTransformation;
import nl.esi.pps.tmsc.xtext.tmscXtext.TmscXtextModel;
import nl.tno.mids.cmi.postprocessing.PostProcessingFilterMode;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationOptions;
import nl.tno.mids.cmi.postprocessing.operations.ExcludeInternalTransitionsOptions;
import nl.tno.mids.cmi.utils.TmscMetrics;
import nl.tno.mids.common.unittest.FileCompare;

class ComponentExtractionTest {
    Path basePath = Paths.get("testData/ComponentExtraction/").toAbsolutePath();

    @BeforeAll
    static void beforeAll() {
        TmscXtextStandaloneSetup.doSetup();
    }

    @Test
    void testBasicTmsc() throws IOException {
        Path baseTestPath = basePath.resolve("Base");
        ComponentExtractionOptions options = new ComponentExtractionOptions();
        performTest(baseTestPath, options);
    }

    @Test
    @DisabledOnOs(OS.LINUX)
    void testProtocolTmsc() throws IOException {
        Path baseTestPath = basePath.resolve("Protocol");
        ComponentExtractionOptions options = new ComponentExtractionOptions();
        options.getOutput().setOutputMode(OutputMode.PROTOCOL);
        options.getOutput().setProtocolName1("Client");
        options.getOutput().setProtocolName2("Server");
        performTest(baseTestPath, options);
    }

    @Test
    @DisabledOnOs(OS.LINUX)
    void testProtocolScopeTmsc() throws IOException {
        Path baseTestPath = basePath.resolve("ProtocolScope");
        ComponentExtractionOptions options = new ComponentExtractionOptions();
        options.getOutput().setOutputMode(OutputMode.PROTOCOL);
        options.getOutput().setProtocolName1("Client");
        options.getOutput().setProtocolName2("Server");
        options.getOutput().getScope().add("Connector");
        performTest(baseTestPath, options);
    }

    @Test
    void testSaveYedTmsc() throws IOException {
        Path baseTestPath = basePath.resolve("SaveYed");
        ComponentExtractionOptions options = new ComponentExtractionOptions();
        options.getOutput().setSaveYed(true);
        performTest(baseTestPath, options);
    }

    @Test
    void testModelPerComponentTmsc() throws IOException {
        Path baseTestPath = basePath.resolve("ModelPerComponent");
        ComponentExtractionOptions options = new ComponentExtractionOptions();
        options.getExtraction().setModelPerComponent(false);
        performTest(baseTestPath, options);
    }

    @Test
    void testNoSynchronizeDependentTransitionsTmsc() throws IOException {
        Path baseTestPath = basePath.resolve("NoSyncDepTrans");
        ComponentExtractionOptions options = new ComponentExtractionOptions();
        options.getExtraction().setSynchronizeDependentTransitions(false);
        performTest(baseTestPath, options);
    }

    @Test
    void testComponentExclusionTmsc() throws IOException {
        Path baseTestPath = basePath.resolve("ComponentExclusion");
        ComponentExtractionOptions options = new ComponentExtractionOptions();
        options.getPostProcessing().setComponentsExclusionRegEx("Client.*");
        performTest(baseTestPath, options);
    }

    @Test
    void testComponentInclusionTmsc() throws IOException {
        Path baseTestPath = basePath.resolve("ComponentInclusion");
        ComponentExtractionOptions options = new ComponentExtractionOptions();
        options.getPostProcessing().setComponentsInclusionRegEx("Client.*");
        performTest(baseTestPath, options);
    }

    @Test
    void testPostProcessingTmsc() throws IOException {
        Path baseTestPath = basePath.resolve("PostProcessing");
        ComponentExtractionOptions options = new ComponentExtractionOptions();
        options.getPostProcessing().getOperations().add(new ExcludeInternalTransitionsOptions());
        performTest(baseTestPath, options);
    }

    @Test
    void testPostProcessingExclusionTmsc() throws IOException {
        Path baseTestPath = basePath.resolve("PostProcessingExclusion");
        ComponentExtractionOptions options = new ComponentExtractionOptions();
        PostProcessingOperationOptions ppo = new ExcludeInternalTransitionsOptions();
        ppo.filterMode = PostProcessingFilterMode.EXCLUSION;
        ppo.filterPattern = "Client.*";
        options.getPostProcessing().getOperations().add(ppo);
        performTest(baseTestPath, options);
    }

    @Test
    void testPostProcessingInclusionTmsc() throws IOException {
        Path baseTestPath = basePath.resolve("PostProcessingInclusion");
        ComponentExtractionOptions options = new ComponentExtractionOptions();
        PostProcessingOperationOptions ppo = new ExcludeInternalTransitionsOptions();
        ppo.filterMode = PostProcessingFilterMode.INCLUSION;
        ppo.filterPattern = "Client.*";
        options.getPostProcessing().getOperations().add(ppo);
        performTest(baseTestPath, options);
    }

    @Test
    void testNoUntracedHandleSynchronouslyTmsc() throws IOException {
        Path baseTestPath = basePath.resolve("NoUntracedSync");
        ComponentExtractionOptions options = new ComponentExtractionOptions();
        // Note that this option is not functional right now, so result is same as base.
        options.getPreProcessing().setUntracedHandleSynchronously(false);
        performTest(baseTestPath, options);
    }

    private void performTest(Path baseTestPath, ComponentExtractionOptions options) throws IOException {
        Path baseInputPath = baseTestPath.resolve("input");
        Path baseTmsctPath = baseInputPath.resolve("extraction.tmsct");
        Path baseTmscPath = baseInputPath.resolve("extraction.tmsc");
        Path actualOutputPath = baseTestPath.resolve("output_actual");
        Path expectedOutputPath = baseTestPath.resolve("output_expected");

        Persistor<EObject> persistor = new PersistorFactory().getPersistor();
        List<EObject> fileContents = persistor.loadAll(URI.createFileURI(baseTmsctPath.toString()));

        TmscXtextModel tmsctModel = (TmscXtextModel)fileContents.get(0);
        FullScopeTMSC tmsc = new TmscXtextToTmscTransformation().transform(tmsctModel);

        ArrayList<EObject> tmscContents = new ArrayList<>(2);
        tmscContents.add(tmsc);
        tmscContents.addAll(tmsc.getArchitectures());

        persistor.save(URIHelper.asURI(baseTmscPath.toFile()), Collections.EMPTY_MAP, tmscContents);

        options.getInput().setPath(baseTmscPath);
        options.getOutput().setPath(actualOutputPath);

        new ComponentExtractionStubbed(baseTestPath).extract(baseTmsctPath, options,
                new NullProgressMonitor());

        try (Stream<Path> baseStream = Files.find(actualOutputPath, Integer.MAX_VALUE,
                (p, bfa) -> String.valueOf(p).endsWith(".graphml")))
        {
            baseStream.forEach(yedPath -> {
                List<String> yedLines;
                try {
                    yedLines = Files.lines(yedPath).collect(Collectors.toList());
                    List<String> newYedLines = new ArrayList<>();
                    for (String yedLine: yedLines) {
                        Pattern heightPattern = Pattern.compile("height=\"\\d+(\\.\\d+)\"");

                        Matcher heightMatcher = heightPattern.matcher(yedLine);
                        String newYedLine = heightMatcher.replaceAll("height=\"0\"");

                        Pattern widthPattern = Pattern.compile("width=\"\\d+(\\.\\d+)\"");

                        Matcher widthMatcher = widthPattern.matcher(newYedLine);
                        newYedLine = widthMatcher.replaceAll("width=\"0\"");

                        newYedLines.add(newYedLine);
                    }

                    Files.write(yedPath, newYedLines, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to update " + yedPath.toString(), e);
                }
            });
        }

        FileCompare.checkDirectoriesEqual(expectedOutputPath, actualOutputPath, p -> true);
    }

    class ComponentExtractionStubbed extends ComponentExtraction {
        private final Path baseTestPath;

        /**
         * @param baseTestPath Base path of test.
         */
        public ComponentExtractionStubbed(Path baseTestPath) {
            this.baseTestPath = baseTestPath;
        }

        @Override
        protected void saveReport(ComponentExtractionOptions options, TmscMetrics tmscMetrics, List<String> warnings,
                long runtimeMs)
        {
            // To ensure deterministic test output, assume runtime is 0.
            super.saveReport(options, tmscMetrics, warnings, 0);
        }

        @Override
        protected Path saveOptions(ComponentExtractionOptions options) {
            Path targetFolder = createOutputFolder(options);

            // To ensure portable test output, make paths relative before saving.
            ComponentExtractionOptions saveOptions = (ComponentExtractionOptions)options.clone();
            saveOptions.getInput().setPath(baseTestPath.relativize(options.getInput().getPath()));
            saveOptions.getOutput().setPath(baseTestPath.relativize(options.getOutput().getPath()));
            return saveOptions.writeOptionsFile(targetFolder);
        }
    }
}
