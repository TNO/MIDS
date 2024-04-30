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

import com.google.common.base.Preconditions;

import nl.tno.mids.cif.extensions.mrr.MrrConfig;

/** Repetition {@link MRR}. */
public class RepetitionMRR<T> extends MRR<T> {
    /** Child MRR. */
    private final MRR<T> child;

    /** The original repetition count (>= 2). */
    private int count;

    /**
     * The modified repetition count (>= 1), or {@code null} for infinitely many.
     */
    private Integer modifiedCount;

    /** The repetition cost for the original repetition count. */
    private int repetitionCost;

    public RepetitionMRR(MRR<T> child, int count, MrrConfig<T> config) {
        this.child = child;
        this.count = count;
        this.modifiedCount = count;
        this.repetitionCost = config.getRepetitionCost(count);
        Preconditions.checkArgument(count >= 2);
    }

    public MRR<T> getChild() {
        return child;
    }

    public int getCount() {
        return count;
    }

    public Integer getModifiedCount() {
        return modifiedCount;
    }

    public void setModifiedCount(Integer count) {
        this.modifiedCount = count;
        Preconditions.checkArgument(count == null || count >= 1);
    }

    @Override
    public int getCost() {
        return repetitionCost + child.getCost();
    }

    @Override
    public int getDomainSize() {
        return child.getDomainSize() * count;
    }

    @Override
    public String toSingleLineString() {
        StringBuilder txt = new StringBuilder();
        txt.append('(');
        txt.append(child.toSingleLineString());
        txt.append(")^");
        txt.append(count);
        return txt.toString();
    }

    @Override
    public String toMultiLineString() {
        StringBuilder txt = new StringBuilder();
        txt.append("|\\ " + count + " repeats\n");
        String childTxt = child.toMultiLineString();
        for (String line: childTxt.split("\n")) {
            txt.append("| ");
            txt.append(line);
            txt.append('\n');
        }
        txt.append("|/ ");
        txt.append(child.getDomainSize() * count);
        txt.append(" domain letters covered");
        return txt.toString();
    }
}
