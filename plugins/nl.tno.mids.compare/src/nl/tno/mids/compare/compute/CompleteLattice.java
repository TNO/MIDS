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

import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import nl.tno.mids.compare.data.LatticeEdge;
import nl.tno.mids.compare.data.LatticeNode;
import nl.tno.mids.compare.data.Variant;

/**
 * Compute variants needed to complete lattice.
 * 
 * @param <T> Type of values contained in the variants.
 */
public abstract class CompleteLattice<T> {
    protected void completeLattice(List<LatticeNode<Variant<T>>> lattice) {
        // Compute the additional nodes needed to complete the lattice, in two steps.
        computeIntersectionLattice(lattice);
        computeUnionLattice(lattice);

        // Sort lattice to ensure consistent ordering.
        Collections.sort(lattice, (n1, n2) -> n1.getValue().compareTo(n2.getValue()));
    }

    /**
     * Attempt to compute all lattice nodes needed to complete the input lattice such that each pair of lattice nodes
     * has an unique closest shared ancestor. If some ancestor variants cannot be computed, the resulting lattice will
     * not be complete.
     * 
     * @param lattice List of initial lattice nodes.
     */
    private void computeIntersectionLattice(List<LatticeNode<Variant<T>>> lattice) {
        computeCombinedLattice(lattice, (t1, t2) -> computeIntersection(t1, t2), (n1, n2) -> new LatticeEdge<>(n2, n1));
    }

    /**
     * Attempt to compute all lattice nodes needed to complete the input lattice such that each pair of lattice nodes
     * has an unique closest shared descendant. If some descendant variants cannot be computed, the resulting lattice
     * will not be complete.
     * 
     * @param lattice List of initial lattice nodes.
     */
    private void computeUnionLattice(List<LatticeNode<Variant<T>>> lattice) {
        computeCombinedLattice(lattice, (t1, t2) -> computeUnion(t1, t2), (n1, n2) -> new LatticeEdge<>(n1, n2));
    }

    /**
     * Attempt to compute all lattice nodes needed to complete the input lattice by combining nodes with a given
     * function. If some combination cannot be computed, the resulting lattice will not be complete.
     * 
     * @param lattice List of initial lattice nodes.
     * @param combineFunction Function to compute a new {@link Variant} based on two input values. The function should
     *     be idempotent, associative and commutative.
     * @param connectFunction Function to connect the lattice node of a combined variant to an existing lattice node,
     *     where the first argument provided is the existing lattice node and the second argument provided is the
     *     combined lattice node.
     */
    private void computeCombinedLattice(List<LatticeNode<Variant<T>>> lattice,
            BiFunction<T, T, Variant<T>> combineFunction,
            BiConsumer<LatticeNode<Variant<T>>, LatticeNode<Variant<T>>> connectFunction)
    {
        // Get the input nodes (the non-computed nodes).
        List<LatticeNode<Variant<T>>> inputNodes = lattice.stream().filter(n -> !n.getValue().isComputed())
                .collect(Collectors.toList());

        // Compute combination nodes from existing nodes in a fixed point manner. Start with the input nodes only. On
        // the first call to this method, to compute intersection nodes, there is at most one computed node. If present,
        // it is the model with empty behavior that is added to ensure all model sets have a model for every entity.
        // There is no need to consider this model for intersections and unions, as 'x intersection empty = empty' and
        // 'x union empty = x', and both 'x' and 'empty' are already present and have already been related to each other
        // earlier on.
        // For the second call to this method, to compute union nodes, considering only the input nodes as starting
        // point ensures that we forgo unions involving nodes computed using intersections. We thus keep the
        // intersection part and the union part of the lattice separate. This keeps the lattice smaller, and leads to a
        // more intuitive lattice, without preventing us from creating lattices that are theoretically 'complete' (see
        // e.g. https://en.wikipedia.org/wiki/Complete_lattice).
        Deque<LatticeNode<Variant<T>>> variantsToProcess = new LinkedList<>(inputNodes);
        while (!variantsToProcess.isEmpty()) {
            // Select the current lattice node to process.
            // Each lattice node is processed exactly once.
            LatticeNode<Variant<T>> latticeNode = variantsToProcess.pop();

            // Determine the nodes to combine with the current lattice node:
            // - Consider only input nodes. Partially this is for the reason described above. Furthermore, there is no
            // need to consider computed nodes created during this fixed point loop. They all originate from
            // combinations of input nodes. Combining the newly constructed combination nodes with all input nodes is
            // sufficient to create theoretically 'complete' lattices.
            // - By using the commutativity property of the combine function, each pair of nodes only needs to be
            // considered once. That is, if '(x, y)' is considered, '(y, x)' does not need to be considered anymore as
            // it would result in the same combined node. This means we can reduce the number of required combination
            // computations by half, increasing performance. Furthermore, there is no need to combine nodes with
            // themselves, as that would result in the node itself, given the idempotent property of the combine
            // function, which also reduces the number of required computations. To exploit these properties, we combine
            // the current node only with nodes with a higher index.
            // - If two nodes are already related (one is an ancestor or descendant of the other), the combined variant
            // will be equal to one of the two nodes being combined, which are already in the lattice and are already
            // related to each other. We can thus forgo combining such nodes, further improving performance.
            Set<LatticeNode<Variant<T>>> others = inputNodes.stream()
                    .filter(n -> latticeNode.getValue().getIdentifier() > n.getValue().getIdentifier()
                            && !latticeNode.isAncestorOf(n) && !latticeNode.isDescendantOf(n))
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            for (LatticeNode<Variant<T>> otherLatticeNode: others) {
                // Try to compute combined variant.
                Variant<T> combinedVariant = combineFunction.apply(latticeNode.getValue().getValue(),
                        otherLatticeNode.getValue().getValue());
                if (combinedVariant == null) {
                    // Combined variant was not computed successfully, so mark lattice nodes as incomplete.
                    // No warnings are added here, as warnings are added by the derived classes, only once per lattice.
                    latticeNode.setLatticeIncomplete();
                    otherLatticeNode.setLatticeIncomplete();
                    continue;
                }

                // Check if variant already exists in the lattice.
                Optional<LatticeNode<Variant<T>>> possCombinedNode = lattice.stream()
                        .filter(n -> n.getValue().equals(combinedVariant)).findAny();
                if (possCombinedNode.isPresent()) {
                    // Variant already exists, so add missing edges to existing node.
                    LatticeNode<Variant<T>> combinedNode = possCombinedNode.get();
                    if (latticeNode != combinedNode && !latticeNode.isAncestorOf(combinedNode)
                            && !latticeNode.isDescendantOf(combinedNode))
                    {
                        connectFunction.accept(latticeNode, combinedNode);
                    }
                    if (otherLatticeNode != combinedNode && !otherLatticeNode.isAncestorOf(combinedNode)
                            && !otherLatticeNode.isDescendantOf(combinedNode))
                    {
                        connectFunction.accept(otherLatticeNode, combinedNode);
                    }
                } else {
                    // Variant does not already exist, so add a new node and corresponding edges.
                    LatticeNode<Variant<T>> combinedNode = new LatticeNode<>(combinedVariant);
                    lattice.add(combinedNode);
                    connectFunction.accept(latticeNode, combinedNode);
                    connectFunction.accept(otherLatticeNode, combinedNode);
                    variantsToProcess.add(combinedNode);
                }
            }
        }
    }

    /**
     * Compute an intersection of the behavior of two variants.
     * 
     * @param value Value representing the first variant to compute intersection for.
     * @param otherValue Value representing the second variant to compute intersection for.
     * @return An intersection variant containing only behavior occurring in both variants, if such can be computed,
     *     {@code null} otherwise. If the computed intersection is represented by an existing variant, that variant is
     *     returned, otherwise a new variant is created.
     */
    protected abstract Variant<T> computeIntersection(T value, T otherValue);

    /**
     * Compute a union of the behavior of two variants.
     * 
     * @param value Value representing the first variant to compute union for.
     * @param otherValue Value representing the second variant to compute union for.
     * @return A union variant containing all behavior occurring in either variant, if such can be computed,
     *     {@code null} otherwise. If the computed union is represented by an existing variant, that variant is
     *     returned, otherwise a new variant is created.
     */
    protected abstract Variant<T> computeUnion(T value, T otherValue);
}
