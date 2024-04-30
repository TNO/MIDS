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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.escet.common.java.Sets;

import com.google.common.base.Preconditions;

/**
 * Represent an edge in a lattice.
 *
 * @param <T> Type of lattice node payload.
 */
public class LatticeEdge<T> {
    /**
     * Data associated with this lattice edge.
     * 
     * <p>
     * Will be {@code null} when constructed, until added with {@link #addAnnotation}. Only one annotation can be added
     * per edge.
     * </p>
     */
    private LatticeEdgeAnnotation annotation;

    /**
     * Lattice node that is the source of this lattice edge, which means it is behaviorally contained in the target
     * node.
     */
    private LatticeNode<T> source;

    /** Lattice node that is the target of this lattice edge, which means it behaviorally contains the source node. */
    private LatticeNode<T> target;

    /**
     * Create a lattice edge between two lattice nodes, indicating the source node is behaviorally contained in the
     * target node.
     * 
     * @param source Source node of the edge.
     * @param target Target node of the edge.
     */
    public LatticeEdge(LatticeNode<T> source, LatticeNode<T> target) {
        Preconditions.checkArgument(source != target, "Source of lattice edge cannot be the same as the target edge");
        Preconditions.checkArgument(!source.getChildren().contains(target), "Lattice edge already present in source.");
        Preconditions.checkArgument(!target.getParents().contains(source), "Lattice edge already present in target.");

        // We don't want multiple paths between any two nodes in the lattice.
        // Therefore, we need to remove existing edges from nodes that can reach the source node to nodes that can be
        // reached from the target node.
        Set<LatticeNode<T>> sourceNodes = new HashSet<>(source.getAncestors());
        sourceNodes.add(source);
        Set<LatticeNode<T>> targetNodes = new HashSet<>(target.getDescendants());
        targetNodes.add(target);
        for (LatticeNode<T> sourceNode: sourceNodes) {
            // If any children of sourceNode are descendants of target, remove the existing edge.
            Set<LatticeNode<T>> duplicateTargets = Sets.intersection(new HashSet<>(sourceNode.getChildren()),
                    targetNodes);
            for (LatticeNode<T> duplicateTarget: duplicateTargets) {
                sourceNode.getChildEdgeTo(duplicateTarget).remove();
            }
        }

        this.source = source;
        source.addChildEdge(this);
        this.target = target;
        target.addParentEdge(this);
    }

    /**
     * Add data to the lattice edge. This may only be done if the current data is {@code null}.
     * 
     * @param annotation Data to add to the lattice edge.
     */
    public void addAnnotation(LatticeEdgeAnnotation annotation) {
        Preconditions.checkArgument(this.annotation == null, "Cannot add annotation to edge if already present");
        this.annotation = annotation;
    }

    /**
     * @return The data associated with this lattice edge. May be {@code null} if no annotation has been added yet.
     */
    public LatticeEdgeAnnotation getAnnotation() {
        return annotation;
    }

    /**
     * @return The source lattice node of this edge, which is behaviorally contained in the target lattice node.
     */
    public LatticeNode<T> getSource() {
        return source;
    }

    /**
     * @return The target lattice node of this edge, which behaviorally contains the source lattice node.
     */
    public LatticeNode<T> getTarget() {
        return target;
    }

    /**
     * Check whether this lattice edge is parallel to another one, i.e. connects the same source to the same target.
     * 
     * @param otherEdge the potentially parallel edge.
     * @return {@code true} if this edge has the same source and target node as {@code otherEdge}, {@code false}
     *     otherwise.
     */
    public boolean isParallelEdge(LatticeEdge<T> otherEdge) {
        return source == otherEdge.getSource() && target == otherEdge.getTarget();
    }

    /**
     * Remove this edge from the lattice.
     */
    public void remove() {
        source.removeChildEdge(this);
        target.removeParentEdge(this);
    }
}
