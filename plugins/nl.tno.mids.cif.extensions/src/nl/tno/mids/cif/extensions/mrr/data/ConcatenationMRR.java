/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cif.extensions.mrr.data;

import static org.eclipse.escet.common.java.Lists.list;

import java.util.List;
import java.util.stream.Collectors;

/** Concatenation {@link MRR}. */
public class ConcatenationMRR<T> extends MRR<T> {
    public final List<MRR<T>> sequence;

    public ConcatenationMRR(List<MRR<T>> sequence) {
        List<MRR<T>> expandedSequence = list();
        for (MRR<T> item: sequence) {
            if (item instanceof ConcatenationMRR<?>) {
                // Invariant: item.sequence is already expanded
                expandedSequence.addAll(((ConcatenationMRR<T>)item).sequence);
            } else {
                expandedSequence.add(item);
            }
        }
        this.sequence = expandedSequence;
    }

    @Override
    public int getCost() {
        return sequence.stream().collect(Collectors.summingInt(m -> m.getCost()));
    }

    @Override
    public int getDomainSize() {
        return sequence.stream().collect(Collectors.summingInt(m -> m.getDomainSize()));
    }

    @Override
    public String toSingleLineString() {
        return sequence.stream().map(i -> i.toSingleLineString()).collect(Collectors.joining(" "));
    }

    @Override
    public String toMultiLineString() {
        StringBuilder txt = new StringBuilder();
        for (MRR<T> item: sequence) {
            String itemTxt = item.toMultiLineString();
            if (txt.length() > 0) {
                txt.append("\n");
            }
            txt.append(itemTxt);
        }
        return txt.toString();
    }
}
