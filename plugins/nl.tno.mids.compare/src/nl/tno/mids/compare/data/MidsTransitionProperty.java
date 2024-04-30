/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.compare.data;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.tno.gltsdiff.glts.lts.automaton.diff.DiffKind;
import com.github.tno.gltsdiff.glts.lts.automaton.diff.DiffProperty;
import com.github.tno.gltsdiff.operators.combiners.Combiner;
import com.github.tno.gltsdiff.operators.combiners.EqualityCombiner;
import com.github.tno.gltsdiff.operators.combiners.SubtypeCombiner;
import com.github.tno.gltsdiff.operators.combiners.lts.automaton.diff.DiffPropertyCombiner;
import com.github.tno.gltsdiff.operators.hiders.Hider;
import com.github.tno.gltsdiff.operators.hiders.SubstitutionHider;
import com.github.tno.gltsdiff.operators.printers.HtmlPrinter;
import com.github.tno.gltsdiff.operators.printers.SetHtmlPrinter;
import com.github.tno.gltsdiff.operators.printers.StringHtmlPrinter;
import com.github.tno.gltsdiff.operators.printers.SubtypeHtmlPrinter;
import com.github.tno.gltsdiff.operators.printers.lts.automaton.diff.DiffPropertyHtmlPrinter;
import com.github.tno.gltsdiff.operators.projectors.IdentityProjector;
import com.github.tno.gltsdiff.operators.projectors.Projector;
import com.github.tno.gltsdiff.operators.projectors.SubtypeProjector;
import com.github.tno.gltsdiff.operators.projectors.lts.automaton.diff.DiffKindProjector;
import com.github.tno.gltsdiff.operators.projectors.lts.automaton.diff.DiffPropertyProjector;
import com.google.common.collect.ImmutableSet;

import nl.tno.mids.gltsdiff.extensions.AnnotatedProperty;
import nl.tno.mids.gltsdiff.extensions.AnnotatedPropertyCombiner;
import nl.tno.mids.gltsdiff.extensions.AnnotatedPropertyHtmlPrinter;
import nl.tno.mids.gltsdiff.extensions.AnnotatedPropertyProjector;

/** A MIDS transition property for structural comparison. */
public class MidsTransitionProperty extends AnnotatedProperty<String, DiffProperty<RepetitionCount>> {
    /** The MIDS transition property that represents tau. */
    public static final MidsTransitionProperty TAU = new MidsTransitionProperty("tau", ImmutableSet.of());

    /** A combiner for MIDS transition properties. */
    public static final Combiner<MidsTransitionProperty> COMBINER = new SubtypeCombiner<>(
            new AnnotatedPropertyCombiner<>(new EqualityCombiner<>(),
                    new DiffPropertyCombiner<>(new EqualityCombiner<>())),
            MidsTransitionProperty::new);

    /** A hider for MIDS transition properties. */
    public static final Hider<MidsTransitionProperty> HIDER = new SubstitutionHider<>(TAU);

    /** An HTML printer for MIDS transition properties. */
    public static final HtmlPrinter<MidsTransitionProperty> PRINTER = new SubtypeHtmlPrinter<>(
            new AnnotatedPropertyHtmlPrinter<>(new StringHtmlPrinter<>(), new SetHtmlPrinter<>(
                    new DiffPropertyHtmlPrinter<>(RepetitionCount.getHtmlPrinter()), "", " ", "")));

    /** A projector for MIDS transition properties. */
    public static final Projector<MidsTransitionProperty, DiffKind> PROJECTOR = new SubtypeProjector<>(
            new AnnotatedPropertyProjector<>(new IdentityProjector<>(),
                    new DiffPropertyProjector<>(new IdentityProjector<>(), new DiffKindProjector())),
            MidsTransitionProperty::new);

    /**
     * Instantiates a MIDS transition property.
     * 
     * @param property The non-{@code null} enclosed property.
     * @param annotations The non-{@code null} set of non-{@code null} annotations.
     */
    public MidsTransitionProperty(String property, Set<DiffProperty<RepetitionCount>> annotations) {
        super(property, annotations);
    }

    /**
     * Instantiates a MIDS transition property from an annotated property.
     * 
     * @param property The non-{@code null} annotated property.
     * @param diffKind The non-{@code null} difference kind to associate to all annotations.
     */
    public MidsTransitionProperty(AnnotatedProperty<String, RepetitionCount> property, DiffKind diffKind) {
        this(property.getProperty(),
                property.getAnnotations().stream().map(annotation -> new DiffProperty<>(annotation, diffKind))
                        .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    /**
     * Instantiates a MIDS transition property from an annotated property with difference information.
     * 
     * @param property The non-{@code null} annotated property.
     */
    public MidsTransitionProperty(AnnotatedProperty<String, DiffProperty<RepetitionCount>> property) {
        this(property.getProperty(), property.getAnnotations());
    }

    /**
     * Determines whether this MIDS transition property contains an annotation with the specified difference kind.
     * 
     * @param diffKind The difference kind for which to check containment.
     * @return {@code true} if this property has an annotation with {@code diffKind}, {@code false} otherwise.
     */
    public boolean has(DiffKind diffKind) {
        return getAnnotations().stream().anyMatch(annotation -> annotation.getDiffKind() == diffKind);
    }
}
