/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cmi.cmi2yed;

/** Colors to use. */
public enum CmiToYedColors {
    COMP_BG_CLOSED_COLOR("#cccccc"), COMP_BG_OPENED_COLOR("#eeeeee"),

    EVENT_DECL_COLOR("#ffc000"), EVENT_PARAM_COLOR("#ffe0c0"), EVENT_REF_COLOR("#ffc080"),

    DATA_DECL_COLOR("#00c0ff"), DATA_PARAM_COLOR("#c0e0ff"), DATA_REF_COLOR("#80c0ff"),

    COMP_HEADER_COLOR("#000000"), INST_HEADER_COLOR("#ff8000"), DEF_HEADER_COLOR("#40c0ff"),
    WRAP_BOX_HEADER_COLOR("#80ff40"),

    EDGE_LABEL_BG_COLOR("#eeeeeedd"), LOC_BG_COLOR("#ffffff"),

    SERV_FRAG_CLIENT_REQ_HEADER_COLOR("#40bb40"), SERV_FRAG_SERVER_RESP_HEADER_COLOR("#4080cc"),

    COMM_REQUEST_LINK_COLOR("#40bb40"), COMM_RESPONSE_LINK_COLOR("#4080cc"),

    ASYNC_CLIENT_LINK_COLOR("#ff60cc"), ASYNC_SERVER_LINK_COLOR("#ffbb00");

    /** The color code, e.g. "#ff0000" for red. */
    public final String color;

    /**
     * Constructor for the {@link CmiToYedColors} enumeration.
     *
     * @param color The color code, e.g. "#ff0000" for red.
     */
    private CmiToYedColors(String color) {
        this.color = color;
    }
}
