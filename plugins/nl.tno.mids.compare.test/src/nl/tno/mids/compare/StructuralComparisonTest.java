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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import com.github.tno.gltsdiff.StructureComparator;
import com.github.tno.gltsdiff.glts.lts.automaton.diff.DiffAutomaton;
import com.github.tno.gltsdiff.glts.lts.automaton.diff.DiffAutomatonStateProperty;
import com.github.tno.gltsdiff.glts.lts.automaton.diff.DiffKind;
import com.github.tno.gltsdiff.glts.lts.automaton.diff.DiffProperty;
import com.github.tno.gltsdiff.matchers.Matcher;
import com.github.tno.gltsdiff.matchers.lts.DynamicLTSMatcher;
import com.github.tno.gltsdiff.mergers.DefaultMerger;
import com.github.tno.gltsdiff.mergers.Merger;
import com.github.tno.gltsdiff.operators.combiners.Combiner;
import com.github.tno.gltsdiff.operators.combiners.lts.automaton.diff.DiffAutomatonStatePropertyCombiner;
import com.github.tno.gltsdiff.operators.combiners.lts.automaton.diff.DiffPropertyCombiner;
import com.github.tno.gltsdiff.operators.hiders.Hider;
import com.github.tno.gltsdiff.operators.hiders.lts.automaton.diff.DiffPropertyHider;
import com.github.tno.gltsdiff.operators.inclusions.EqualToCombinationInclusion;
import com.github.tno.gltsdiff.operators.projectors.Projector;
import com.github.tno.gltsdiff.rewriters.FixedPointRewriter;
import com.github.tno.gltsdiff.rewriters.LocalRedundancyRewriter;
import com.github.tno.gltsdiff.rewriters.NothingRewriter;
import com.github.tno.gltsdiff.rewriters.Rewriter;
import com.github.tno.gltsdiff.rewriters.SequenceRewriter;
import com.github.tno.gltsdiff.rewriters.lts.automaton.diff.SkipForkPatternRewriter;
import com.github.tno.gltsdiff.rewriters.lts.automaton.diff.SkipJoinPatternRewriter;
import com.github.tno.gltsdiff.scorers.lts.DynamicLTSScorer;
import com.github.tno.gltsdiff.utils.TriFunction;

import net.automatalib.automata.fsa.impl.compact.CompactNFA;
import nl.tno.mids.compare.compute.CompleteModelLattice;
import nl.tno.mids.compare.compute.CompleteModelSetLattice;
import nl.tno.mids.compare.compute.ComputeModelLattice;
import nl.tno.mids.compare.compute.ComputeModelSetLattice;
import nl.tno.mids.compare.compute.ComputeVariants;
import nl.tno.mids.compare.compute.StructuralComparisonUtils;
import nl.tno.mids.compare.data.ComparisonData;
import nl.tno.mids.compare.data.Entity;
import nl.tno.mids.compare.data.LatticeNode;
import nl.tno.mids.compare.data.MidsTransitionProperty;
import nl.tno.mids.compare.data.Model;
import nl.tno.mids.compare.data.ModelSet;
import nl.tno.mids.compare.data.RepetitionCount;
import nl.tno.mids.compare.data.Variant;
import nl.tno.mids.compare.input.ModelSetLoader;
import nl.tno.mids.compare.options.CmiCompareMode;
import nl.tno.mids.compare.options.CompareAlgorithm;
import nl.tno.mids.compare.options.CompareOptions;
import nl.tno.mids.compare.options.HslColorScheme;
import nl.tno.mids.compare.options.ModelType;
import nl.tno.mids.gltsdiff.extensions.AnnotatedProperty;
import nl.tno.mids.gltsdiff.extensions.DiffAutomatonUtils;

class StructuralComparisonTest {
    @ParameterizedTest
    @CsvFileSource(resources = "/testCaseList.csv")
    public void performStructuralComparisonTest(String testName, ModelType modelType, boolean applyPostprocessing)
            throws IOException
    {
        Path basePath = Paths.get("test").resolve(testName);
        Path inputPath = basePath.resolve("input");
        CompareOptions options = new CompareOptions();
        options.inputPath = inputPath;
        options.outputPath = basePath.resolve("output_actual");
        options.compareAlgorithm = CompareAlgorithm.DYNAMIC;
        options.applyPostprocessing = applyPostprocessing;
        options.modelType = modelType;
        options.cmiCompareMode = CmiCompareMode.AUTOMATIC;
        options.colorScheme = HslColorScheme.INTUITIVE;
        options.unionIntersectionSizeLimit = Integer.MAX_VALUE;
        options.structuralCompareSizeLimit = Integer.MAX_VALUE;

        List<String> warnings = new ArrayList<>();

        // Perform all computations.
        List<ModelSet> modelSets = ModelSetLoader.load(options, warnings);
        ComparisonData data = new ComparisonData(modelSets);
        ComputeVariants.computeEntityVariants(data);
        ComputeVariants.computeModelSetVariants(data);
        new ComputeModelLattice(data).computeLattices(new NullProgressMonitor());
        new ComputeModelSetLattice(data).computeModelSetLattice(new NullProgressMonitor());
        new CompleteModelLattice(data, options).completeLattices(new NullProgressMonitor(), new ArrayList<>());
        new CompleteModelSetLattice(data, options).completeModelSetLattice(new ArrayList<>());

        for (Entity entity: data.getEntities()) {
            // Iterate over all directly connected variant pairs.
            for (LatticeNode<Variant<Model>> latticeNode: entity.getLattice()) {
                for (LatticeNode<Variant<Model>> parentNode: latticeNode.getParents()) {
                    CompactNFA<AnnotatedProperty<String, RepetitionCount>> lhs = latticeNode.getValue().getValue()
                            .getStructuralAutomaton();
                    CompactNFA<AnnotatedProperty<String, RepetitionCount>> rhs = parentNode.getValue().getValue()
                            .getStructuralAutomaton();
                    performTest(lhs, rhs);
                }
            }

            // Iterate over all input variant pairs.
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
                            {
                                CompactNFA<AnnotatedProperty<String, RepetitionCount>> lhs = firstVariant.getValue()
                                        .getStructuralAutomaton();
                                CompactNFA<AnnotatedProperty<String, RepetitionCount>> rhs = secondVariant.getValue()
                                        .getStructuralAutomaton();
                                performTest(lhs, rhs);
                            }
                        }
                    }
                }
            }
        }
    }

    private void performTest(CompactNFA<AnnotatedProperty<String, RepetitionCount>> lhs,
            CompactNFA<AnnotatedProperty<String, RepetitionCount>> rhs)
    {
        // Mark every transition of LHS as being removed and every transition of RHS as being added.
        DiffAutomaton<MidsTransitionProperty> newLhs = StructuralComparisonUtils.convertToDiffAutomaton(lhs,
                DiffKind.REMOVED, property -> new MidsTransitionProperty(property, DiffKind.REMOVED));
        DiffAutomaton<MidsTransitionProperty> newRhs = StructuralComparisonUtils.convertToDiffAutomaton(rhs,
                DiffKind.ADDED, property -> new MidsTransitionProperty(property, DiffKind.ADDED));

        // Set up structural comparison.
        Combiner<DiffAutomatonStateProperty> statePropertyCombiner = new DiffAutomatonStatePropertyCombiner();
        Combiner<DiffProperty<MidsTransitionProperty>> transitionPropertyCombiner = new DiffPropertyCombiner<>(
                MidsTransitionProperty.COMBINER);
        Hider<DiffProperty<MidsTransitionProperty>> transitionPropertyHider = new DiffPropertyHider<>(
                MidsTransitionProperty.HIDER);
        TriFunction<DiffAutomatonStateProperty, DiffKind, Optional<DiffKind>, DiffAutomatonStateProperty> statePropertyTransformer = (
                sp, sd, id
        ) -> new DiffAutomatonStateProperty(sp.isAccepting(), sd, id);
        Rewriter<DiffAutomatonStateProperty, DiffProperty<MidsTransitionProperty>, DiffAutomaton<MidsTransitionProperty>> rewriter = new FixedPointRewriter<>(
                new SequenceRewriter<>(Arrays.asList(new LocalRedundancyRewriter<>(transitionPropertyCombiner),
                        new SkipForkPatternRewriter<>(statePropertyCombiner, transitionPropertyCombiner,
                                transitionPropertyHider, new EqualToCombinationInclusion<>(), statePropertyTransformer),
                        new SkipJoinPatternRewriter<>(statePropertyCombiner, transitionPropertyCombiner,
                                transitionPropertyHider, new EqualToCombinationInclusion<>(),
                                statePropertyTransformer))));

        Matcher<DiffAutomatonStateProperty, DiffProperty<MidsTransitionProperty>, DiffAutomaton<MidsTransitionProperty>> matcher = new DynamicLTSMatcher<>(
                statePropertyCombiner, transitionPropertyCombiner,
                new DynamicLTSScorer<>(statePropertyCombiner, transitionPropertyCombiner));
        Merger<DiffAutomatonStateProperty, DiffProperty<MidsTransitionProperty>, DiffAutomaton<MidsTransitionProperty>> merger = new DefaultMerger<>(
                statePropertyCombiner, transitionPropertyCombiner, DiffAutomaton::new);

        // Compare the structures of the adjusted LHS and RHS.
        DiffAutomaton<MidsTransitionProperty> diffNoRewriting = new StructureComparator<>(matcher, merger,
                new NothingRewriter<>()).compare(newLhs, newRhs);

        // Assert that the projections of the difference automaton are language equivalent to the originals.
        Projector<MidsTransitionProperty, DiffKind> projector = MidsTransitionProperty.PROJECTOR;
        assertTrue(StructuralComparisonUtils.areLanguageEquivalent(newLhs,
                DiffAutomatonUtils.projectLeft(diffNoRewriting, projector)));
        assertTrue(StructuralComparisonUtils.areLanguageEquivalent(newRhs,
                DiffAutomatonUtils.projectRight(diffNoRewriting, projector)));

        // Compare with rewriting.
        DiffAutomaton<MidsTransitionProperty> diffWithRewriting = new StructureComparator<>(matcher, merger, rewriter)
                .compare(newLhs, newRhs);

        // Assert that post-processing was language preserving.
        assertTrue(StructuralComparisonUtils.areWeakLanguageEquivalent(diffWithRewriting, diffNoRewriting, projector,
                MidsTransitionProperty.TAU));
    }
}
