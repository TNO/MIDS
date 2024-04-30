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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.escet.common.java.Pair;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;

import nl.tno.mids.cif.extensions.mrr.cif.MrrToCifMode;
import nl.tno.mids.cmi.postprocessing.PostProcessingFilterMode;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationOptions;
import nl.tno.mids.cmi.postprocessing.operations.AddAsyncPatternConstraintsOptions;
import nl.tno.mids.cmi.postprocessing.operations.ExcludeInternalTransitionsOptions;
import nl.tno.mids.cmi.postprocessing.operations.FilterClientServerInteractionsOptions;
import nl.tno.mids.cmi.postprocessing.operations.HideActionsOptions;
import nl.tno.mids.cmi.postprocessing.operations.InjectDomainKnowledgeOperator;
import nl.tno.mids.cmi.postprocessing.operations.InjectDomainKnowledgeOptions;
import nl.tno.mids.cmi.postprocessing.operations.MergeComponentsOptions;
import nl.tno.mids.cmi.postprocessing.operations.MergeInterfaceClientsServersOptions;
import nl.tno.mids.cmi.postprocessing.operations.ModifyRepetitionsOptions;
import nl.tno.mids.cmi.postprocessing.operations.PrefixCloseOptions;
import nl.tno.mids.cmi.postprocessing.operations.RenameComponentOptions;
import nl.tno.mids.cmi.postprocessing.operations.RenameFunctionsOptions;

class ComponentExtractionOptionsTest {
    @Test
    public void performHelpTest() throws IOException {
        String[] args = {"-h"};

        assertNull(ComponentExtractionOptions.parse(args));
    }

    @Nested
    class Input {
        @Test
        public void performNoInputValidateTest() throws IOException {
            String[] args = {};

            assertThrows(RuntimeException.class, () -> ComponentExtractionOptions.parse(args));
        }

        @Test
        public void performInputNotExistsValidateTest() throws IOException {
            Path inputPath = Paths.get("testData/testOptions/notExist.tmscz");
            String[] args = {"-input", inputPath.toString()};

            assertThrows(RuntimeException.class, () -> ComponentExtractionOptions.parse(args));
        }

        @Test
        public void performInputNotFileValidateTest() throws IOException {
            Path inputPath = Paths.get("testData/testOptions/");
            String[] args = {"-input", inputPath.toString()};

            assertThrows(RuntimeException.class, () -> ComponentExtractionOptions.parse(args));
        }
    }

    @Nested
    class Output {
        @Test
        public void performNoOutputParseTest() throws IOException {
            Path inputPath = Paths.get("testData/testOptions/dummy.tmscz");
            Path outputPath = inputPath.resolveSibling("cmi");
            String[] args = {"-input", inputPath.toString()};

            ComponentExtractionOptions componentExtractionOptions = ComponentExtractionOptions.parse(args);

            assertEquals(inputPath.toAbsolutePath(), componentExtractionOptions.getInput().getPath());
            assertEquals(outputPath.toAbsolutePath(), componentExtractionOptions.getOutput().getPath());
            assertEquals(OutputMode.COMPONENTS, componentExtractionOptions.getOutput().getOutputMode());
            assertNull(componentExtractionOptions.getOutput().getProtocolName1());
            assertNull(componentExtractionOptions.getOutput().getProtocolName2());
            assertFalse(componentExtractionOptions.getOutput().isSaveYed());
        }

        @Test
        public void performOutputFileValidateTest() throws IOException {
            Path inputPath = Paths.get("testData/testOptions/dummy.tmscz");
            Path outputPath = Paths.get("testData/testOptions/dummy.tmscz");
            String[] args = {"-input", inputPath.toString(), "-output", outputPath.toString()};

            assertThrows(RuntimeException.class, () -> ComponentExtractionOptions.parse(args));
        }

        @Test
        public void performOutputParseTest() throws IOException {
            Path inputPath = Paths.get("testData/testOptions/dummy.tmscz");
            Path outputPath = Paths.get("path/to/output");
            String[] args = {"-input", inputPath.toString(), "-output", outputPath.toString()};

            ComponentExtractionOptions componentExtractionOptions = ComponentExtractionOptions.parse(args);

            assertEquals(inputPath.toAbsolutePath(), componentExtractionOptions.getInput().getPath());
            assertEquals(outputPath.toAbsolutePath(), componentExtractionOptions.getOutput().getPath());
            assertEquals(OutputMode.COMPONENTS, componentExtractionOptions.getOutput().getOutputMode());
            assertNull(componentExtractionOptions.getOutput().getProtocolName1());
            assertNull(componentExtractionOptions.getOutput().getProtocolName2());
            assertFalse(componentExtractionOptions.getOutput().isSaveYed());
        }

        @Test
        public void performProtocolParseTest() throws IOException {
            Path inputPath = Paths.get("testData/testOptions/dummy.tmscz");
            Path outputPath = Paths.get("path/to/output");
            String[] args = {"-input", inputPath.toString(), "-output", outputPath.toString(), "-p", "CompA,CompB"};

            ComponentExtractionOptions componentExtractionOptions = ComponentExtractionOptions.parse(args);

            assertEquals(inputPath.toAbsolutePath(), componentExtractionOptions.getInput().getPath());
            assertEquals(outputPath.toAbsolutePath(), componentExtractionOptions.getOutput().getPath());
            assertEquals(OutputMode.PROTOCOL, componentExtractionOptions.getOutput().getOutputMode());
            assertEquals("CompA", componentExtractionOptions.getOutput().getProtocolName1());
            assertEquals("CompB", componentExtractionOptions.getOutput().getProtocolName2());
            assertFalse(componentExtractionOptions.getOutput().isSaveYed());
        }

        @Test
        public void performProtocolParseScopeTest() throws IOException {
            Path inputPath = Paths.get("testData/testOptions/dummy.tmscz");
            Path outputPath = Paths.get("path/to/output");
            String[] args = {"-input", inputPath.toString(), "-output", outputPath.toString(), "-p", "CompA,CompB",
                    "-ps", "CompC,CompD"};

            ComponentExtractionOptions componentExtractionOptions = ComponentExtractionOptions.parse(args);

            assertEquals(inputPath.toAbsolutePath(), componentExtractionOptions.getInput().getPath());
            assertEquals(outputPath.toAbsolutePath(), componentExtractionOptions.getOutput().getPath());
            assertEquals(OutputMode.PROTOCOL, componentExtractionOptions.getOutput().getOutputMode());
            assertEquals("CompA", componentExtractionOptions.getOutput().getProtocolName1());
            assertEquals("CompB", componentExtractionOptions.getOutput().getProtocolName2());
            assertLinesMatch(Lists.newArrayList("CompC", "CompD"), componentExtractionOptions.getOutput().getScope());
            assertFalse(componentExtractionOptions.getOutput().isSaveYed());
        }

        @Test
        public void performSaveYedParseTest() throws IOException {
            Path inputPath = Paths.get("testData/testOptions/dummy.tmscz");
            Path outputPath = Paths.get("path/to/output");
            String[] args = {"-input", inputPath.toString(), "-output", outputPath.toString(), "-yed"};

            ComponentExtractionOptions componentExtractionOptions = ComponentExtractionOptions.parse(args);

            assertEquals(inputPath.toAbsolutePath(), componentExtractionOptions.getInput().getPath());
            assertEquals(outputPath.toAbsolutePath(), componentExtractionOptions.getOutput().getPath());
            assertEquals(OutputMode.COMPONENTS, componentExtractionOptions.getOutput().getOutputMode());
            assertNull(componentExtractionOptions.getOutput().getProtocolName1());
            assertNull(componentExtractionOptions.getOutput().getProtocolName2());
            assertTrue(componentExtractionOptions.getOutput().isSaveYed());
        }
    }

    @Nested
    class Extraction {
        @Test
        public void performSingleModelParseTest() throws IOException {
            Path inputPath = Paths.get("testData/testOptions/dummy.tmscz");
            String[] args = {"-input", inputPath.toString(), "-single-model"};

            ComponentExtractionOptions componentExtractionOptions = ComponentExtractionOptions.parse(args);

            assertEquals(inputPath.toAbsolutePath(), componentExtractionOptions.getInput().getPath());
            assertFalse(componentExtractionOptions.getExtraction().isModelPerComponent());
            assertTrue(componentExtractionOptions.getExtraction().isSynchronizeDependentTransitions());
        }

        @Test
        public void performNoSyncDependentParseTest() throws IOException {
            Path inputPath = Paths.get("testData/testOptions/dummy.tmscz");
            String[] args = {"-input", inputPath.toString(), "-no-sync-dependent"};

            ComponentExtractionOptions componentExtractionOptions = ComponentExtractionOptions.parse(args);

            assertEquals(inputPath.toAbsolutePath(), componentExtractionOptions.getInput().getPath());
            assertTrue(componentExtractionOptions.getExtraction().isModelPerComponent());
            assertFalse(componentExtractionOptions.getExtraction().isSynchronizeDependentTransitions());
        }
    }

    @Nested
    class PostProcessing {
        @Test
        public void performComponentExclusionParseTest() throws IOException {
            Path inputPath = Paths.get("testData/testOptions/dummy.tmscz");
            String[] args = {"-input", inputPath.toString(), "-component-exclusion", "ABC.*"};

            ComponentExtractionOptions componentExtractionOptions = ComponentExtractionOptions.parse(args);

            assertEquals(inputPath.toAbsolutePath(), componentExtractionOptions.getInput().getPath());
            assertEquals("ABC.*", componentExtractionOptions.getPostProcessing().getComponentsExclusionRegEx());
            assertEquals("", componentExtractionOptions.getPostProcessing().getComponentsInclusionRegEx());
        }

        @Test
        public void performComponentInclusionParseTest() throws IOException {
            Path inputPath = Paths.get("testData/testOptions/dummy.tmscz");
            String[] args = {"-input", inputPath.toString(), "-component-inclusion", "ABC.*"};

            ComponentExtractionOptions componentExtractionOptions = ComponentExtractionOptions.parse(args);

            assertEquals(inputPath.toAbsolutePath(), componentExtractionOptions.getInput().getPath());
            assertEquals("", componentExtractionOptions.getPostProcessing().getComponentsExclusionRegEx());
            assertEquals("ABC.*", componentExtractionOptions.getPostProcessing().getComponentsInclusionRegEx());
        }

        @Test
        public void performRenameComponentParseTest() throws IOException {
            Path inputPath = Paths.get("testData/testOptions/dummy.tmscz");
            String[] args = {"-input", inputPath.toString(), "-post-processing", "RenameComponent(CompA,CompB)"};

            ComponentExtractionOptions componentExtractionOptions = ComponentExtractionOptions.parse(args);

            assertEquals(inputPath.toAbsolutePath(), componentExtractionOptions.getInput().getPath());
            assertEquals("", componentExtractionOptions.getPostProcessing().getComponentsExclusionRegEx());
            assertEquals("", componentExtractionOptions.getPostProcessing().getComponentsInclusionRegEx());
            assertEquals(1, componentExtractionOptions.getPostProcessing().getOperations().size());
            PostProcessingOperationOptions options = componentExtractionOptions.getPostProcessing().getOperations()
                    .get(0);
            assertTrue(options instanceof RenameComponentOptions);
            RenameComponentOptions renameComponentOptions = (RenameComponentOptions)options;
            assertEquals("CompA", renameComponentOptions.getOldComponentName());
            assertEquals("CompB", renameComponentOptions.getNewComponentName());
        }

        @Test
        public void performRenameFunctionsParseTest() throws IOException {
            Path inputPath = Paths.get("testData/testOptions/dummy.tmscz");
            String[] args = {"-input", inputPath.toString(), "-post-processing",
                    "RenameFunctions(CompA:FunctionA->CompA:FunctionB,CompB:FunctionC->CompC:FunctionC)"};

            ComponentExtractionOptions componentExtractionOptions = ComponentExtractionOptions.parse(args);

            assertEquals(inputPath.toAbsolutePath(), componentExtractionOptions.getInput().getPath());
            assertEquals("", componentExtractionOptions.getPostProcessing().getComponentsExclusionRegEx());
            assertEquals("", componentExtractionOptions.getPostProcessing().getComponentsInclusionRegEx());
            assertEquals(1, componentExtractionOptions.getPostProcessing().getOperations().size());
            PostProcessingOperationOptions options = componentExtractionOptions.getPostProcessing().getOperations()
                    .get(0);
            assertTrue(options instanceof RenameFunctionsOptions);
            RenameFunctionsOptions renameComponentOptions = (RenameFunctionsOptions)options;
            assertEquals(2, renameComponentOptions.getFunctionMapping().size());
            assertTrue(renameComponentOptions.getFunctionMapping().containsKey(Pair.pair("CompA", "FunctionA")));
            assertEquals(Pair.pair("CompA", "FunctionB"),
                    renameComponentOptions.getFunctionMapping().get(Pair.pair("CompA", "FunctionA")));
            assertTrue(renameComponentOptions.getFunctionMapping().containsKey(Pair.pair("CompB", "FunctionC")));
            assertEquals(Pair.pair("CompC", "FunctionC"),
                    renameComponentOptions.getFunctionMapping().get(Pair.pair("CompB", "FunctionC")));
        }

        @Test
        public void performAddAsyncPatternConstraintsParseTest() throws IOException {
            Path inputPath = Paths.get("testData/testOptions/dummy.tmscz");
            String[] args = {"-input", inputPath.toString(), "-post-processing", "AddAsyncPatternConstraints"};

            ComponentExtractionOptions componentExtractionOptions = ComponentExtractionOptions.parse(args);

            assertEquals(inputPath.toAbsolutePath(), componentExtractionOptions.getInput().getPath());
            assertEquals("", componentExtractionOptions.getPostProcessing().getComponentsExclusionRegEx());
            assertEquals("", componentExtractionOptions.getPostProcessing().getComponentsInclusionRegEx());
            assertEquals(1, componentExtractionOptions.getPostProcessing().getOperations().size());
            PostProcessingOperationOptions options = componentExtractionOptions.getPostProcessing().getOperations()
                    .get(0);
            assertTrue(options instanceof AddAsyncPatternConstraintsOptions);
        }

        @Test
        public void performExcludeInternalTransitionsParseTest() throws IOException {
            Path inputPath = Paths.get("testData/testOptions/dummy.tmscz");
            String[] args = {"-input", inputPath.toString(), "-post-processing", "ExcludeInternalTransitions"};

            ComponentExtractionOptions componentExtractionOptions = ComponentExtractionOptions.parse(args);

            assertEquals(inputPath.toAbsolutePath(), componentExtractionOptions.getInput().getPath());
            assertEquals("", componentExtractionOptions.getPostProcessing().getComponentsExclusionRegEx());
            assertEquals("", componentExtractionOptions.getPostProcessing().getComponentsInclusionRegEx());
            assertEquals(1, componentExtractionOptions.getPostProcessing().getOperations().size());
            PostProcessingOperationOptions options = componentExtractionOptions.getPostProcessing().getOperations()
                    .get(0);
            assertTrue(options instanceof ExcludeInternalTransitionsOptions);
        }

        @Test
        public void performFilterClientServerInteractionsParseTest() throws IOException {
            Path inputPath = Paths.get("testData/testOptions/dummy.tmscz");
            String[] args = {"-input", inputPath.toString(), "-post-processing",
                    "FilterClientServerInteractions(CompA,CompB)"};

            ComponentExtractionOptions componentExtractionOptions = ComponentExtractionOptions.parse(args);

            assertEquals(inputPath.toAbsolutePath(), componentExtractionOptions.getInput().getPath());
            assertEquals("", componentExtractionOptions.getPostProcessing().getComponentsExclusionRegEx());
            assertEquals("", componentExtractionOptions.getPostProcessing().getComponentsInclusionRegEx());
            assertEquals(1, componentExtractionOptions.getPostProcessing().getOperations().size());
            PostProcessingOperationOptions options = componentExtractionOptions.getPostProcessing().getOperations()
                    .get(0);
            assertTrue(options instanceof FilterClientServerInteractionsOptions);
            FilterClientServerInteractionsOptions filterClientServerOptions = (FilterClientServerInteractionsOptions)options;
            assertEquals("CompA", filterClientServerOptions.getComponentName1());
            assertEquals("CompB", filterClientServerOptions.getComponentName2());
        }

        @Test
        public void performHideActionsParseTest() throws IOException {
            Path inputPath = Paths.get("testData/testOptions/dummy.tmscz");
            String[] args = {"-input", inputPath.toString(), "-post-processing", "HideActions(.*test.*)"};

            ComponentExtractionOptions componentExtractionOptions = ComponentExtractionOptions.parse(args);

            assertEquals(inputPath.toAbsolutePath(), componentExtractionOptions.getInput().getPath());
            assertEquals("", componentExtractionOptions.getPostProcessing().getComponentsExclusionRegEx());
            assertEquals("", componentExtractionOptions.getPostProcessing().getComponentsInclusionRegEx());
            assertEquals(1, componentExtractionOptions.getPostProcessing().getOperations().size());
            PostProcessingOperationOptions options = componentExtractionOptions.getPostProcessing().getOperations()
                    .get(0);
            assertTrue(options instanceof HideActionsOptions);
            HideActionsOptions hideActionsOptions = (HideActionsOptions)options;
            assertEquals(".*test.*", hideActionsOptions.getPattern());
        }

        @Test
        public void performInjectDomainKnowledgeParseTest() throws IOException {
            Path inputPath = Paths.get("testData/testOptions/dummy.tmscz");
            String[] args = {"-input", inputPath.toString(), "-post-processing",
                    "InjectDomainKnowledge(union,test.cif)"};

            ComponentExtractionOptions componentExtractionOptions = ComponentExtractionOptions.parse(args);

            assertEquals(inputPath.toAbsolutePath(), componentExtractionOptions.getInput().getPath());
            assertEquals("", componentExtractionOptions.getPostProcessing().getComponentsExclusionRegEx());
            assertEquals("", componentExtractionOptions.getPostProcessing().getComponentsInclusionRegEx());
            assertEquals(1, componentExtractionOptions.getPostProcessing().getOperations().size());
            PostProcessingOperationOptions options = componentExtractionOptions.getPostProcessing().getOperations()
                    .get(0);
            assertTrue(options instanceof InjectDomainKnowledgeOptions);
            InjectDomainKnowledgeOptions injectDomainKnowledgeOptions = (InjectDomainKnowledgeOptions)options;
            assertEquals(InjectDomainKnowledgeOperator.UNION, injectDomainKnowledgeOptions.operator);
            assertEquals("test.cif", injectDomainKnowledgeOptions.modelPath);
        }

        @Test
        public void performMergeInterfaceClientsServersParseTest() throws IOException {
            Path inputPath = Paths.get("testData/testOptions/dummy.tmscz");
            String[] args = {"-input", inputPath.toString(), "-post-processing",
                    "MergeInterfaceClientsServers(Test,true,false)"};

            ComponentExtractionOptions componentExtractionOptions = ComponentExtractionOptions.parse(args);

            assertEquals(inputPath.toAbsolutePath(), componentExtractionOptions.getInput().getPath());
            assertEquals("", componentExtractionOptions.getPostProcessing().getComponentsExclusionRegEx());
            assertEquals("", componentExtractionOptions.getPostProcessing().getComponentsInclusionRegEx());
            assertEquals(1, componentExtractionOptions.getPostProcessing().getOperations().size());
            PostProcessingOperationOptions options = componentExtractionOptions.getPostProcessing().getOperations()
                    .get(0);
            assertTrue(options instanceof MergeInterfaceClientsServersOptions);
            MergeInterfaceClientsServersOptions mergeInterfaceClientsServersOptions = (MergeInterfaceClientsServersOptions)options;
            assertEquals("Test", mergeInterfaceClientsServersOptions.mergeInterface);
            assertTrue(mergeInterfaceClientsServersOptions.mergeClients);
            assertFalse(mergeInterfaceClientsServersOptions.mergeServers);
        }

        @Test
        public void performMergeComponentsParseTest() throws IOException {
            Path inputPath = Paths.get("testData/testOptions/dummy.tmscz");
            String[] args = {"-input", inputPath.toString(), "-post-processing",
                    "MergeComponents((?<name>\\w+TE)\\d+)"};

            ComponentExtractionOptions componentExtractionOptions = ComponentExtractionOptions.parse(args);

            assertEquals(inputPath.toAbsolutePath(), componentExtractionOptions.getInput().getPath());
            assertEquals("", componentExtractionOptions.getPostProcessing().getComponentsExclusionRegEx());
            assertEquals("", componentExtractionOptions.getPostProcessing().getComponentsInclusionRegEx());
            assertEquals(1, componentExtractionOptions.getPostProcessing().getOperations().size());
            PostProcessingOperationOptions options = componentExtractionOptions.getPostProcessing().getOperations()
                    .get(0);
            assertTrue(options instanceof MergeComponentsOptions);
            MergeComponentsOptions mergeComponentsOptions = (MergeComponentsOptions)options;
            assertEquals("(?<name>\\w+TE)\\d+", mergeComponentsOptions.pattern);
        }

        @Test
        public void performModifyRepetitionsParseTest() throws IOException {
            Path inputPath = Paths.get("testData/testOptions/dummy.tmscz");
            String[] args = {"-input", inputPath.toString(), "-post-processing",
                    "ModifyRepetitions(data,5,10,false,6)"};

            ComponentExtractionOptions componentExtractionOptions = ComponentExtractionOptions.parse(args);

            assertEquals(inputPath.toAbsolutePath(), componentExtractionOptions.getInput().getPath());
            assertEquals("", componentExtractionOptions.getPostProcessing().getComponentsExclusionRegEx());
            assertEquals("", componentExtractionOptions.getPostProcessing().getComponentsInclusionRegEx());
            assertEquals(1, componentExtractionOptions.getPostProcessing().getOperations().size());
            PostProcessingOperationOptions options = componentExtractionOptions.getPostProcessing().getOperations()
                    .get(0);
            assertTrue(options instanceof ModifyRepetitionsOptions);
            ModifyRepetitionsOptions modifyRepetitionsOptions = (ModifyRepetitionsOptions)options;
            assertEquals(MrrToCifMode.DATA, modifyRepetitionsOptions.mode);
            assertEquals(5, modifyRepetitionsOptions.lowerThreshold);
            assertEquals(10, modifyRepetitionsOptions.upperThreshold);
            assertFalse(modifyRepetitionsOptions.makeInfinite);
            assertEquals(6, modifyRepetitionsOptions.maxRepeats);
        }

        @Test
        public void performPrefixCloseParseTest() throws IOException {
            Path inputPath = Paths.get("testData/testOptions/dummy.tmscz");
            String[] args = {"-input", inputPath.toString(), "-post-processing", "PrefixClose"};

            ComponentExtractionOptions componentExtractionOptions = ComponentExtractionOptions.parse(args);

            assertEquals(inputPath.toAbsolutePath(), componentExtractionOptions.getInput().getPath());
            assertEquals("", componentExtractionOptions.getPostProcessing().getComponentsExclusionRegEx());
            assertEquals("", componentExtractionOptions.getPostProcessing().getComponentsInclusionRegEx());
            assertEquals(1, componentExtractionOptions.getPostProcessing().getOperations().size());
            PostProcessingOperationOptions options = componentExtractionOptions.getPostProcessing().getOperations()
                    .get(0);
            assertTrue(options instanceof PrefixCloseOptions);
        }

        @Test
        public void performPPInclusionFilterParseTest() throws IOException {
            Path inputPath = Paths.get("testData/testOptions/dummy.tmscz");
            String[] args = {"-input", inputPath.toString(), "-post-processing", "<inclusion,test.*>PrefixClose"};

            ComponentExtractionOptions componentExtractionOptions = ComponentExtractionOptions.parse(args);

            assertEquals(inputPath.toAbsolutePath(), componentExtractionOptions.getInput().getPath());
            assertEquals("", componentExtractionOptions.getPostProcessing().getComponentsExclusionRegEx());
            assertEquals("", componentExtractionOptions.getPostProcessing().getComponentsInclusionRegEx());
            assertEquals(1, componentExtractionOptions.getPostProcessing().getOperations().size());
            PostProcessingOperationOptions options = componentExtractionOptions.getPostProcessing().getOperations()
                    .get(0);
            assertEquals(PostProcessingFilterMode.INCLUSION, options.filterMode);
            assertEquals("test.*", options.filterPattern);
        }

        @Test
        public void performPPExclusionFilterParseTest() throws IOException {
            Path inputPath = Paths.get("testData/testOptions/dummy.tmscz");
            String[] args = {"-input", inputPath.toString(), "-post-processing", "<exclusion,test.*>PrefixClose"};

            ComponentExtractionOptions componentExtractionOptions = ComponentExtractionOptions.parse(args);

            assertEquals(inputPath.toAbsolutePath(), componentExtractionOptions.getInput().getPath());
            assertEquals("", componentExtractionOptions.getPostProcessing().getComponentsExclusionRegEx());
            assertEquals("", componentExtractionOptions.getPostProcessing().getComponentsInclusionRegEx());
            assertEquals(1, componentExtractionOptions.getPostProcessing().getOperations().size());
            PostProcessingOperationOptions options = componentExtractionOptions.getPostProcessing().getOperations()
                    .get(0);
            assertEquals(PostProcessingFilterMode.EXCLUSION, options.filterMode);
            assertEquals("test.*", options.filterPattern);
        }
    }

    @Nested
    class PreProcessing {
        @Test
        public void performNoUntracedSynchronousParseTest() throws IOException {
            Path inputPath = Paths.get("testData/testOptions/dummy.tmscz");
            String[] args = {"-input", inputPath.toString(), "-no-untraced-synchronous"};

            ComponentExtractionOptions componentExtractionOptions = ComponentExtractionOptions.parse(args);

            assertEquals(inputPath.toAbsolutePath(), componentExtractionOptions.getInput().getPath());
            assertFalse(componentExtractionOptions.getPreProcessing().isUntracedHandleSynchronously());
        }
    }

    @Nested
    class ConfigFile {
        @Test
        public void performDefaultConfigTest() throws IOException {
            Path configPath = Paths.get("testData/testOptions/defaultOptions.txt");
            String[] args = {"-options-file", configPath.toString()};

            ComponentExtractionOptions componentExtractionOptions = ComponentExtractionOptions.parse(args);

            assertEquals(Paths.get("testData/testOptions/dummy.tmscz").toAbsolutePath(),
                    componentExtractionOptions.getInput().getPath());
            assertEquals(Paths.get("path/to/cmi").toAbsolutePath(), componentExtractionOptions.getOutput().getPath());
            assertEquals(OutputMode.COMPONENTS, componentExtractionOptions.getOutput().getOutputMode());
            assertNull(componentExtractionOptions.getOutput().getProtocolName1());
            assertNull(componentExtractionOptions.getOutput().getProtocolName2());
            assertFalse(componentExtractionOptions.getOutput().isSaveYed());
        }

        @Test
        public void performNonDefaultConfigTest() throws IOException {
            Path configPath = Paths.get("testData/testOptions/nonDefaultOptions.txt");
            String[] args = {"-options-file", configPath.toString()};

            ComponentExtractionOptions componentExtractionOptions = ComponentExtractionOptions.parse(args);

            assertEquals(Paths.get("testData/testOptions/dummy.tmscz").toAbsolutePath(),
                    componentExtractionOptions.getInput().getPath());
            assertEquals(Paths.get("path/to/output").toAbsolutePath(),
                    componentExtractionOptions.getOutput().getPath());
            assertEquals(OutputMode.COMPONENTS, componentExtractionOptions.getOutput().getOutputMode());
            assertEquals("CompA", componentExtractionOptions.getOutput().getProtocolName1());
            assertEquals("CompB", componentExtractionOptions.getOutput().getProtocolName2());
            assertLinesMatch(Lists.newArrayList("CompC", "CompD"), componentExtractionOptions.getOutput().getScope());
            assertTrue(componentExtractionOptions.getOutput().isSaveYed());
            assertFalse(componentExtractionOptions.getPreProcessing().isUntracedHandleSynchronously());
            assertFalse(componentExtractionOptions.getExtraction().isModelPerComponent());
            assertFalse(componentExtractionOptions.getExtraction().isSynchronizeDependentTransitions());
            assertEquals("TEST.*", componentExtractionOptions.getPostProcessing().getComponentsExclusionRegEx());
            assertEquals("TESTA.*", componentExtractionOptions.getPostProcessing().getComponentsInclusionRegEx());
            assertEquals(1, componentExtractionOptions.getPostProcessing().getOperations().size());
            PostProcessingOperationOptions options = componentExtractionOptions.getPostProcessing().getOperations()
                    .get(0);
            assertTrue(options instanceof PrefixCloseOptions);
            assertEquals(PostProcessingFilterMode.INCLUSION, options.filterMode);
            assertEquals("TESTB.*", options.filterPattern);
        }
    }
}
