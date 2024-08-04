/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.compare.compute;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

import com.github.tno.gltsdiff.StructureComparator;
import com.github.tno.gltsdiff.glts.lts.automaton.diff.DiffAutomaton;
import com.github.tno.gltsdiff.glts.lts.automaton.diff.DiffAutomatonStateProperty;
import com.github.tno.gltsdiff.glts.lts.automaton.diff.DiffKind;
import com.github.tno.gltsdiff.glts.lts.automaton.diff.DiffProperty;
import com.github.tno.gltsdiff.mergers.DefaultMerger;
import com.github.tno.gltsdiff.operators.combiners.Combiner;
import com.github.tno.gltsdiff.operators.combiners.lts.automaton.diff.DiffAutomatonStatePropertyCombiner;
import com.github.tno.gltsdiff.operators.combiners.lts.automaton.diff.DiffPropertyCombiner;
import com.github.tno.gltsdiff.operators.hiders.Hider;
import com.github.tno.gltsdiff.operators.hiders.lts.automaton.diff.DiffPropertyHider;
import com.github.tno.gltsdiff.operators.inclusions.EqualToCombinationInclusion;
import com.github.tno.gltsdiff.rewriters.FixedPointRewriter;
import com.github.tno.gltsdiff.rewriters.LocalRedundancyRewriter;
import com.github.tno.gltsdiff.rewriters.NothingRewriter;
import com.github.tno.gltsdiff.rewriters.Rewriter;
import com.github.tno.gltsdiff.rewriters.SequenceRewriter;
import com.github.tno.gltsdiff.rewriters.lts.automaton.diff.SkipForkPatternRewriter;
import com.github.tno.gltsdiff.rewriters.lts.automaton.diff.SkipJoinPatternRewriter;
import com.github.tno.gltsdiff.utils.TriFunction;

import net.automatalib.automata.fsa.impl.compact.CompactNFA;
import nl.tno.mids.compare.data.ComparisonData;
import nl.tno.mids.compare.data.Entity;
import nl.tno.mids.compare.data.LatticeEdgeAnnotation;
import nl.tno.mids.compare.data.LatticeNode;
import nl.tno.mids.compare.data.MidsTransitionProperty;
import nl.tno.mids.compare.data.Model;
import nl.tno.mids.compare.data.RepetitionCount;
import nl.tno.mids.compare.data.Variant;
import nl.tno.mids.compare.options.CompareAlgorithm;
import nl.tno.mids.compare.options.CompareOptions;
import nl.tno.mids.compare.options.EntityType;
import nl.tno.mids.compare.output.Level6Writer;
import nl.tno.mids.gltsdiff.extensions.AnnotatedProperty;

/** Compute and write structural differences between variants. */
public class ComputeStructuralDifferences {
    private final ComparisonData comparisonData;

    private final CompareOptions options;

    private final EntityType entityType;

    /**
     * @param data {@link ComparisonData} containing entities with variants to compute differences for.
     * @param options Settings controlling compare behavior.
     */
    public ComputeStructuralDifferences(ComparisonData data, CompareOptions options) {
        this.comparisonData = data;
        this.options = options;
        this.entityType = comparisonData.getEntityType();
    }

    /**
     * Compute and write structural differences per entity.
     * 
     * @param monitor Monitor to report progress.
     * @param warnings List to collect warnings generated during comparison.
     * @throws IOException In case of an I/O error.
     */
    public void computeAndWriteDifferences(IProgressMonitor monitor, List<String> warnings) throws IOException {
        SubMonitor subMonitor = SubMonitor.convert(monitor, comparisonData.getEntities().size());
        for (Entity entity: comparisonData.getEntities()) {
            subMonitor.split(1);
            computeAndWriteLatticeDifferences(warnings, entity);

            computeAndWriteInputDifferences(warnings, entity);
        }
    }

    private void computeAndWriteLatticeDifferences(List<String> warnings, Entity entity) throws IOException {
        for (LatticeNode<Variant<Model>> parentLatticeNode: entity.getLattice()) {
            Variant<Model> parentVariant = parentLatticeNode.getValue();
            if (parentVariant.getValue().hasBehavior()
                    && (!parentVariant.isComputed() || options.showComputedVariants))
            {
                for (LatticeNode<Variant<Model>> childLatticeNode: parentLatticeNode.getChildren()) {
                    Variant<Model> childVariant = childLatticeNode.getValue();
                    if (parentVariant.getValue().isStructuralComparable(options.structuralCompareSizeLimit)
                            && childVariant.getValue().isStructuralComparable(options.structuralCompareSizeLimit)
                            && childVariant.getValue().hasBehavior()
                            && (!childVariant.isComputed() || options.showComputedVariants))
                    {
                        DiffAutomaton<MidsTransitionProperty> diff = performStructuralComparison(
                                parentVariant.getValue().getStructuralAutomaton(),
                                childVariant.getValue().getStructuralAutomaton(), options.compareAlgorithm,
                                options.applyPostprocessing);

                        int nrAdded = Math.toIntExact(
                                diff.countInitialStates(s -> s.getProperty().getInitDiffKind() == DiffKind.ADDED)
                                        + diff.countTransitions(p -> p.getProperty().getDiffKind() == DiffKind.ADDED
                                                || p.getProperty().getProperty().has(DiffKind.ADDED)));

                        int nrRemoved = Math.toIntExact(
                                diff.countInitialStates(s -> s.getProperty().getInitDiffKind() == DiffKind.REMOVED)
                                        + diff.countTransitions(p -> p.getProperty().getDiffKind() == DiffKind.REMOVED
                                                || p.getProperty().getProperty().has(DiffKind.REMOVED)));

                        parentLatticeNode.getChildEdgeTo(childLatticeNode)
                                .addAnnotation(new LatticeEdgeAnnotation(nrAdded, 0, nrRemoved));
                        Level6Writer.write(parentVariant, childVariant, entity, diff, options, entityType, warnings);
                        entity.addStructuralDifferencePair(parentVariant, childVariant);
                    }
                }
            }
        }
    }

    private void computeAndWriteInputDifferences(List<String> warnings, Entity entity) throws IOException {
        for (LatticeNode<Variant<Model>> firstLatticeNode: entity.getLattice()) {
            Variant<Model> firstVariant = firstLatticeNode.getValue();
            if (firstVariant.getValue().hasBehavior() && !firstVariant.isComputed()) {
                for (LatticeNode<Variant<Model>> secondLatticeNode: entity.getLattice()) {
                    Variant<Model> secondVariant = secondLatticeNode.getValue();
                    if (secondVariant.getValue().hasBehavior() && !secondVariant.isComputed()
                            && firstVariant.getIdentifier() < secondVariant.getIdentifier()
                            && !firstLatticeNode.isAncestorOf(secondLatticeNode)
                            && !secondLatticeNode.isAncestorOf(firstLatticeNode)
                            && firstVariant.getValue().isStructuralComparable(options.structuralCompareSizeLimit)
                            && secondVariant.getValue().isStructuralComparable(options.structuralCompareSizeLimit))
                    {
                        DiffAutomaton<MidsTransitionProperty> diff = performStructuralComparison(
                                firstVariant.getValue().getStructuralAutomaton(),
                                secondVariant.getValue().getStructuralAutomaton(), options.compareAlgorithm,
                                options.applyPostprocessing);

                        Level6Writer.write(firstVariant, secondVariant, entity, diff, options, entityType, warnings);
                        entity.addStructuralDifferencePair(firstVariant, secondVariant);
                    }
                }
            }
        }
    }

    /**
     * Compares the structures of two NFAs.
     *
     * @param lhs First NFA.
     * @param rhs Second NFA.
     * @param compareAlgorithm The choice of algorithm to use for structural comparison.
     * @param applyPostprocessing Whether post-processing should be applied to the structural comparison result.
     * @return The structural comparison result.
     */
    public static DiffAutomaton<MidsTransitionProperty> performStructuralComparison(
            CompactNFA<AnnotatedProperty<String, RepetitionCount>> lhs,
            CompactNFA<AnnotatedProperty<String, RepetitionCount>> rhs, CompareAlgorithm compareAlgorithm,
            boolean applyPostprocessing)
    {
        // Mark every transition of LHS as being removed and every transition of RHS as being added.
        DiffAutomaton<MidsTransitionProperty> newLhs = StructuralComparisonUtils.convertToDiffAutomaton(lhs,
                DiffKind.REMOVED, property -> new MidsTransitionProperty(property, DiffKind.REMOVED));
        DiffAutomaton<MidsTransitionProperty> newRhs = StructuralComparisonUtils.convertToDiffAutomaton(rhs,
                DiffKind.ADDED, property -> new MidsTransitionProperty(property, DiffKind.ADDED));

        // Configure structural comparison.
        Combiner<DiffAutomatonStateProperty> statePropertyCombiner = new DiffAutomatonStatePropertyCombiner();
        Combiner<DiffProperty<MidsTransitionProperty>> transitionPropertyCombiner = new DiffPropertyCombiner<>(
                MidsTransitionProperty.COMBINER);
        Hider<DiffProperty<MidsTransitionProperty>> transitionPropertyHider = new DiffPropertyHider<>(
                MidsTransitionProperty.HIDER);
        TriFunction<DiffAutomatonStateProperty, DiffKind, Optional<DiffKind>, DiffAutomatonStateProperty> statePropertyTransformer = (
                sp, sd, id
        ) -> new DiffAutomatonStateProperty(sp.isAccepting(), sd, id);
        Rewriter<DiffAutomatonStateProperty, DiffProperty<MidsTransitionProperty>, DiffAutomaton<MidsTransitionProperty>> rewriter = applyPostprocessing
                ? new FixedPointRewriter<>(new SequenceRewriter<>(Arrays.asList(
                        new LocalRedundancyRewriter<>(transitionPropertyCombiner),
                        new SkipForkPatternRewriter<>(statePropertyCombiner, transitionPropertyCombiner,
                                transitionPropertyHider, new EqualToCombinationInclusion<>(), statePropertyTransformer),
                        new SkipJoinPatternRewriter<>(statePropertyCombiner, transitionPropertyCombiner,
                                transitionPropertyHider, new EqualToCombinationInclusion<>(),
                                statePropertyTransformer))))
                : new NothingRewriter<>();

        // Compare the structures of the adjusted LHS and RHS.
        DiffAutomaton<MidsTransitionProperty> diff = new StructureComparator<>(
                compareAlgorithm.getMatcher(statePropertyCombiner, transitionPropertyCombiner),
                new DefaultMerger<>(statePropertyCombiner, transitionPropertyCombiner, DiffAutomaton::new), rewriter)
                        .compare(newLhs, newRhs);
        return diff;
    }
}
