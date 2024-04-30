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

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.eclipse.escet.cif.metamodel.cif.Specification;

import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.automata.fsa.impl.compact.CompactNFA;
import nl.tno.mids.automatalib.extensions.util.AutomataLibUtil;
import nl.tno.mids.compare.data.Model;
import nl.tno.mids.compare.data.ModelSet;
import nl.tno.mids.compare.data.RepetitionCount;
import nl.tno.mids.compare.options.EntityType;
import nl.tno.mids.compare.options.ModelType;
import nl.tno.mids.gltsdiff.extensions.AnnotatedProperty;

/** A ModelSetBuilder to build a {@link ModelSet} holding models ({@link Model}) that apply to CIF specifications. */
public class CifModelSetBuilder extends BaseModelSetBuilder {
    private final EntityType entityType;

    /**
     * @param modelSetName Name of created {@link ModelSet}.
     * @param entityType Type of entity to compare. If the entity type is {@code null}, the model set builder will
     *     select "entity,entities" as entity type.
     */
    public CifModelSetBuilder(String modelSetName, EntityType entityType) {
        super(modelSetName);

        if (entityType != null) {
            this.entityType = entityType;
        } else {
            this.entityType = new EntityType("entity", "entities");
        }
    }

    @Override
    public ModelSet createModelSet(Map<Path, List<String>> descriptions) {
        return new ModelSet(modelSetName, models, getModelType(), entityType, descriptions);
    }

    @Override
    protected ModelType getModelType() {
        return ModelType.CIF;
    }

    @Override
    protected void validate() {
        // Nothing to validate specially for this builder.
    }

    @Override
    void add(Specification cifSpecification, String specificationName, List<String> warnings) {
        // Convert specification to NFA and DFA. This supports NFAs, tau events, data, and most other CIF concepts.
        CompactNFA<String> nfa = ModelSetBuilderUtils.convertCifSpecToNfa(cifSpecification);
        CompactDFA<String> dfa = ModelSetBuilderUtils.convertNfaToDfa(nfa);
        CompactNFA<AnnotatedProperty<String, RepetitionCount>> annotatedNfa = AutomataLibUtil.rename(nfa,
                AnnotatedProperty::new);
        add(new Model(cifSpecification, dfa, annotatedNfa, specificationName));
    }
}
