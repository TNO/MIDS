/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cmi.protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.escet.cif.cif2cif.ElimGroups;
import org.eclipse.escet.cif.cif2cif.ElimTauEvent;
import org.eclipse.escet.cif.cif2cif.LiftEvents;
import org.eclipse.escet.cif.cif2mcrl2.Cif2Mcrl2Application;
import org.eclipse.escet.cif.common.CifCollectUtils;
import org.eclipse.escet.cif.io.CifWriter;
import org.eclipse.escet.cif.metamodel.cif.Component;
import org.eclipse.escet.cif.metamodel.cif.Group;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.declarations.Declaration;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.cif.metamodel.java.CifConstructors;
import org.eclipse.escet.common.app.framework.AppEnv;
import org.eclipse.escet.common.java.Assert;
import org.eclipse.escet.common.java.Sets;

import com.google.common.base.Preconditions;

import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.automata.fsa.impl.compact.CompactNFA;
import net.automatalib.util.automata.fsa.NFAs;
import nl.tno.mids.automatalib.extensions.cif.AutomataLibToCif;
import nl.tno.mids.automatalib.extensions.cif.CifToAutomataLib;
import nl.tno.mids.automatalib.extensions.util.AldebaranUtil;
import nl.tno.mids.automatalib.extensions.util.AutomataLibUtil;
import nl.tno.mids.cif.extensions.CIFOperations;
import nl.tno.mids.cif.extensions.CifExtensions;
import nl.tno.mids.cif.extensions.WindowsLongPathSupport;
import nl.tno.mids.cmi.api.general.CmiGeneralEventQueries;
import nl.tno.mids.cmi.api.protocol.CmiProtocolQueries;
import nl.tno.mids.common.MidsExecutableProvider;

/** Methods for creating protocol models. */
public class InferProtocolModel {
    /**
     * Create protocol model describing the interactions between two components.
     * 
     * @param protocolComponent1 Name of first component communicating in protocol, which must be present in
     *     {@link models}.
     * @param protocolComponent2 Name of second component communicating in protocol, which must be present in
     *     {@link models}.
     * @param contextNames Additional names of components that should be taken into account during protocol computation,
     *     which must all be present in {@link models}. The list may be empty.
     * @param models Models containing component behavior.
     * @param monitor Monitor to report progress.
     * @return {@link Specification} containing the computed protocol.
     */
    public static Specification createProtocol(String protocolComponent1, String protocolComponent2,
            List<String> contextNames, Map<String, Specification> models, IProgressMonitor monitor)
    {
        // Clean inputs.
        String componentName1 = protocolComponent1.trim();
        String componentName2 = protocolComponent2.trim();
        contextNames = contextNames.stream().map(name -> name.trim()).collect(Collectors.toList());

        // Get all component names, including context.
        Set<String> componentNames = new LinkedHashSet<>();
        componentNames.add(componentName1);
        componentNames.add(componentName2);
        componentNames.addAll(contextNames);

        // Check inputs.
        Assert.check(!componentName1.isEmpty(), "Name of first component must not be empty.");
        Assert.check(!componentName2.isEmpty(), "Name of second component must not be empty.");
        Assert.check(!componentName1.equals(componentName2),
                "Can't infer protocol for component " + componentName1 + " with itself.");
        Assert.check(!contextNames.contains(componentName1),
                "First component can't also be a context component: " + componentName1 + ".");
        Assert.check(!contextNames.contains(componentName2),
                "Second component can't also be a context component: " + componentName2 + ".");
        Assert.check(contextNames.size() == Sets.list2set(contextNames).size(),
                "Duplicate context component: " + String.join(", ", contextNames) + ".");
        Set<String> missingComponentNames = Sets.difference(componentNames, models.keySet());
        Preconditions.checkArgument(missingComponentNames.isEmpty(), "Not all required components are present: "
                + String.join(", ", Sets.sortedstrings(missingComponentNames)) + ".");

        // Get component models of protocol.
        Set<Specification> cifSpecs = models.entrySet().stream().filter(e -> componentNames.contains(e.getKey()))
                .map(e -> e.getValue()).collect(Collectors.toCollection(Sets::set));

        // Merge specifications.
        Specification mergedSpec = CIFOperations.mergeSpecifications(cifSpecs);

        // Make tau events explicit so CIF to mCRL2 transformation can handle them.
        ElimTauEvent elimTau = new ElimTauEvent();
        elimTau.transform(mergedSpec);

        // Get non-protocol events.
        List<Event> eventList = new ArrayList<>();
        CifCollectUtils.collectEvents(mergedSpec, eventList);

        Set<Event> nonProtocolEvents = new LinkedHashSet<>();
        nonProtocolEvents.addAll(eventList.stream()
                .filter(evt -> !CmiGeneralEventQueries.isCommunicationBetween(evt, componentName1, componentName2))
                .collect(Collectors.toList()));

        // Compute protocol state space, rename non-protocol actions to tau and weak-trace normalize. As part of this,
        // any data and tau events present are removed, and all states are marked, so the result is prefix-closed.
        Specification protocolSpec = convertToStateSpaceMCRL2(mergedSpec, nonProtocolEvents);

        Set<Automaton> protocolAutomata = CifExtensions.allAutomata(protocolSpec);
        Preconditions.checkArgument(protocolAutomata.size() == 1,
                "Unexpected number of statespaces found during protocol inference.");

        Automaton protocolAutomaton = protocolAutomata.iterator().next();

        // Determinize protocol model.
        FastNFA<String> protocolNfa = CifToAutomataLib.cifAutomatonToFastNfa(protocolAutomaton, true);

        CompactDFA<String> protocolDfa = NFAs.determinize(protocolNfa, protocolNfa.getInputAlphabet(), true, false);

        // Minimize DFA separately, for better performance.
        CompactDFA<String> minimizedProtocolDfa = AutomataLibUtil.minimizeDFA(protocolDfa);

        // Create protocol CIF specification with proper name.
        String protocolName = CmiProtocolQueries.createProtocolName(protocolComponent1, protocolComponent2);

        Specification protocolSpecification = AutomataLibToCif.fsaToCifSpecification(minimizedProtocolDfa, protocolName,
                true);

        // Restore event names and scope.
        invertElimGroups(protocolSpecification);
        return protocolSpecification;
    }

    /**
     * Compute the statespace of the automata in a specification.
     * 
     * <p>
     * Note that mCRL2 does not support marking states, so any marking present in the input specification will be
     * ignored and all states in the result will be marked.
     * </p>
     * 
     * @param specification {@link Specification} containing automata to combine into single statespace.
     * @param nonProtocolEvents {@link Set} of events that should be filtered from the protocol.
     * @return {@link Specification} containing statespace automaton.
     */
    private static Specification convertToStateSpaceMCRL2(Specification specification, Set<Event> nonProtocolEvents) {
        // Create temporary files.
        Path cifPath = null;
        Path mcrl2Path = null;
        Path lpsPath = null;
        Path autPath = null;
        Path autMinPath = null;
        try {
            cifPath = Files.createTempFile("cif2mcrl2", ".cif");
            mcrl2Path = Files.createTempFile("cif2mcrl2", ".mcrl2");
            lpsPath = Files.createTempFile("cif2mcrl2", ".lps");
            autPath = Files.createTempFile("cif2mcrl2", ".aut");
            autMinPath = Files.createTempFile("cif2mcrl2min", ".aut");
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temporary files.", e);
        }

        // Prefix event names to allow reverting pre-processing later on.
        prefixEvents(specification);

        // Pre-process CIF specification to ensure it is supported by CIF to mCRL2.
        AppEnv.registerSimple();
        try {
            // Eliminate groups as precondition for lifting events.
            ElimGroups elimGroups = new ElimGroups();
            elimGroups.transform(specification);

            // Lift events as workaround for an Eclipse ESCET issue.
            // See https://gitlab.eclipse.org/eclipse/escet/escet/-/issues/224.
            LiftEvents liftEvents = new LiftEvents();
            liftEvents.transform(specification);

            // Write pre-processed CIF specification.
            CifWriter.writeCifSpec(specification, cifPath.toString(), cifPath.toString());
        } finally {
            AppEnv.unregisterApplication();
        }

        // Convert CIF to mCRL2.
        Cif2Mcrl2Application cif2mcrl = new Cif2Mcrl2Application();
        String[] args = {cifPath.toString(), "--read-values=*,-*", "-o", mcrl2Path.toString()};
        // CIF to mCRL2 handles AppEnv registration itself, so no need to do that here.
        cif2mcrl.runTest(args);

        try {
            Files.delete(cifPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete temporary CIF file " + cifPath, e);
        }

        // Convert mCRL2 to LPS.
        try {
            execMCRL2TransCmd("mcrl22lps", mcrl2Path.toString(), lpsPath.toString(), mcrl2Path.getParent(), "--binary",
                    "--lin-method=regular");
        } catch (RuntimeException ex) {
            throw new RuntimeException("Failed to convert mCRL2 to LPS for " + mcrl2Path, ex);
        }

        try {
            Files.delete(mcrl2Path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete temporary mCRL2 file " + mcrl2Path, e);
        }

        // Convert LPS to LTS.
        try {
            execMCRL2TransCmd("lps2lts", lpsPath.toString(), autPath.toString(), lpsPath.getParent(), "--cached");
        } catch (RuntimeException ex) {
            throw new RuntimeException("Failed to convert LPS to LTS for " + lpsPath, ex);
        }

        try {
            Files.delete(lpsPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete temporary LPS file " + lpsPath, e);
        }

        // Weak-trace normalize the LTS.
        String nonProtocolString = nonProtocolEvents.stream().map(e -> e.getName()).collect(Collectors.joining(","));
        try {
            if (nonProtocolString.isEmpty()) {
                execMCRL2TransCmd("ltsconvert", autPath.toString(), autMinPath.toString(), lpsPath.getParent(),
                        "--equivalence=weak-trace", "-n");
            } else {
                execMCRL2TransCmd("ltsconvert", autPath.toString(), autMinPath.toString(), lpsPath.getParent(),
                        "--equivalence=weak-trace", "-n", "--tau=" + nonProtocolString);
            }
        } catch (RuntimeException ex) {
            throw new RuntimeException("Failed to weak-trace reduce LTS for " + mcrl2Path, ex);
        }

        try {
            Files.delete(autPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete temporary LTS file " + autPath, e);
        }

        // Read LTS.
        // All states will be accepting states as Aldebaran format has no such concept.
        CompactNFA<String> statespace = null;
        try {
            statespace = AldebaranUtil.readAldebaranCompactNfa(Files.newInputStream(autMinPath));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read LTS from disk for " + autMinPath, e);
        }

        try {
            Files.delete(autMinPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete temporary LTS file " + autMinPath, e);
        }

        // Convert LTS to CIF.
        return AutomataLibToCif.fsaToCifSpecification(statespace, "statespace", true);
    }

    /**
     * Apply mCRL2 to a given input with given options.
     * 
     * @param commandName Tool to apply.
     * @param inputPath String representing path to input file.
     * @param outputPath String representing path to output file.
     * @param workingDir {@link Path} to directory use as working directory by the tool.
     * @param options Options to provide to the tool.
     */
    private static void execMCRL2TransCmd(String commandName, String inputPath, String outputPath, Path workingDir,
            String... options)
    {
        ArrayList<String> command = new ArrayList<>();

        command.add(MidsExecutableProvider.getExecutablePath(commandName));
        command.addAll(Arrays.asList(options));
        command.add(WindowsLongPathSupport.ensureLongPathPrefix(inputPath));
        command.add(WindowsLongPathSupport.ensureLongPathPrefix(outputPath));
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workingDir.toFile());

        // Start process.
        String commandTxt = builder.command().stream().collect(Collectors.joining(" "));

        Process process;
        try {
            process = builder.start();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to create command: " + commandTxt, ex);
        }

        int exitCode;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException ex) {
            throw new RuntimeException("Unexpected interruption of sleep.", ex);
        }

        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                lines.add(line);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to process command output: " + commandTxt, ex);
        }

        if (exitCode != 0) {
            throw new RuntimeException("Error executing command: " + commandTxt + " (exit code " + exitCode + ")\n"
                    + String.join("\n", lines));
        }
    }

    /**
     * Prefix event names with "__" to make it possible to reconstruct component names from absolute names.
     * 
     * @param model Model containing events to rename.
     */
    private static void prefixEvents(Specification model) {
        List<Event> events = new ArrayList<>();
        CifCollectUtils.collectEvents(model, events);
        for (Event e: events) {
            String eventName = e.getName();
            e.setName("__" + eventName);
        }
    }

    /**
     * Move events into groups representing components.
     * 
     * @param model Model containing events to move into groups.
     */
    private static void invertElimGroups(Specification model) {
        List<Declaration> decls = new ArrayList<>(model.getDeclarations());

        for (Declaration decl: decls) {
            String declName = decl.getName();
            if (declName.contains("___")) {
                List<String> declNameParts = Arrays.asList(declName.split("___", 2));
                decl.setName(declNameParts.get(1));
                Group declGroup = getOrCreateGroup(model, declNameParts.get(0));
                declGroup.getDeclarations().add(decl);
            }
        }
    }

    /**
     * Return a group with given name from given model, creating it if it did not exist already.
     * 
     * @param model Model that should contain group.
     * @param name Name of group to retrieve.
     * @return {@link Group} with given name.
     */
    private static Group getOrCreateGroup(Specification model, String name) {
        Optional<Component> possGroup = model.getComponents().stream()
                .filter(c -> c instanceof Group && c.getName().equals(name)).findAny();
        if (possGroup.isPresent()) {
            return (Group)possGroup.get();
        }

        Group newGroup = CifConstructors.newGroup();
        newGroup.setName(name);
        model.getComponents().add(newGroup);
        return newGroup;
    }
}
