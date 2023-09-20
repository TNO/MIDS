/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.compare.data;

import org.eclipse.escet.cif.metamodel.cif.Specification;

import com.google.common.base.Preconditions;

import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.automata.fsa.impl.compact.CompactNFA;
import net.automatalib.util.automata.fsa.DFAs;
import nl.tno.mids.automatalib.extensions.util.AutomataLibUtil;
import nl.tno.mids.cif.extensions.CIFOperations;
import nl.tno.mids.gltsdiff.extensions.AnnotatedProperty;

/**
 * Data concerning a model in one of the model sets.
 */
public class Model {
    private final String entityName;

    private final Specification originalSpecification;

    private final CompactDFA<String> languageAutomaton;

    private final CompactNFA<AnnotatedProperty<String, RepetitionCount>> structuralAutomaton;

    protected final int size;

    protected final boolean hasBehavior;

    private Variant<Model> variant;

    /**
     * Constructs a new {@link Model} that consists of three views of a specification with an empty language.
     * 
     * @param entityName The entity name.
     */
    public Model(String entityName) {
        this(CIFOperations.createEmptyLanguageSpecification(entityName),
                AutomataLibUtil.createEmptyLanguageCompactDfa(), AutomataLibUtil.createEmptyLanguageCompactNfa(),
                entityName);
    }

    /**
     * Constructs a new {@link Model} that consists of three (potentially) different views of the same input
     * specification.
     * 
     * @param originalSpec The original input CIF specification.
     * @param languageSpec The DFA that should be used for all language-related purposes. This DFA must be minimal, must
     *     not contain 'tau' events, and must be language equivalent to {@code originalSpec}.
     * @param structuralSpec The NFA that should be used for all structural-related purposes. This NFA must resemble
     *     {@code originalSpec}, for example by having repetition-related information encoded as annotations.
     * @param entityName The entity name.
     */
    public Model(Specification originalSpec, CompactDFA<String> languageSpec,
            CompactNFA<AnnotatedProperty<String, RepetitionCount>> structuralSpec, String entityName)
    {
        Preconditions.checkNotNull(originalSpec);
        Preconditions.checkNotNull(languageSpec);
        Preconditions.checkArgument(languageSpec.size() > 0,
                "One of the language specifications of " + entityName + " contains no states.");
        Preconditions.checkNotNull(structuralSpec);
        Preconditions.checkArgument(structuralSpec.size() > 0,
                "One of the structural specifications of " + entityName + " contains no states.");
        this.originalSpecification = originalSpec;
        this.languageAutomaton = languageSpec;
        this.structuralAutomaton = structuralSpec;
        this.entityName = entityName;
        this.size = AutomataLibUtil.countTransitions(structuralAutomaton);
        this.hasBehavior = !DFAs.acceptsEmptyLanguage(languageAutomaton);

        // Ensure language specification has no tau events.
        Preconditions.checkArgument(!languageAutomaton.getInputAlphabet().containsSymbol("tau"), "Model " + entityName
                + " contains specification that contains tau steps when converted to DFA, this is not supported.");

        // Ensure no unreachable states. Required for AutomataLib accepts empty language check.
        Preconditions.checkArgument(!AutomataLibUtil.hasUnreachableStates(languageAutomaton),
                "Model " + entityName + " contains specification that has unreachable states, this is not supported.");
    }

    /**
     * 
     * @return The original input CIF specification. This specification may contain data.
     *     <p>
     *     This specification must not be used for any language-related or structural-related checks and operations. It
     *     should only be used for writing the original CIF specification to disk. For language-related purposes use
     *     {@link #getLanguageAutomaton()} instead. For structural-related purposes use
     *     {@link #getStructuralAutomaton()} instead.
     *     </p>
     */
    public Specification getOriginalSpecification() {
        return originalSpecification;
    }

    /**
     * @return The DFA that is to be used for language-related checks and operations. The returned DFA should be
     *     minimal, should not contain 'tau' events, and should be language equivalent to
     *     {@link #getOriginalSpecification()}.
     */
    public CompactDFA<String> getLanguageAutomaton() {
        return languageAutomaton;
    }

    /**
     * @return The NFA that is to be used for structural-related checks and operations, like structural comparison.
     *     <p>
     *     Note that the returned NFA may have a different language than the original input specification, for example
     *     as result of encoding repetition-related data. The returned NFA should therefore only be used for
     *     structural-related purposes, like computing difference automata for level 6. For language-related purposes
     *     use {@link #getLanguageAutomaton()} instead.
     *     </p>
     */
    public CompactNFA<AnnotatedProperty<String, RepetitionCount>> getStructuralAutomaton() {
        return structuralAutomaton;
    }

    /**
     * @return The entity name.
     */
    public String getEntityName() {
        return entityName;
    }

    /**
     * @return The variant.
     */
    public Variant<Model> getVariant() {
        return variant;
    }

    /**
     * @param variant The variant to set.
     */
    public void setVariant(Variant<Model> variant) {
        this.variant = variant;
    }

    /**
     * Returns the number of 'transitions' and 'initial state arrows' of the model.
     *
     * @return The number of 'transitions' and 'initial state arrows'.
     */
    public int getModelSize() {
        return size;
    }

    /**
     * Check if the model has behavior.
     * 
     * @return {@code false} if the model has no transitions, {@code true} otherwise.
     */
    public boolean hasBehavior() {
        return hasBehavior;
    }

    /**
     * Determine whether the model is within the union/intersection size limit.
     *
     * @param unionIntersectionSizeLimit The maximum size of an automaton (measured in the number of states) to be
     *     considered for union/intersection.
     * @return {@code true} if the model is to be considered, {@code false} otherwise.
     */
    public boolean isWithinUnionIntersectionSizeLimit(int unionIntersectionSizeLimit) {
        Preconditions.checkArgument(0 <= unionIntersectionSizeLimit);
        return languageAutomaton.size() <= unionIntersectionSizeLimit;
    }

    /**
     * Determine whether the model is to be considered for structural comparison.
     *
     * @param structuralCompareSizeLimit The maximum size of an automaton (measured in the number of states) to be
     *     considered for structural comparison.
     * @return {@code true} if the model is to be considered, {@code false} otherwise.
     */
    public boolean isStructuralComparable(int structuralCompareSizeLimit) {
        return structuralAutomaton.size() <= structuralCompareSizeLimit;
    }
}
