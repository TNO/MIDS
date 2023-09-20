/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cmi.cmi2yed;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.common.app.framework.AppEnv;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import nl.tno.mids.cif.extensions.CIFOperations;
import nl.tno.mids.common.unittest.FileCompare;

@DisabledOnOs(OS.LINUX)
class CmiToYedTransformerTest {
    @ParameterizedTest
    @ValueSource(strings =
    {"Base", // Test with basic client-server communication.
            "ClientFcnFcncb", // Test with client model with asynchronous call and callback, connected by constraint.
            "ClientReqWait", // Test with client model with request and wait, connected by constraint.
            "Mrr", // Test with model with MRR data.
            "SameState", // Test with two edges, connected by constraint and with the same source state.
            "ServerAsyncArslt", // Test with server model with asynchronous call and callback, connected by constraint.
            "WithExistingConstraint" // Test with constraint added to model with existing guard and update.
    })
    void baseTest(String testName) throws IOException {
        Path testRootPath = Paths.get("testData").resolve("CmiToYed").resolve(testName).toAbsolutePath();
        Path testInputPath = testRootPath.resolve("input").resolve(testName + ".cif");
        Path testOutputFolderPath = testRootPath.resolve("output_actual");
        Path testOutputPath = testOutputFolderPath.resolve(testName + ".graphml");
        Path testExpectedPath = testRootPath.resolve("output_expected").resolve(testName + ".graphml");

        Specification cifSpecification = getInputModel(testInputPath);

        Files.createDirectories(testOutputPath.getParent());
        clearOutput(testOutputFolderPath);
        CmiToYedTransformer.transform(cifSpecification, testOutputPath);

        FileCompare.checkDirectoriesEqual(testExpectedPath.getParent(), testOutputFolderPath,
                path -> path.endsWith("graphml"));
    }

    private static Specification getInputModel(Path testInputPath) {
        Specification cifSpecification = null;

        AppEnv.registerSimple();
        try {
            cifSpecification = CIFOperations.loadCIFSpec(testInputPath);
        } finally {
            AppEnv.unregisterThread();
        }
        return cifSpecification;
    }

    private static void clearOutput(Path outputPath) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(outputPath)) {
            for (Path p: Files.walk(outputPath).sorted((a, b) -> b.compareTo(a)). // reverse; files before dirs
                    toArray(Path[]::new)) {
                if (!p.equals(outputPath)) {
                    Files.delete(p);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not clear output folders.", e);
        }
    }
}
