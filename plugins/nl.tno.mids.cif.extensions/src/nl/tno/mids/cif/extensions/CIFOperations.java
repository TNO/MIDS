/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cif.extensions;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.eclipse.escet.cif.cif2cif.ElimComponentDefInst;
import org.eclipse.escet.cif.cif2cif.PrintFileIntoDecls;
import org.eclipse.escet.cif.cif2cif.SvgFileIntoDecls;
import org.eclipse.escet.cif.common.CifScopeUtils;
import org.eclipse.escet.cif.common.CifValueUtils;
import org.eclipse.escet.cif.explorer.CifAutomatonBuilder;
import org.eclipse.escet.cif.explorer.ExplorerStateFactory;
import org.eclipse.escet.cif.explorer.app.AutomatonNameOption;
import org.eclipse.escet.cif.explorer.runtime.BaseState;
import org.eclipse.escet.cif.explorer.runtime.Explorer;
import org.eclipse.escet.cif.explorer.runtime.ExplorerBuilder;
import org.eclipse.escet.cif.io.CifReader;
import org.eclipse.escet.cif.merger.CifMerger;
import org.eclipse.escet.cif.merger.CifMergerPostCheckEnv;
import org.eclipse.escet.cif.metamodel.cif.Component;
import org.eclipse.escet.cif.metamodel.cif.Group;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.java.CifConstructors;
import org.eclipse.escet.cif.typechecker.postchk.CifSvgPostChecker;
import org.eclipse.escet.cif.typechecker.postchk.CyclePostChecker;
import org.eclipse.escet.common.app.framework.AppEnv;
import org.eclipse.escet.common.app.framework.Application;
import org.eclipse.escet.common.app.framework.DummyApplication;
import org.eclipse.escet.common.app.framework.exceptions.UnsupportedException;
import org.eclipse.escet.common.app.framework.io.AppStreams;
import org.eclipse.escet.common.app.framework.options.GuiMode;
import org.eclipse.escet.common.app.framework.options.GuiOption;
import org.eclipse.escet.common.app.framework.options.Options;
import org.eclipse.escet.common.app.framework.output.OutputMode;
import org.eclipse.escet.common.app.framework.output.OutputModeOption;
import org.eclipse.escet.common.java.Sets;
import org.eclipse.escet.common.typechecker.SemanticException;

import com.google.common.base.Preconditions;

/**
 * Utilities relating to CIF automata.
 */
public class CIFOperations {
    /**
     * Retrieve a named component of type componentType directly contained in a specification.
     * 
     * @param cif {@link Specification} containing component.
     * @param name Name of component to retrieve.
     * @param componentType Type of the component to retrieve.
     * @return Named component if present in specification, null otherwise.
     * 
     * @note This method does not support component instantiation or nested components.
     */
    @SuppressWarnings("unchecked")
    public static <C extends Component> C getComponentByName(Specification cif, String name, Class<C> componentType) {
        for (Component c: cif.getComponents()) {
            if (c.getName().equals(name) && (componentType.isInstance(c))) {
                return (C)c;
            }
        }
        return null;
    }

    /**
     * Compute the statespace of the automata in a specification.
     * 
     * @param cif {@link Specification} containing automata to convert.
     * @return {@link Specification} containing statespace automaton.
     */
    public static Specification convertToStateSpace(Specification cif) {
        Application<?> app = new DummyApplication(new AppStreams());
        Options.set(OutputModeOption.class, OutputMode.ERROR);
        Options.set(AutomatonNameOption.class, null);

        Specification statespace;
        try {
            ExplorerBuilder builder = new ExplorerBuilder(cif);
            builder.collectData();
            ExplorerStateFactory stateFactory = new ExplorerStateFactory();
            Explorer explorer = builder.buildExplorer(stateFactory);
            List<BaseState> initials = explorer.getInitialStates(app);
            Preconditions.checkArgument(initials != null && !initials.isEmpty());
            Queue<BaseState> queue = new ArrayDeque<>();
            queue.addAll(initials);
            while (!queue.isEmpty()) {
                BaseState state = queue.poll();
                queue.addAll(state.getNewSuccessorStates());
            }
            explorer.renumberStates();
            explorer.minimizeEdges();
            CifAutomatonBuilder statespaceBuilder = new CifAutomatonBuilder();
            statespace = statespaceBuilder.createAutomaton(explorer, cif);
        } finally {
            AppEnv.unregisterApplication();
        }
        return statespace;
    }

    /**
     * Read CIF {@link Specification} from given {@link Path}.
     * 
     * @param path {@link Path} of input file.
     * @return {@link Specification} read from input file.
     */
    public static Specification loadCIFSpec(Path path) {
        CifReader reader = new CifReader();
        reader.suppressWarnings = true;
        reader.init(path.toString(), path.toAbsolutePath().toString(), false);
        return reader.read();
    }

    /**
     * Read CIF {@link Specification} from given {@link String}.
     *
     * @param cifSpecString {@link String} representation of input a CIF specification.
     * @return {@link Specification} constructed from the cifSpecString.
     */
    public static Specification read(String cifSpecString) {
        try {
            AppEnv.registerSimple();
            Options.set(GuiOption.class, GuiMode.OFF);
            CifReader cifReader = new CifReader().init("", "", false);
            Specification cifSpec = cifReader.read(cifSpecString);
            return cifSpec;
        } finally {
            AppEnv.unregisterApplication();
        }
    }

    /**
     * Merges the given specifications using the {@link CifMerger}.
     *
     * <p>
     * It is assumed the specifications have no relative paths that need to be adapted.
     * </p>
     *
     * <p>
     * It is assumed the specifications have no I/O declarations that require post checking.
     * </p>
     *
     * @param specifications The specifications to merge. Must provide at least one specification.
     * @return The merged specification.
     * @throws UnsupportedException If specifications can't be merged.
     */
    public static Specification mergeSpecifications(Collection<Specification> specifications) {
        Preconditions.checkArgument(!specifications.isEmpty());
        Specification mergedSpec = null;
        for (Specification specification: specifications) {
            if (mergedSpec == null) {
                mergedSpec = specification;
            } else {
                mergedSpec = mergeSpecifications(mergedSpec, specification);
            }
        }
        return mergedSpec;
    }

    /**
     * Merges the given specifications using the {@link CifMerger}.
     *
     * <p>
     * It is assumed the specifications have no relative paths that need to be adapted.
     * </p>
     *
     * <p>
     * It is assumed the specifications have no I/O declarations that require post checking.
     * </p>
     *
     * @param specification1 The first specification.
     * @param specification2 The second specification.
     * @return The merged specification.
     * @throws UnsupportedException If specifications can't be merged.
     */
    private static Specification mergeSpecifications(Specification specification1, Specification specification2) {
        // NOTE: Partial copy of CifMergerApp.
        // CifMergerApp should be refactored to allow invoking this as a static method, to avoid code duplication.

        // Preprocessing.
        new ElimComponentDefInst().transform(specification1);
        new SvgFileIntoDecls().transform(specification1);
        new PrintFileIntoDecls().transform(specification1);

        new ElimComponentDefInst().transform(specification2);
        new SvgFileIntoDecls().transform(specification2);
        new PrintFileIntoDecls().transform(specification2);

        // Assumes no relative paths in specifications. Adapting relative paths skipped.

        // Merge specifications.
        CifMerger merger = new CifMerger();
        Specification mergedSpec = merger.merge(specification1, specification2);

        // Check resulting specification (post check).
        CifMergerPostCheckEnv env = new CifMergerPostCheckEnv(null);
        try {
            // Same checks as CIF type checker, in same order.
            CyclePostChecker.check(mergedSpec, env);
            new CifSvgPostChecker(env).check(mergedSpec);
            // CifPrintPostChecker skipped (warnings only, no new problems).
            // SingleEventUsePerAutPostChecker skipped (no new problems).
        } catch (SemanticException ex) {
            // Ignore.
        }

        // Report post check problems.
        if (!env.errors.isEmpty()) {
            List<String> errors = Sets.sortedstrings(env.errors);
            String errorsTxt = String.join("\n", errors);
            throw new UnsupportedException(errorsTxt);
        }

        // Return merged specification.
        return mergedSpec;
    }

    /**
     * Rename the automaton, identified by automatonName within the given specification, to the new name. If a group
     * with the new name already exists in the specification, the declarations from that group will be moved to the
     * automaton via {@link #moveGroupDeclarationsToAutomaton}.
     * 
     * @param specification {@link Specification} containing automaton to rename.
     * @param automatonName Current name of automaton to rename.
     * @param newName New name for renamed automaton.
     */
    public static void renameAutomaton(Specification specification, String automatonName, String newName) {
        // Get the automaton.
        Automaton automaton = CIFOperations.getComponentByName(specification, automatonName, Automaton.class);
        Preconditions.checkNotNull(automaton, "The automaton with name '" + automatonName
                + "' can not be found in specification '" + specification.getName() + "'");

        // If a group with the new name already exists, move all declarations from that group to the automaton.
        Group group = CIFOperations.getComponentByName(specification, newName, Group.class);
        if (group != null) {
            moveGroupDeclarationsToAutomaton(group, automaton);
            // Remove the group.
            specification.getComponents().remove(group);
        }

        // Rename the automaton.
        automaton.setName(newName);
    }

    /**
     * Move the declarations from the {@link Group} to the {@link Automaton}.
     * 
     * <p>
     * It is assumed that:
     * <ul>
     * <li>both the automaton and the group are not null.</li>
     * <li>the group has no components.</li>
     * <li>the automaton and the group don't have declarations with the same name.</li>
     * </ul>
     * </p>
     * 
     * @param group {@link Group} containing declarations to move.
     * @param automaton {@link Automaton} to contain moved declarations.
     */
    private static void moveGroupDeclarationsToAutomaton(Group group, Automaton automaton) {
        Preconditions.checkNotNull(group, "The group can not be null.");
        Preconditions.checkNotNull(automaton, "The automaton can not be null.");
        Preconditions.checkState(group.getComponents().isEmpty(),
                "Group with name '" + group.getName() + "' has components. This is not allowed.");

        // Make sure no name clashes will occur.
        Set<String> automatonSymbols = CifScopeUtils.getSymbolNamesForScope(automaton, null);
        Set<String> groupSymbols = CifScopeUtils.getSymbolNamesForScope(group, null);
        Preconditions.checkState(!automatonSymbols.stream().anyMatch(s -> groupSymbols.contains(s)),
                String.format("Automaton %s and group %s have clashing words.", automaton.getName(), group.getName()));

        // Move that group declarations to the automaton
        automaton.getDeclarations().addAll(group.getDeclarations());
        automaton.getInvariants().addAll(group.getInvariants());
    }

    /**
     * Create automaton with empty language.
     * 
     * @param name Name of created automaton.
     * @return {@link Automaton} with empty language and given name.
     */
    private static Automaton createEmptyLanguageAutomaton(String name) {
        Automaton automaton = CifConstructors.newAutomaton();
        automaton.setName(name);
        Location location = CifConstructors.newLocation();
        location.getInitials().add(CifValueUtils.makeTrue());
        automaton.getLocations().add(location);
        return automaton;
    }

    /**
     * Create a specification containing one automaton with an empty language.
     * 
     * @param automatonName Name of created automaton.
     * @return {@link Specification} containing empty language automaton.
     */
    public static Specification createEmptyLanguageSpecification(String automatonName) {
        Specification specification = CifConstructors.newSpecification();
        specification.setName("specification");
        specification.getComponents().add(createEmptyLanguageAutomaton(automatonName));
        return specification;
    }
}
