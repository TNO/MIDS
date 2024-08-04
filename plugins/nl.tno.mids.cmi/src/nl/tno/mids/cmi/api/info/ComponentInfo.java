/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cmi.api.info;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.escet.common.java.Assert;

import nl.tno.mids.cmi.api.protocol.CmiProtocolQueries;
import nl.tno.mids.pps.extensions.info.EventFunctionExecutionType;

/** Container for component information. */
public class ComponentInfo {
    /** The default postfix for the names of components that are untraced. */
    private static final String COMPONENT_UNTRACED_POSTFIX = "_untraced";

    /** A regex for obtaining a name and unique variant number out of a (possibly postfixed) component name. */
    private static final Pattern COMPONENT_NAME_PATTERN = Pattern.compile("^(?<name>.*?)(_(?<id>\\d+))?(_untraced)?$");

    /** The base name of the component, without postfixes. */
    public final String name;

    /** The variant of the component, or {@code null} if only one variant. */
    public final Integer variant;

    /** Whether the component is traced ({@code true}) or untraced ({@code false}). */
    public final boolean traced;

    /**
     * Instantiate component information based on an existing component name.
     * 
     * @param componentName The component name.
     */
    public ComponentInfo(String componentName) {
        this(getName(componentName), getVariant(componentName), isUntraced(componentName));
        Assert.check(this.toString().equals(componentName));
    }

    /**
     * Instantiate component information based on provided properties.
     * 
     * @param name The base name of the component, without postfixes.
     * @param variant The variant of the component, or {@code null} if only one variant.
     * @param traced Whether the component is traced ({@code true}) or untraced ({@code false}).
     */
    public ComponentInfo(String name, Integer variant, boolean traced) {
        // Ensure name does not end with one of the component postfixes. If condition is violated, distinguishing the
        // name from a postfix is infeasible.
        Assert.check(getVariant(name) == null);
        Assert.check(!name.endsWith(COMPONENT_UNTRACED_POSTFIX));
        Assert.check(!name.contains("__"), "Component name " + name + " contains double underscore.");

        // Ensure name does not start with a function execution side postfix. If condition is violated, distinguishing a
        // postfix from a component name is infeasible at the end of an event name.
        Assert.check(EventFunctionExecutionSide.detectPrefix(name) == EventFunctionExecutionSide.START);

        // Ensure name does not contain a function execution type postfix. If condition is violated, distinguishing a
        // postfix from a component name is infeasible at the end of an event name.
        Assert.check(EventFunctionExecutionType.containsPostfix(name) == null);

        // Ensure name is not a protocol rather than a component.
        Assert.check(!CmiProtocolQueries.isProtocolName(name));

        this.name = name;
        this.variant = variant;
        this.traced = traced;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ComponentInfo)) {
            return false;
        }
        ComponentInfo other = (ComponentInfo)obj;
        return this.name.equals(other.name) && this.variant == other.variant && this.traced == other.traced;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, variant, traced);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(name);
        if (variant != null) {
            builder.append("_");
            builder.append(variant);
        }
        if (!traced) {
            builder.append(COMPONENT_UNTRACED_POSTFIX);
        }
        return builder.toString();
    }

    private static String getName(String componentName) {
        Matcher matcher = ComponentInfo.COMPONENT_NAME_PATTERN.matcher(componentName);
        Assert.check(matcher.matches(), componentName + " does not match expected pattern.");

        return matcher.group("name");
    }

    private static Integer getVariant(String componentName) {
        Matcher matcher = ComponentInfo.COMPONENT_NAME_PATTERN.matcher(componentName);
        Assert.check(matcher.matches());

        if (matcher.group("id") != null) {
            return Integer.parseInt(matcher.group("id"));
        }
        return null;
    }

    private static boolean isUntraced(String componentName) {
        return !componentName.endsWith(COMPONENT_UNTRACED_POSTFIX);
    }
}
