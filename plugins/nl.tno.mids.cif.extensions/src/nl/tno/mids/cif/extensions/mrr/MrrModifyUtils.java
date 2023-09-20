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

import java.util.function.Predicate;

import nl.tno.mids.cif.extensions.mrr.data.ConcatenationMRR;
import nl.tno.mids.cif.extensions.mrr.data.LetterMRR;
import nl.tno.mids.cif.extensions.mrr.data.MRR;
import nl.tno.mids.cif.extensions.mrr.data.RepetitionMRR;

public class MrrModifyUtils {
    /**
     * Set the {@link RepetitionMMR#modifiedCount} to the given maxRepeats for each {@link RepetitionMMR} that fulfills
     * the filter predicate.
     *
     * @param mrr The MRR to modify.
     * @param maxRepeats The maximum number of repeats to which a repetition is restricted.
     * @param filter A filtering predicate on the RepetitionMRR that must be true to allow the operation to be applied.
     */
    public static <T> void mrrRestrictMaxRepeat(MRR<T> mrr, int maxRepeats, Predicate<RepetitionMRR<T>> filter) {
        if (mrr instanceof ConcatenationMRR<?>) {
            ConcatenationMRR<T> concat = (ConcatenationMRR<T>)mrr;
            for (MRR<T> item: concat.sequence) {
                mrrRestrictMaxRepeat(item, maxRepeats, filter);
            }
        } else if (mrr instanceof RepetitionMRR<?>) {
            RepetitionMRR<T> repeat = (RepetitionMRR<T>)mrr;
            mrrRestrictMaxRepeat(repeat.getChild(), maxRepeats, filter);
            if (filter.test(repeat)) {
                repeat.setModifiedCount(Integer.min(repeat.getCount(), maxRepeats));
            }
        } else if (mrr instanceof LetterMRR<?>) {
            // Nothing to do here.
        } else {
            throw new RuntimeException("Unexpected MRR: " + mrr.getClass().getName());
        }
    }

    /**
     * Mark the {@link RepetitionMMR} as infinite when the filter predicate is fulfilled.
     *
     * @param mrr The MRR to modify.
     * @param filter A filtering predicate on the RepetitionMRR that must be true to allow the operation to be applied.
     */
    public static <T> void mrrSetInfiniteRepeat(MRR<T> mrr, Predicate<RepetitionMRR<T>> filter) {
        if (mrr instanceof ConcatenationMRR<?>) {
            ConcatenationMRR<T> concat = (ConcatenationMRR<T>)mrr;
            for (MRR<T> item: concat.sequence) {
                mrrSetInfiniteRepeat(item, filter);
            }
        } else if (mrr instanceof RepetitionMRR<?>) {
            RepetitionMRR<T> repeat = (RepetitionMRR<T>)mrr;
            mrrSetInfiniteRepeat(repeat.getChild(), filter);
            if (filter.test(repeat)) {
                repeat.setModifiedCount(null);
            }
        } else if (mrr instanceof LetterMRR<?>) {
            // Nothing to do here.
        } else {
            throw new RuntimeException("Unexpected MRR: " + mrr.getClass().getName());
        }
    }
}
