/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cmi.cmi2yed;

import static nl.tno.mids.cmi.cmi2yed.CmiToYedColors.COMP_BG_CLOSED_COLOR;
import static nl.tno.mids.cmi.cmi2yed.CmiToYedColors.COMP_BG_OPENED_COLOR;
import static nl.tno.mids.cmi.cmi2yed.CmiToYedColors.COMP_HEADER_COLOR;
import static nl.tno.mids.cmi.cmi2yed.CmiToYedColors.LOC_BG_COLOR;
import static org.eclipse.escet.common.java.Lists.list;
import static org.eclipse.escet.common.java.Lists.listc;
import static org.eclipse.escet.common.java.Strings.str;

import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.escet.cif.common.CifEdgeUtils;
import org.eclipse.escet.cif.common.CifLocationUtils;
import org.eclipse.escet.cif.common.CifTextUtils;
import org.eclipse.escet.cif.common.CifValueUtils;
import org.eclipse.escet.cif.metamodel.cif.ComplexComponent;
import org.eclipse.escet.cif.metamodel.cif.Component;
import org.eclipse.escet.cif.metamodel.cif.ComponentInst;
import org.eclipse.escet.cif.metamodel.cif.Group;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.escet.common.java.Assert;
import org.eclipse.escet.common.java.Strings;
import org.eclipse.escet.common.position.metamodel.position.PositionObject;
import org.w3c.dom.Element;

import nl.tno.mids.cif.extensions.EdgeExtensions;
import nl.tno.mids.cmi.api.basic.CmiBasicModifications;
import nl.tno.mids.cmi.api.basic.CmiBasicServiceFragmentQueries;
import nl.tno.mids.cmi.api.general.CmiGeneralAsyncPatternQueries;
import nl.tno.mids.cmi.api.general.CmiGeneralComponentQueries;
import nl.tno.mids.cmi.api.general.CmiGeneralDataQueries;
import nl.tno.mids.cmi.api.general.CmiGeneralEventQueries;
import nl.tno.mids.cmi.api.general.CmiGeneralQueries;
import nl.tno.mids.cmi.api.general.CmiSubset;
import nl.tno.mids.cmi.api.info.ComponentInfo;
import nl.tno.mids.cmi.api.info.EventInfo;
import nl.tno.mids.cmi.api.split.CmiSplitComponentQueries;
import nl.tno.mids.cmi.api.split.CmiSplitServiceFragmentQueries;

/** CMI to yEd 'model' diagram transformation. */
public class CmiToYedModelDiagram extends CmiToYedDiagram {
    /** Which subset, as defined by the CMI API, is the specification part of? */
    private CmiSubset subset;

    @Override
    protected void addSpec(Specification spec, Element root) {
        // Split service fragments, if possible.
        if (CmiBasicServiceFragmentQueries.canBeSplitIntoServiceFragments(spec)) {
            CmiBasicModifications.splitServiceFragments(spec);
        }

        // CMI initialization.
        subset = CmiGeneralQueries.detectSubset(spec);

        // Add root 'graph' element.
        rootGraph = doc.createElement("graph");
        root.appendChild(rootGraph);
        rootGraph.setAttribute("id", getId(spec));
        rootGraph.setAttribute("edgedefault", "directed");

        // Add body.
        addCompBody(spec, rootGraph);
    }

    /**
     * Add a component.
     *
     * @param comp The component.
     * @param parent The parent XML element to which to add new elements.
     */
    private void addComp(Component comp, Element parent) {
        if (comp instanceof Group) {
            if (!((Group)comp).getComponents().isEmpty()) {
                addGroup((Group)comp, parent);
            }
        } else if (comp instanceof Automaton) {
            addAutomaton((Automaton)comp, parent);
        } else if (comp instanceof ComponentInst) {
            // Should already have been handled as a special case by
            // 'addCompBody'.
            throw new RuntimeException("Should not get here.");
        } else {
            throw new RuntimeException("Unknown component: " + comp);
        }
    }

    /**
     * Add a group.
     *
     * @param group The group.
     * @param parent The parent XML element to which to add new elements.
     */
    private void addGroup(Group group, Element parent) {
        // Add the group.
        parent = addCompNode(group, parent);

        // Add the body.
        addCompBody(group, parent);
    }

    /**
     * Add an automaton.
     *
     * @param aut The automaton.
     * @param parent The parent XML element to which to add new elements.
     */
    private void addAutomaton(Automaton aut, Element parent) {
        // Add the automaton.
        parent = addCompNode(aut, parent);

        // Add the body.
        addCompBody(aut, parent);
    }

    /**
     * Add a node for a component.
     *
     * @param comp The component. Must be an automaton, a group, or a component definition body. Must not be a component
     *     instantiation.
     * @param parent The parent XML element to which to add new elements.
     * @return The node created for the component.
     */
    private Element addCompNode(Component comp, Element parent) {
        // Determine type of component.
        boolean isAut = comp instanceof Automaton;
        boolean isCmiComponent = CmiGeneralComponentQueries.isComponent(comp);
        boolean isCmiProtocol = isAut && subset == CmiSubset.PROTOCOL;
        boolean isCmiServiceFragment = isAut && subset == CmiSubset.SPLIT;
        boolean isGroup = comp instanceof Group;
        Assert.check(isAut || isGroup);
        int cmiTypeCount = 0;
        if (isCmiComponent) {
            cmiTypeCount++;
        }
        if (isCmiProtocol) {
            cmiTypeCount++;
        }
        if (isCmiServiceFragment) {
            cmiTypeCount++;
        }
        Assert.check(cmiTypeCount <= 1);

        // CMI: determine whether this is an initial service fragment automaton: a
        // service fragment handling a request
        // from a component that is not in the same CMI specification (or from itself).
        boolean isInitialAut = false;
        if (isCmiServiceFragment) {
            Event event = CmiSplitServiceFragmentQueries.getServiceFragmentEvent((Automaton)comp);
            EventInfo eventInfo = CmiGeneralEventQueries.getEventInfo(event, subset);
            Specification model = CmiGeneralQueries.getModel(event);
            isInitialAut = eventInfo.otherCompInfo == null || CmiSplitComponentQueries.getComponentsWithBehavior(model)
                    .stream().noneMatch(c -> c.getName().equals(eventInfo.declCompInfo.toString()));
        }

        // Add GraphML 'node'.
        Element nodeElem = doc.createElement("node");
        parent.appendChild(nodeElem);
        String id = getId(comp);
        nodeElem.setAttribute("id", id);
        parent = nodeElem;

        // Add yEd styles.
        nodeElem.setAttribute("yfiles.foldertype", isAut ? "folder" : "group");

        Element dnDataElem = doc.createElement("data");
        nodeElem.appendChild(dnDataElem);
        dnDataElem.setAttribute("key", "dn");

        Element ngDataElem = doc.createElement("data");
        nodeElem.appendChild(ngDataElem);
        ngDataElem.setAttribute("key", "ng");

        Element pabnElem = doc.createElement("y:ProxyAutoBoundsNode");
        ngDataElem.appendChild(pabnElem);

        Element realElem = doc.createElement("y:Realizers");
        pabnElem.appendChild(realElem);
        realElem.setAttribute("active", isAut ? "1" : "0");

        String kindText;
        if (isCmiServiceFragment) {
            kindText = "Serv. frag. ";
        } else if (isCmiComponent) {
            kindText = "Runtime component ";
        } else { // Protocols don't need prefix (its in their names). Wrapper groups etc also don't need a prefix.
            kindText = "";
        }

        String title;
        CmiToYedColors bgColor;
        title = kindText + comp.getName();
        if (isCmiServiceFragment) {
            if (CmiSplitServiceFragmentQueries.isClientRequestServiceFragment((Automaton)comp)) {
                bgColor = CmiToYedColors.SERV_FRAG_CLIENT_REQ_HEADER_COLOR;
            } else if (CmiSplitServiceFragmentQueries.isServerResponseServiceFragment((Automaton)comp)) {
                bgColor = CmiToYedColors.SERV_FRAG_SERVER_RESP_HEADER_COLOR;
            } else if (CmiSplitServiceFragmentQueries.isInternalServiceFragment((Automaton)comp)) {
                bgColor = COMP_HEADER_COLOR;
            } else {
                throw new RuntimeException("Unknown type of service fragment: " + comp);
            }
        } else {
            bgColor = COMP_HEADER_COLOR;
        }

        dnDataElem.setTextContent(title);

        for (boolean closed: list(false, true)) {
            Element gnElem = doc.createElement("y:GroupNode");
            realElem.appendChild(gnElem);

            if (closed) {
                Element geoElem = doc.createElement("y:Geometry");
                gnElem.appendChild(geoElem);
                Rectangle2D size = guessTextSize(title, 5);
                double width = size.getWidth() + 40;
                double height = size.getHeight();
                geoElem.setAttribute("width", str(width));
                geoElem.setAttribute("height", str(height));
            }

            String label = title;
            label = Strings.spaces(6) + label; // Avoid label behind '-' icon.
            // label = highlight(label);

            Element nlElem = doc.createElement("y:NodeLabel");
            gnElem.appendChild(nlElem);
            nlElem.setAttribute("alignment", "left");
            nlElem.setAttribute("autoSizePolicy", "node_width");
            nlElem.setAttribute("backgroundColor", bgColor.color);
            nlElem.setAttribute("textColor", "#ffffff");
            nlElem.setAttribute("modelName", "internal");
            nlElem.setAttribute("modelPosition", "t");
            nlElem.setAttribute("fontStyle", isGroup ? "bolditalic" : "bold");
            nlElem.setTextContent(label);

            Element fillElem = doc.createElement("y:Fill");
            gnElem.appendChild(fillElem);
            CmiToYedColors fillColor = closed ? COMP_BG_CLOSED_COLOR : COMP_BG_OPENED_COLOR;
            fillElem.setAttribute("color", fillColor.color);

            if (isInitialAut) { // CMI: initial service fragments thicker border.
                Element borderElem = doc.createElement("y:BorderStyle");
                gnElem.appendChild(borderElem);
                borderElem.setAttribute("width", "3.0");
            }

            Element shapeElem = doc.createElement("y:Shape");
            gnElem.appendChild(shapeElem);
            shapeElem.setAttribute("type", "rectangle");

            Element stateElem = doc.createElement("y:State");
            gnElem.appendChild(stateElem);
            stateElem.setAttribute("closed", str(closed));
        }

        // Add 'graph' element.
        Element graphElem = doc.createElement("graph");
        parent.appendChild(graphElem);
        graphElem.setAttribute("id", id + ":");
        graphElem.setAttribute("edgedefault", "directed");
        parent = graphElem;
        return parent;
    }

    /**
     * Add a component body.
     *
     * @param comp The component.
     * @param parent The parent XML element to which to add new elements.
     */
    private void addCompBody(ComplexComponent comp, Element parent) {
        // Add child components.
        if (comp instanceof Group) {
            Assert.check(((Group)comp).getDefinitions().isEmpty());

            // Get child components.
            List<ComplexComponent> others = list();
            for (Component child: ((Group)comp).getComponents()) {
                boolean isAut = child instanceof Automaton;
                boolean isGroup = child instanceof Group;
                Assert.check(isAut || isGroup);
                others.add((ComplexComponent)child);
            }

            // Add other child components, separately.
            for (ComplexComponent child: others) {
                addComp(child, parent);
            }
        }

        // Add locations with their outgoing edges.
        if (comp instanceof Automaton) {
            Automaton aut = (Automaton)comp;
            for (int i = 0; i < aut.getLocations().size(); i++) {
                Location loc = aut.getLocations().get(i);
                addLocation(loc, i, parent);
            }
        }
    }

    /**
     * Add the locations and its outgoing edges.
     *
     * @param loc The location.
     * @param idx The 0-based index of the location in the automaton.
     * @param parent The parent XML element to which to add new elements.
     */
    private void addLocation(Location loc, int idx, Element parent) {
        // Add GraphML 'node'.
        Element nodeElem = doc.createElement("node");
        parent.appendChild(nodeElem);
        String parentId = parent.getAttribute("id");
        Assert.check(parentId.endsWith(":"));
        String locId = parentId + ":loc" + str(idx);
        nodeElem.setAttribute("id", locId);

        // Get description.
        String description = "location";
        if (loc.getName() != null) {
            description += " " + loc.getName();
        }

        String label = "";
        if (loc.getName() != null) {
            label = loc.getName();
        }

        // Add yEd styles.
        Element dnDataElem = doc.createElement("data");
        nodeElem.appendChild(dnDataElem);
        dnDataElem.setAttribute("key", "dn");
        dnDataElem.setTextContent(description);

        Element ngDataElem = doc.createElement("data");
        nodeElem.appendChild(ngDataElem);
        ngDataElem.setAttribute("key", "ng");

        Element snElem;
        if (loc.getMarkeds().isEmpty() || CifValueUtils.isTriviallyFalse(loc.getMarkeds(), false, true)) {
            snElem = doc.createElement("y:ShapeNode");
            ngDataElem.appendChild(snElem);

            Element shapeElem = doc.createElement("y:Shape");
            snElem.appendChild(shapeElem);
            shapeElem.setAttribute("type", "roundrectangle");
        } else {
            Assert.check(CifValueUtils.isTriviallyTrue(loc.getMarkeds(), false, true),
                    "Only locations that are trivially marked or trivially non-marked are supported: "
                            + CifTextUtils.getAbsName(loc));

            snElem = doc.createElement("y:GenericNode");
            ngDataElem.appendChild(snElem);
            snElem.setAttribute("configuration", "com.yworks.bpmn.Activity.withShadow");

            Element styleElem = doc.createElement("y:StyleProperties");
            snElem.appendChild(styleElem);

            Element awtColorProp = doc.createElement("y:Property");
            styleElem.appendChild(awtColorProp);
            awtColorProp.setAttribute("class", "java.awt.Color");
            awtColorProp.setAttribute("name", "com.yworks.bpmn.icon.line.color");
            awtColorProp.setAttribute("value", "#000000");

            Element bpmnTaskTypeProp = doc.createElement("y:Property");
            styleElem.appendChild(bpmnTaskTypeProp);
            bpmnTaskTypeProp.setAttribute("class", "com.yworks.yfiles.bpmn.view.TaskTypeEnum");
            bpmnTaskTypeProp.setAttribute("name", "com.yworks.bpmn.taskType");
            bpmnTaskTypeProp.setAttribute("value", "TASK_TYPE_ABSTRACT");

            Element iconFillColorProp = doc.createElement("y:Property");
            styleElem.appendChild(iconFillColorProp);
            iconFillColorProp.setAttribute("class", "java.awt.Color");
            iconFillColorProp.setAttribute("name", "com.yworks.bpmn.icon.fill");
            iconFillColorProp.setAttribute("value", "#ffffffe6");

            Element iconFill2ColorProp = doc.createElement("y:Property");
            styleElem.appendChild(iconFill2ColorProp);
            iconFill2ColorProp.setAttribute("class", "java.awt.Color");
            iconFill2ColorProp.setAttribute("name", "com.yworks.bpmn.icon.fill2");
            iconFill2ColorProp.setAttribute("value", "#d4d4d4cc");

            Element bpmnTypeProp = doc.createElement("y:Property");
            bpmnTypeProp.setAttribute("class", "com.yworks.yfiles.bpmn.view.BPMNTypeEnum");
            bpmnTypeProp.setAttribute("name", "com.yworks.bpmn.type");
            bpmnTypeProp.setAttribute("value", "ACTIVITY_TYPE");

            Element activityTypeProp = doc.createElement("y:Property");
            styleElem.appendChild(activityTypeProp);
            activityTypeProp.setAttribute("class", "com.yworks.yfiles.bpmn.view.ActivityTypeEnum");
            activityTypeProp.setAttribute("name", "com.yworks.bpmn.activityType");
            activityTypeProp.setAttribute("value", "ACTIVITY_TYPE_TRANSACTION");
        }

        Element geoElem = doc.createElement("y:Geometry");
        snElem.appendChild(geoElem);
        Rectangle2D size = guessTextSize(label, 5);
        double width = size.getWidth() + 30;
        double height = size.getHeight();
        geoElem.setAttribute("width", str(width));
        geoElem.setAttribute("height", str(height));

        label = escapeHTML(label);

        Element nlElem = doc.createElement("y:NodeLabel");
        snElem.appendChild(nlElem);
        nlElem.setAttribute("alignment", "left");
        nlElem.setAttribute("autoSizePolicy", "content");
        nlElem.setAttribute("modelName", "internal");
        nlElem.setAttribute("modelPosition", "c");
        nlElem.setTextContent(label);

        Element fillElem = doc.createElement("y:Fill");
        snElem.appendChild(fillElem);
        fillElem.setAttribute("color", LOC_BG_COLOR.color);

        // Add initialization.
        addLocInit(loc, locId, parent);

        // Add outgoing edges.
        for (Edge edge: loc.getEdges()) {
            // Get target location node id.
            Automaton aut = (Automaton)loc.eContainer();
            Location tgtLoc = CifEdgeUtils.getTarget(edge);
            int tgtIdx = aut.getLocations().indexOf(tgtLoc);
            String tgtId = parentId + (parentId.endsWith(":") ? "" : ":") + ":loc" + str(tgtIdx);

            // CMI: additional relation arrows, if model has service fragments.
            Event event = EdgeExtensions.getEventDecl(edge, true);
            if (event != null && subset == CmiSubset.SPLIT) {
                EventInfo eventInfo = CmiGeneralEventQueries.getEventInfo(event, subset);

                // CMI communication, visualized as arrows. For both models with and without
                // service fragments.
                // Represented in CIF using non-directional synchronizing events, but for CMI it
                // is directional.
                Group edgeComponent = CmiSplitComponentQueries.getComponent(aut);
                Group eventComponent = CmiSplitComponentQueries.getComponent(event);
                if (edgeComponent != eventComponent && eventInfo.otherCompInfo != null) {
                    // Event defined in other component and event communicates. There are only 2 components involved,
                    // as event naming scheme includes source and target component identities.
                    // Events are defined in component that initiates the communication. So, examine all edges of the
                    // component that defines the event.
                    for (Automaton candidateServiceFrag: CmiSplitServiceFragmentQueries
                            .getServiceFragments(eventComponent))
                    {
                        for (Location candidateSourceLoc: candidateServiceFrag.getLocations()) {
                            for (Edge candidateEdge: candidateSourceLoc.getEdges()) {
                                if (event == EdgeExtensions.getEventDecl(candidateEdge, true)) {
                                    // Matching synchronizing edge found. Arrow starts at source location of edge in
                                    // automaton that starts the communication, and ends at source location of edge
                                    // in automaton that receives the communication.

                                    String linkSrcId = "::loc" + candidateServiceFrag.getLocations()
                                            .indexOf(CifEdgeUtils.getSource(candidateEdge));
                                    linkSrcId = getId(candidateServiceFrag) + linkSrcId;

                                    // Determine link color.
                                    String linkLabelPrefix;
                                    CmiToYedColors linkColor;
                                    if (CmiGeneralEventQueries.isRequestEvent(eventInfo)) {
                                        linkColor = CmiToYedColors.COMM_REQUEST_LINK_COLOR;
                                        linkLabelPrefix = "request";
                                    } else if (CmiGeneralEventQueries.isResponseEvent(eventInfo)) {
                                        linkColor = CmiToYedColors.COMM_RESPONSE_LINK_COLOR;
                                        linkLabelPrefix = "response";
                                    } else {
                                        throw new RuntimeException("Event must be request or response: " + eventInfo);
                                    }

                                    // Determine link label.
                                    String linkLabel = Strings.fmt("<html><div color=\"gray\">[%s]</div>%s</html>",
                                            linkLabelPrefix, getEventDescription(eventInfo));

                                    // Add link.
                                    addCmiLink(linkLabel, linkColor, linkSrcId, locId, rootGraph);
                                }
                            }
                        }
                    }
                }

                // CMI asynchronous pattern constraints.
                if (CmiGeneralAsyncPatternQueries.isAsyncPatternStart(edge)) {
                    // Start of constraint found. Get matching end edges.
                    Set<Edge> endEdges = CmiGeneralAsyncPatternQueries.getAsyncPatternEnds(edge);

                    // Process constraint edge pairs.
                    for (Edge endEdge: endEdges) {
                        // Arrow starts at source location of constraint start edge and ends at source
                        // location of constraint end edge.
                        Location endSource = CifEdgeUtils.getSource(endEdge);
                        Automaton endServiceFragment = CifLocationUtils.getAutomaton(endSource);
                        int endLinkIdx = endServiceFragment.getLocations().indexOf(endSource);
                        String endLinkId = getId(endServiceFragment) + "::loc" + endLinkIdx;

                        // Determine link color.
                        Event endEvent = EdgeExtensions.getEventDecl(endEdge, false);
                        EventInfo endEventInfo = CmiGeneralEventQueries.getEventInfo(endEvent, subset);
                        ComplexComponent endComponent = CmiSplitComponentQueries.getComponent(endServiceFragment);
                        ComponentInfo endComponentInfo = CmiGeneralComponentQueries.getComponentInfo(endComponent);
                        ComponentInfo endEventComponentInfo = endEventInfo.declCompInfo;
                        CmiToYedColors linkColor = endComponentInfo.equals(endEventComponentInfo)
                                ? CmiToYedColors.ASYNC_SERVER_LINK_COLOR // Source of end event (response) is this
                                                                         // component -> this is the server.
                                : CmiToYedColors.ASYNC_CLIENT_LINK_COLOR;// Source of end event (response) is other
                                                                         // component -> this is the client.

                        // Determine link label.
                        String linkLabel = Strings.fmt(
                                "<html><div color=\"gray\">[request]</div>%s<div color=\"gray\">[response]</div>%s</html>",
                                getEventDescription(eventInfo), getEventDescription(endEventInfo));

                        // Add link.
                        addCmiLink(linkLabel, linkColor, locId, endLinkId, rootGraph);
                    }
                }
            }

            // Add edge
            addEdge(edge, locId, tgtId, parent);
        }
    }

    /**
     * Add initialization of a location.
     *
     * @param loc The location.
     * @param locId The 'id' of the node of the location.
     * @param parent The parent XML element to which to add new elements.
     */
    private void addLocInit(Location loc, String locId, Element parent) {
        // Skip if not initial location.
        List<Expression> inits = loc.getInitials();
        boolean hasInit = !inits.isEmpty() && !CifValueUtils.isTriviallyFalse(inits, true, true);
        if (!hasInit)
            return;

        Assert.check(CifValueUtils.isTriviallyTrue(inits, true, true), "Unsupported inits expression.");

        // Add GraphML 'node'.
        Element initNodeElem = doc.createElement("node");
        parent.appendChild(initNodeElem);
        String initId = locId + "::init";
        initNodeElem.setAttribute("id", initId);

        // Add yEd styles.
        Element dnDataElem = doc.createElement("data");
        initNodeElem.appendChild(dnDataElem);
        dnDataElem.setAttribute("key", "dn");
        dnDataElem.setTextContent("<init>");

        Element initNgDataElem = doc.createElement("data");
        initNodeElem.appendChild(initNgDataElem);
        initNgDataElem.setAttribute("key", "ng");

        Element initSnElem = doc.createElement("y:ShapeNode");
        initNgDataElem.appendChild(initSnElem);

        Element initGeoElem = doc.createElement("y:Geometry");
        initSnElem.appendChild(initGeoElem);
        initGeoElem.setAttribute("width", "1");
        initGeoElem.setAttribute("height", "1");

        Element initBsElem = doc.createElement("y:BorderStyle");
        initSnElem.appendChild(initBsElem);
        initBsElem.setAttribute("hasColor", "false");

        Element initFillElem = doc.createElement("y:Fill");
        initSnElem.appendChild(initFillElem);
        initFillElem.setAttribute("transparent", "true");

        // Add GraphML 'edge'. Note that according to the GraphML
        // specification: "The edges between two nodes in a nested graph
        // have to be declared in a graph, which is an ancestor of both
        // nodes in the hierarchy. [...] A good policy is to place the
        // edges at the least common ancestor of the nodes in the
        // hierarchy, or at the top level."
        Element initEdgeElem = doc.createElement("edge");
        parent.appendChild(initEdgeElem);
        initEdgeElem.setAttribute("source", initId);
        initEdgeElem.setAttribute("target", locId);

        // Add yEd styles.
        Element initEdgeEgDataElem = doc.createElement("data");
        initEdgeElem.appendChild(initEdgeEgDataElem);
        initEdgeEgDataElem.setAttribute("key", "eg");

        Element initEdgePleElem = doc.createElement("y:PolyLineEdge");
        initEdgeEgDataElem.appendChild(initEdgePleElem);

        Element initEdgeArrElem = doc.createElement("y:Arrows");
        initEdgePleElem.appendChild(initEdgeArrElem);
        initEdgeArrElem.setAttribute("source", "none");
        initEdgeArrElem.setAttribute("target", "arrow");

        Element initEdgeBsElem = doc.createElement("y:BendStyle");
        initEdgePleElem.appendChild(initEdgeBsElem);
        initEdgeBsElem.setAttribute("smoothed", "true");
    }

    /**
     * Add an edge.
     *
     * @param edge The edge. May be a 'tau' reference.
     * @param src The 'id' of the source location node.
     * @param tgt The 'id' of the target location node.
     * @param parent The parent XML element to which to add new elements.
     */
    private void addEdge(Edge edge, String src, String tgt, Element parent) {
        // Add GraphML 'edge'. Note that according to the GraphML
        // specification: "The edges between two nodes in a nested graph have
        // to be declared in a graph, which is an ancestor of both nodes in the
        // hierarchy. [...] A good policy is to place the edges at the least
        // common ancestor of the nodes in the hierarchy, or at the top level."
        Element edgeElem = doc.createElement("edge");
        parent.appendChild(edgeElem);
        edgeElem.setAttribute("source", src);
        edgeElem.setAttribute("target", tgt);

        // Add yEd styles.
        Element edgeEgDataElem = doc.createElement("data");
        edgeElem.appendChild(edgeEgDataElem);
        edgeEgDataElem.setAttribute("key", "eg");

        Element edgePleElem = doc.createElement("y:PolyLineEdge");
        edgeEgDataElem.appendChild(edgePleElem);

        Element edgeArrElem = doc.createElement("y:Arrows");
        edgePleElem.appendChild(edgeArrElem);
        edgeArrElem.setAttribute("source", "none");
        edgeArrElem.setAttribute("target", "arrow");

        Element edgeBsElem = doc.createElement("y:BendStyle");
        edgePleElem.appendChild(edgeBsElem);
        edgeBsElem.setAttribute("smoothed", "true");

        // Initialize label texts. Optimized for pure event-based models.
        List<String> texts = listc(1);

        // CMI: omitting guards, updates, urgency. Use CMI API to query relevant
        // information instead.

        // Add events label text.
        List<Event> events = EdgeExtensions.getEventDecls(edge, true);

        texts.addAll(events.stream()
                .map(e -> e == null ? "tau" : getEventDescription(CmiGeneralEventQueries.getEventInfo(e, subset)))
                .collect(Collectors.toSet()));

        // If edge is the start of any repetition, add the repetition count to its label
        if (CmiGeneralDataQueries.isDataRepetitionStart(edge)) {
            texts.add(Strings.fmt("<strong>(%d times)</strong>", CmiGeneralDataQueries.getDataRepetitionCount(edge)));
        }

        // Get full label text.
        String label = Strings.fmt("<html>%s</html>", StringUtils.join(texts, "<br>"));

        // Add edge label.
        Element edgeLblElem = doc.createElement("y:EdgeLabel");
        edgePleElem.appendChild(edgeLblElem);
        edgeLblElem.setAttribute("alignment", "center");
        addEdgeLabelBackground(edgeLblElem);

        edgeLblElem.setTextContent(label);
    }

    /**
     * Gives an HTML description of the given event information.
     * 
     * @param info The event information for which an HTML description should be provided.
     * @return A HTML description for the given event information.
     */
    private String getEventDescription(EventInfo info) {
        StringBuilder sb = new StringBuilder();
        sb.append(info.interfaceName);
        sb.append(":");
        sb.append(info.functionName);

        if (info.asyncDirection != null) {
            sb.append(" <span color=\"gray\">");
            sb.append(info.asyncDirection.getName());
            sb.append("</span>");
        }
        sb.append("<br>");

        if (info.otherCompInfo != null) {
            sb.append("from ");
        } else {
            sb.append("on ");
        }
        sb.append(info.declCompInfo + " ");
        sb.append(info.declType.getName() + " ");
        sb.append(info.declSide.getFullPostfix());
        if (info.otherCompInfo != null) {
            sb.append("<br>");
            sb.append("to ");
            sb.append(info.otherCompInfo + " ");
            sb.append(info.otherType.getName() + " ");
            sb.append(info.otherSide.getFullPostfix());
        }
        return sb.toString();
    }

    /**
     * Add a CMI link.
     *
     * @param text The label of the link.
     * @param color The color of the link.
     * @param src The 'id' of the source location node.
     * @param tgt The 'id' of the target location node.
     * @param parent The parent XML element to which to add new elements.
     */
    private void addCmiLink(String text, CmiToYedColors color, String src, String tgt, Element parent) {
        // Add GraphML 'edge'. Note that according to the GraphML
        // specification: "The edges between two nodes in a nested graph have
        // to be declared in a graph, which is an ancestor of both nodes in the
        // hierarchy. [...] A good policy is to place the edges at the least
        // common ancestor of the nodes in the hierarchy, or at the top level."
        Element edgeElem = doc.createElement("edge");
        parent.appendChild(edgeElem);
        edgeElem.setAttribute("source", src);
        edgeElem.setAttribute("target", tgt);

        // Add yEd styles.
        Element edgeEgDataElem = doc.createElement("data");
        edgeElem.appendChild(edgeEgDataElem);
        edgeEgDataElem.setAttribute("key", "eg");

        Element edgePleElem = doc.createElement("y:PolyLineEdge");
        edgeEgDataElem.appendChild(edgePleElem);

        Element edgeLsElem = doc.createElement("y:LineStyle");
        edgePleElem.appendChild(edgeLsElem);
        edgeLsElem.setAttribute("type", "dashed");
        edgeLsElem.setAttribute("color", color.color);
        edgeLsElem.setAttribute("width", "3.0");

        Element edgeArrElem = doc.createElement("y:Arrows");
        edgePleElem.appendChild(edgeArrElem);
        edgeArrElem.setAttribute("source", "none");
        edgeArrElem.setAttribute("target", "arrow");

        Element edgeBsElem = doc.createElement("y:BendStyle");
        edgePleElem.appendChild(edgeBsElem);
        edgeBsElem.setAttribute("smoothed", "true");

        // Add edge label.
        Element edgeLblElem = doc.createElement("y:EdgeLabel");
        edgePleElem.appendChild(edgeLblElem);
        edgeLblElem.setAttribute("alignment", "center");
        addEdgeLabelBackground(edgeLblElem);

        edgeLblElem.setTextContent(text);
    }

    /**
     * Returns the unique diagram node id to use for a object.
     *
     * @param obj The CIF object. Must be a {@link CifTextUtils#getName named object}.
     * @return The unique diagram node id.
     */
    private static String getId(PositionObject obj) {
        String name = CifTextUtils.getAbsName(obj, false);
        return "cif:" + name.replace('.', ':');
    }
}
