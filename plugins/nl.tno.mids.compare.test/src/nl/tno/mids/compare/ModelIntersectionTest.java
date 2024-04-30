/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.compare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.escet.cif.io.CifWriter;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.common.app.framework.AppEnv;
import org.eclipse.escet.common.java.Lists;
import org.junit.jupiter.api.Test;

import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import nl.tno.mids.automatalib.extensions.cif.AutomataLibToCif;
import nl.tno.mids.automatalib.extensions.cif.CifToAutomataLib;
import nl.tno.mids.automatalib.extensions.util.AutomataLibUtil;
import nl.tno.mids.cif.extensions.CIFOperations;
import nl.tno.mids.cif.extensions.CifExtensions;
import nl.tno.mids.common.unittest.FileCompare;

class ModelIntersectionTest {
    private static final String CIF_FILE_EXTENSION = "cif";

    /**
     * Test to validate intersection creation of 2 models.
     * 
     * @throws IOException when loading files fails.
     */
    @Test
    public void testModelIntersection() throws IOException {
        Path basePath = Paths.get("test").resolve("testModelIntersection");
        Path inputPath = basePath.resolve("input");
        Path expectedPath = basePath.resolve("output_expected");
        Path outputPath = basePath.resolve("output_actual/");

        if (Files.exists(outputPath)) {
            FileUtils.cleanDirectory(outputPath.toFile());
        }
        Files.createDirectories(outputPath);

        Path baseModelPath = inputPath.resolve("model/automaton.cif");
        Specification baseModel = CIFOperations.loadCIFSpec(baseModelPath);
        Specification variantModel = CIFOperations.loadCIFSpec(inputPath.resolve("model_variant/automaton.cif"));

        Set<Automaton> baseAutomata = CifExtensions.allAutomata(baseModel);
        assertEquals(1, baseAutomata.size());
        Automaton baseAut = Lists.set2list(baseAutomata).get(0);
        Set<Automaton> variantAutomata = CifExtensions.allAutomata(variantModel);
        assertEquals(1, baseAutomata.size());
        Automaton variantAut = Lists.set2list(variantAutomata).get(0);

        CompactDFA<String> baseDfa = CifToAutomataLib.cifAutomatonToCompactDfa(baseAut, false);
        CompactDFA<String> variantDfa = CifToAutomataLib.cifAutomatonToCompactDfa(variantAut, false);

        CompactDFA<String> intersection = AutomataLibUtil.intersectionMinimized(baseDfa, variantDfa);
        Path intersectionPath = outputPath.resolve("intersection.cif");
        Specification intersectionSpec = AutomataLibToCif.fsaToCifSpecification(intersection, "AUTOMATON", true);

        AppEnv.registerSimple();
        try {
            CifWriter.writeCifSpec(intersectionSpec, intersectionPath.toString(), baseModelPath.toString());
        } finally {
            AppEnv.unregisterThread();
        }

        assertTrue(Files.exists(outputPath));
        assertTrue(Files.isDirectory(outputPath));

        FileCompare.checkDirectoriesEqual(expectedPath, outputPath, ModelIntersectionTest::isCompareFile);
    }

    /**
     * Determine if a given path refers to a file that should be compared.
     * 
     * @param p {@link Path} to check.
     * @return {@code true} if the path refers to a cif file, {@code false} otherwise.
     */
    private static boolean isCompareFile(Path p) {
        return FilenameUtils.isExtension(p.getFileName().toString(), CIF_FILE_EXTENSION);
    }
}
