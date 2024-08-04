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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.escet.common.java.Sets;

import com.google.common.base.Preconditions;

/**
 * Represent a node in a lattice.
 *
 * @param <T> Type of lattice node payload.
 */
public class LatticeNode<T> {
    /** The representative value for this lattice node. */
    private final T value;

    /** Whether at least one parent or child has not been computed for performance reasons. */
    boolean hasIncompleteLattice = false;

    /**
     * Set of lattice edges linking this lattice node to parent nodes, i.e. nodes behaviorally contained in this node.
     */
    private final List<LatticeEdge<T>> parentEdges = new ArrayList<>();

    /**
     * Set of lattice edges linking this lattice node to child nodes, i.e. nodes behaviorally containing this node.
     */
    private final List<LatticeEdge<T>> childEdges = new ArrayList<>();

    /**
     * Construct a lattice node for a given value.
     * 
     * @param value The value to be contained in the lattice node.
     */
    public LatticeNode(T value) {
        this.value = value;
    }

    /**
     * @return The value contained in this lattice node.
     */
    public T getValue() {
        return value;
    }

    /**
     * @return The children of this lattice node, i.e. nodes behaviorally containing this node.
     */
    public List<LatticeNode<T>> getChildren() {
        return childEdges.stream().map(e -> e.getTarget()).collect(Collectors.toList());
    }

    /**
     * @return The edges connecting to the children of this lattice node.
     */
    public List<LatticeEdge<T>> getChildEdges() {
        return childEdges;
    }

    /**
     * Find the child edge connecting to the given node, if it exists.
     * 
     * @param otherNode The possible child node.
     * @return The edge connecting to the child of this lattice node, or {@code null} if there is no such edge.
     */
    public LatticeEdge<T> getChildEdgeTo(LatticeNode<T> otherNode) {
        Optional<LatticeEdge<T>> result = childEdges.stream().filter(e -> e.getTarget() == otherNode).findAny();
        return result.orElse(null);
    }

    /**
     * @return The parents of this lattice node, i.e. nodes behaviorally contained in this node.
     */
    public List<LatticeNode<T>> getParents() {
        return parentEdges.stream().map(e -> e.getSource()).collect(Collectors.toList());
    }

    /**
     * @return The edges connecting to the parents of this lattice node.
     */
    public List<LatticeEdge<T>> getParentEdges() {
        return parentEdges;
    }

    /**
     * Find the parent edge connecting to the given node, if it exists.
     * 
     * @param otherNode The possible parent node.
     * @return The edge connecting to the child of this lattice node, or {@code null} if there is no such edge.
     */
    public LatticeEdge<T> getParentEdgeTo(LatticeNode<T> otherNode) {
        Optional<LatticeEdge<T>> result = parentEdges.stream().filter(e -> e.getSource() == otherNode).findAny();
        return result.orElse(null);
    }

    /**
     * @return The set of ancestors of this lattice node, i.e. nodes behaviorally contained in this node.
     */
    public Set<LatticeNode<T>> getAncestors() {
        Set<LatticeNode<T>> result = new HashSet<>();

        for (LatticeNode<T> parent: getParents()) {
            result.add(parent);
            result.addAll(parent.getAncestors());
        }

        return result;
    }

    /**
     * Determine whether this lattice node is an ancestor of another lattice node, i.e. the other lattice node is a
     * descendant of this lattice node. A lattice node is not considered to be a ancestor/descendant of itself.
     * 
     * @param otherLatticeNode Possible descendant lattice node.
     * @return {@code true} if the other lattice node is in the transitive closure of the children relation of this
     *     lattice node, {@code false} otherwise.
     */
    public boolean isAncestorOf(LatticeNode<T> otherLatticeNode) {
        if (this == otherLatticeNode) {
            return false;
        }

        if (getChildren().contains(otherLatticeNode)) {
            return true;
        }

        for (LatticeNode<T> child: getChildren()) {
            if (child.isAncestorOf(otherLatticeNode)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @return The set of descendants of this lattice node, i.e. nodes behaviorally containing this node.
     */
    public Set<LatticeNode<T>> getDescendants() {
        Set<LatticeNode<T>> result = new HashSet<>();

        for (LatticeNode<T> child: getChildren()) {
            result.add(child);
            result.addAll(child.getChildren());
        }

        return result;
    }

    /**
     * Determine whether this lattice node is a descendant of another lattice node, i.e. the other lattice node is an
     * ancestor of this lattice node. A lattice node is not considered to be a ancestor/descendant of itself.
     * 
     * @param otherLatticeNode Possible ancestor lattice node.
     * @return {@code true} if the other lattice node is in the transitive closure of the parent relation of this
     *     lattice node, {@code false} otherwise.
     */
    public boolean isDescendantOf(LatticeNode<T> otherLatticeNode) {
        if (this == otherLatticeNode) {
            return false;
        }

        if (getParents().contains(otherLatticeNode)) {
            return true;
        }

        for (LatticeNode<T> parent: getParents()) {
            if (parent.isDescendantOf(otherLatticeNode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Find the closest common ancestor shared between this lattice node and a given other lattice node.
     * 
     * @param otherLatticeNode The other lattice node.
     * @return The closest common ancestor if there is one, or {@code null} if none exist. If multiple ancestors are
     *     closest, a random one is chosen.
     */
    public LatticeNode<T> findCommonAncestor(LatticeNode<T> otherLatticeNode) {
        if (this == otherLatticeNode) {
            return this;
        }

        if (this.isAncestorOf(otherLatticeNode)) {
            return this;
        }

        if (this.isDescendantOf(otherLatticeNode)) {
            return otherLatticeNode;
        }

        Set<LatticeNode<T>> commonAncestors = Sets.intersection(this.getAncestors(), otherLatticeNode.getAncestors());

        for (LatticeNode<T> latticeNode: commonAncestors) {
            Set<LatticeNode<T>> ancestorChildren = Sets.intersection(new HashSet<>(latticeNode.getChildren()),
                    commonAncestors);
            if (ancestorChildren.isEmpty()) {
                return latticeNode;
            }
        }

        return null;
    }

    /**
     * Find the closest common descendant shared between this lattice node and a given other lattice node.
     * 
     * @param otherLatticeNode The other lattice node.
     * @return The closest common descendant if there is one, or {@code null} if none exist. If multiple descendants are
     *     closest, a random one is chosen.
     */
    public LatticeNode<T> findCommonDescendant(LatticeNode<T> otherLatticeNode) {
        if (this == otherLatticeNode) {
            return this;
        }

        if (this.isAncestorOf(otherLatticeNode)) {
            return otherLatticeNode;
        }

        if (this.isDescendantOf(otherLatticeNode)) {
            return this;
        }

        Set<LatticeNode<T>> commonDescendants = Sets.intersection(this.getDescendants(),
                otherLatticeNode.getDescendants());

        for (LatticeNode<T> latticeNode: commonDescendants) {
            Set<LatticeNode<T>> descendantParents = Sets.intersection(new HashSet<>(latticeNode.getParents()),
                    commonDescendants);
            if (descendantParents.isEmpty()) {
                return latticeNode;
            }
        }

        return null;
    }

    /** @return Whether at least one parent or child has not been computed for performance reasons. */
    public boolean hasIncompleteLattice() {
        return hasIncompleteLattice;
    }

    /**
     * Store that children and/or parents have not been completely computed.
     */
    public void setLatticeIncomplete() {
        hasIncompleteLattice = true;
    }

    /**
     * Add a lattice edge connecting this lattice node to a parent lattice node, indicating this node behaviorally
     * contains the parent node.
     * 
     * @param edge Lattice edge to add.
     */
    void addParentEdge(LatticeEdge<T> edge) {
        Preconditions.checkArgument(edge.getTarget() == this, "Adding invalid edge to lattice.");
        Preconditions.checkArgument(parentEdges.stream().noneMatch(e -> e.isParallelEdge(edge)),
                "Adding duplicate edge to lattice.");
        parentEdges.add(edge);
    }

    /**
     * Add a lattice edge connecting this lattice node to a child lattice node, indicating this node is behaviorally
     * contained in the child node.
     * 
     * @param edge Lattice edge to add.
     */
    void addChildEdge(LatticeEdge<T> edge) {
        Preconditions.checkArgument(edge.getSource() == this, "Adding invalid edge to lattice.");
        Preconditions.checkArgument(childEdges.stream().noneMatch(e -> e.isParallelEdge(edge)),
                "Adding duplicate edge to lattice.");
        childEdges.add(edge);
    }

    /**
     * Remove a lattice edge connecting this lattice node to a parent lattice node.
     * 
     * @param edge Lattice edge to remove.
     */
    void removeParentEdge(LatticeEdge<T> edge) {
        Preconditions.checkArgument(parentEdges.contains(edge), "Cannot remove edge that is not present.");
        parentEdges.remove(edge);
    }

    /**
     * Remove a lattice edge connecting this lattice node to a child lattice node.
     * 
     * @param edge Lattice edge to remove.
     */
    void removeChildEdge(LatticeEdge<T> edge) {
        Preconditions.checkArgument(childEdges.contains(edge), "Cannot remove edge that is not present.");
        childEdges.remove(edge);
    }
}
