/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////
package nl.tno.mids.cmi.postprocessing

import com.google.common.base.Preconditions
import net.automatalib.automata.fsa.impl.compact.CompactDFA
import nl.tno.mids.automatalib.extensions.cif.AutomataLibToCif
import nl.tno.mids.cmi.postprocessing.status.PostProcessingStatus

class PostProcessingModelCompactDfa extends PostProcessingModel {
    val CompactDFA<String> dfa

    new(CompactDFA<String> dfa, String name, PostProcessingStatus status) {
        super(name, status)
        this.dfa = dfa

        Preconditions.checkArgument(status.dataIsNotPresent)
    }

    override getCifSpec() {
        return AutomataLibToCif.fsaToCifSpecification(dfa, name, true)
    }

    override getCompactDfa() {
        Preconditions.checkState(status.dataIsNotPresent)

        return dfa
    }

    override recategorizeAsNoData() {
        throw new UnsupportedOperationException(
            "Can't re-categorize. Must already have no data, as CompactDFA cannot represent data.")
    }

    override recategorizeAsNoTau() {
        Preconditions.checkArgument(status.tauIsPresent)
        return new PostProcessingModelCompactDfa(dfa, name, new PostProcessingStatus(status.dataIsPresent, false))
    }
}
