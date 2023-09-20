/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cif.extensions.mrr;

import static org.eclipse.escet.common.java.Lists.list;
import static org.eclipse.escet.common.java.Lists.listc;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

import nl.tno.mids.cif.extensions.mrr.data.ConcatenationMRR;
import nl.tno.mids.cif.extensions.mrr.data.LetterMRR;
import nl.tno.mids.cif.extensions.mrr.data.MRR;
import nl.tno.mids.cif.extensions.mrr.data.MrrWithWord;
import nl.tno.mids.cif.extensions.mrr.data.RepetitionMRR;

/**
 * Compute minimal repetition representation for a string/word.
 *
 * <p>
 * Implementation based on the following paper: Atsuyoshi Nakamura, Tomoya Saito, Ichigaku Takigawa, Mineichi Kudo, and
 * Hiroshi Mamitsuka, "Fast algorithms for finding a minimum repetition representation of strings and trees", in
 * Discrete Applied Mathematics 161, pages 1556-1575, Elsevier, 2013.
 * </p>
 */
public class ComputeMRR {
    private static final boolean DEBUG = false;

    public static <T> MrrWithWord<T> computeMRR(List<T> domainWord, MrrConfig<T> config, IProgressMonitor monitor) {
        // Implements CMR algorithm from Figure 1 of the paper.
        SubMonitor subMonitor = SubMonitor.convert(monitor, domainWord.size() + 2);
        subMonitor.split(1);

        // Construct MRR word from domain word.
        List<LetterMRR<T>> word = listc(domainWord.size());
        for (T domainLetter: domainWord) {
            word.add(new LetterMRR<>(domainLetter, config));
        }

        // Declarations.
        int n = word.size();
        int[][] l = new int[n][n];
        NodeType[][] type1 = new NodeType[n][n];
        int[][] type2 = new int[n][n];

        // Initialization. Lines 1 + 2 from Figure 1.
        for (int i = 0; i < n; i++) {
            l[i][i] = word.get(i).getCost();
            for (int j = i + 1; j < n; j++) {
                l[i][j] = Integer.MAX_VALUE;
            }
        }

        // Algorithm loops. Lines 3-19 from Figure 1.
        for (int z = 1; z <= n; z++) { // Length of considered segment.
            subMonitor.split(1);
            for (int i = 0; i <= n - z; i++) { // Index of considered segment.
                for (int d = 0; d <= z - 2; d++) { // Length of considered split for cheapest concatenate (nesting RR).
                    int consider = l[i][i + d] + l[i + d + 1][i + z - 1];
                    if (consider < l[i][i + z - 1]) {
                        l[i][i + z - 1] = consider;
                        type1[i][i + z - 1] = NodeType.CONCATENATE;
                        type2[i][i + z - 1] = d;
                    }
                }
                for (int ii = i + z; ii <= n - z; ii += z) { // Index of start next candidate for finding repetitions.
                    if (areDifferent(word, ii, ii + z - 1, i, i + z - 1)) {
                        break;
                    }
                    int consider = l[i][i + z - 1] + config.getRepetitionCost((ii - i) / z + 1);
                    if (consider < l[i][ii + z - 1]) {
                        l[i][ii + z - 1] = consider;
                        type1[i][ii + z - 1] = NodeType.REPEAT;
                        type2[i][ii + z - 1] = z;
                    }
                }
            }
        }

        // Debug output.
        subMonitor.split(1);
        if (DEBUG) {
            System.out.println("l=");
            for (int i = 0; i < n; i++) {
                System.out.println(Arrays.toString(l[i]));
            }
            System.out.println();

            System.out.println("type1=");
            for (int i = 0; i < n; i++) {
                System.out.println(Arrays.toString(type1[i]));
            }
            System.out.println();

            System.out.println("type2=");
            for (int i = 0; i < n; i++) {
                System.out.println(Arrays.toString(type2[i]));
            }
            System.out.println();
        }

        // Construct MRR. Line 20 from Figure 1.
        MRR<T> mrr = constructMRR(word, type1, type2, config);
        return new MrrWithWord<T>(word, mrr);
    }

    private static <T> boolean areDifferent(List<LetterMRR<T>> word, int lower1, int upper1, int lower2, int upper2) {
        int x1, x2;
        for (x1 = lower1, x2 = lower2; x1 <= upper1 || x2 <= upper2; x1++, x2++) {
            if (word.get(x1).letterRepr != word.get(x2).letterRepr) {
                return true;
            }
        }
        return false;
    }

    private static <T> MRR<T> constructMRR(List<LetterMRR<T>> word, NodeType[][] type1, int[][] type2,
            MrrConfig<T> config)
    {
        return constructMRR(word, 0, word.size() - 1, type1, type2, config);
    }

    private static <T> MRR<T> constructMRR(List<LetterMRR<T>> word, int i, int j, NodeType[][] type1, int[][] type2,
            MrrConfig<T> config)
    {
        // Implements 'constructMRRs' from bottom of page 1560 of paper.
        if (i == j) {
            return word.get(i);
        }

        switch (type1[i][j]) {
            case REPEAT: {
                int z = type2[i][j];
                MRR<T> child = constructMRR(word, i, i + z - 1, type1, type2, config);
                int h = (j - i + 1) / z;
                return new RepetitionMRR<T>(child, h, config);
            }
            case CONCATENATE: {
                int d = type2[i][j];
                MRR<T> child1 = constructMRR(word, i, i + d, type1, type2, config);
                MRR<T> child2 = constructMRR(word, i + d + 1, j, type1, type2, config);
                return new ConcatenationMRR<T>(list(child1, child2));
            }
            default:
                throw new RuntimeException("Unknown node type: " + type1[i][j]);
        }
    }

    private enum NodeType {
        CONCATENATE, // type(2, ...)
        REPEAT; // type(1, ...)
    }
}
