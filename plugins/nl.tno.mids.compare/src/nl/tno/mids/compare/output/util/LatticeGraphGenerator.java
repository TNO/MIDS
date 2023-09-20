/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.compare.output.util;

import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import nl.tno.mids.compare.data.LatticeEdgeAnnotation;
import nl.tno.mids.compare.data.LatticeNode;
import nl.tno.mids.compare.data.Variant;

/**
 * Generate lattice graphs.
 *
 * @apiNote Essentially <a href="https://en.wikipedia.org/wiki/Hasse_diagram">Hasse diagrams (Wikipedia)</a>
 **/
public class LatticeGraphGenerator {
    /**
     * Generate a DOT representation of a given lattice with a given lattice name.
     * 
     * @param <T> Type of element in the variants.
     * @param <D> Type of graph edge payload.
     * @param latticeNodes Lattice nodes to be represented in DOT lattice.
     * @param latticeName Name of the generated lattice.
     * @param latticeNumber 1-based position of the lattice in the overall order. Ignored if zero or negative.
     * @return The generated DOT lattice.
     */
    public static <T, D extends LatticeEdgeAnnotation> String
            generateLattice(List<LatticeNode<Variant<T>>> latticeNodes, String latticeName, int latticeNumber)
    {
        StringJoiner latticeDotContents = new StringJoiner("\n", "", "\n");

        latticeDotContents.add("digraph " + latticeName + "{");
        latticeDotContents.add("rankdir=\"BT\"");

        // Generate lattice nodes.
        for (LatticeNode<Variant<T>> latticeNode: latticeNodes) {
            Variant<T> variant = latticeNode.getValue();
            String style = latticeNode.hasIncompleteLattice() ? ", style=\"dashed,filled\"" : ", style=\"filled\"";
            if (variant.isComputed()) {
                style += ", shape=\"diamond\", fillcolor=\"dimgray\", fontcolor=\"white\"";
            } else {
                style += ", fillcolor=\"lightgray\"";
            }
            String latticeNumberRepresentation = latticeNumber > 0 && hasBehavior(variant)
                    ? ("<sub><font point-size=\"10\">" + latticeNumber + "</font></sub>") : "";
            latticeDotContents
                    .add(variant.getIdentifier() + " [label=<" + TemplateUtils.renderVariant(variant.getIdentifier())
                            + latticeNumberRepresentation + " (" + variant.getSize() + ")" + ">" + style + "]");
        }

        // Generate lattice edges.
        for (LatticeNode<Variant<T>> latticeNode: latticeNodes) {
            Variant<T> variant = latticeNode.getValue();
            for (LatticeNode<Variant<T>> childNode: latticeNode.getChildren().stream()
                    .sorted(Comparator.comparing(LatticeNode<Variant<T>>::getValue)).collect(Collectors.toList()))
            {
                Variant<T> child = childNode.getValue();
                LatticeEdgeAnnotation annotation = childNode.getParentEdgeTo(latticeNode).getAnnotation();
                StringJoiner dataStringJoiner = new StringJoiner(" ");
                if (annotation != null) {
                    if (annotation.getAdded() > 0) {
                        dataStringJoiner.add("<font color=\"limegreen\">+" + annotation.getAdded() + "</font>");
                    }
                    if (annotation.getRemoved() > 0) {
                        dataStringJoiner.add("<font color=\"red\">-" + annotation.getRemoved() + "</font>");
                    }
                    if (annotation.getChanged() > 0) {
                        dataStringJoiner.add("<font color=\"blue\">~" + annotation.getChanged() + "</font>");
                    }
                }
                String annotationText;
                if (dataStringJoiner.length() > 0) {
                    annotationText = dataStringJoiner.toString();
                } else {
                    annotationText = "?";
                }

                latticeDotContents.add(variant.getIdentifier() + " -> " + child.getIdentifier()
                // Surround label with spaces to avoid being close to arrowhead.
                        + " [label=<  " + annotationText + "  >]");
            }
        }

        latticeDotContents.add("}");

        return latticeDotContents.toString();
    }

    /**
     * Check if a given variant has behavior.
     * 
     * <p>
     * Model variants have behavior if they have a non-empty language. Model set variants are considered to always have
     * behavior.
     * <p>
     * 
     * @param variant Variant to check.
     * @return {@code true} if the variant is a model variant with behavior or a model set variant, {@code false}
     *     otherwise.
     */
    private static boolean hasBehavior(Variant<?> variant) {
        // Note that for model set variants, the identifier is always at least one, and thus this method always returns
        // 'true'.
        return variant.getIdentifier() > 0;
    }
}
