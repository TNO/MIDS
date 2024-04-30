/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cmi.api.general;

import nl.tno.mids.cmi.api.basic.CmiBasicServiceFragmentQueries;
import nl.tno.mids.cmi.api.protocol.CmiProtocolQueries;
import nl.tno.mids.cmi.api.split.CmiSplitServiceFragmentQueries;

/**
 * Enumeration describing CMI subsets.
 */
public enum CmiSubset {
    /**
     * Basic CMI subset, with component models.
     * 
     * @see CmiBasicServiceFragmentQueries#isBasicCmiModelWithNoSplitServiceFragments
     */
    BASIC,

    /**
     * Protocol CMI subset, with protocol models.
     * 
     * @see CmiProtocolQueries#isProtocolCmiModel
     * @see CmiProtocolQueries#isProtocol
     */
    PROTOCOL,

    /**
     * Split service fragments CMI subset, with separate service fragment automata.
     * 
     * @see CmiSplitServiceFragmentQueries#isSplitCmiModelWithOnlySplitServiceFragments
     */
    SPLIT;
}
