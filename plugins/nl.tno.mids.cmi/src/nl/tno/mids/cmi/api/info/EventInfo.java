/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cmi.api.info;

import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.common.java.Assert;

import nl.tno.mids.pps.extensions.info.EventFunctionExecutionType;

/**
 * Container for event information.
 */
public class EventInfo {
    /**
     * Information about the component declaring the event. Represents one of the following:
     * <ul>
     * <li>The component on which the internal event occurs.</li>
     * <li>The source of a communication, in case of synchronous component composition.</li>
     * <li>The sending or receiving side of the communication (matching {@link #asyncDirection}), in case of
     * asynchronous component composition.</li>
     * </ul>
     */
    public final ComponentInfo declCompInfo;

    /**
     * Direction of communication for event in case of asynchronous component composition. {@code null} in case of
     * synchronous component composition.
     */
    public final EventAsyncDirection asyncDirection;

    /** Name of the interface. */
    public final String interfaceName;

    /** Name of the function. */
    public final String functionName;

    /**
     * Represents the type of the function execution on the component described by {@link #declCompInfo}.
     */
    public final EventFunctionExecutionType declType;

    /**
     * Represents the function execution side on the component described by {@link #declCompInfo}.
     */
    public final EventFunctionExecutionSide declSide;

    /**
     * Represents the type of the function execution on the component described by {@link #otherCompInfo}, or
     * {@code null} if {@link #otherCompInfo} is {@code null}.
     */
    public final EventFunctionExecutionType otherType;

    /**
     * Represents the function execution side on the component described by {@link #otherCompInfo}, or {@code null} if
     * {@link #otherCompInfo} is {@code null}.
     */
    public final EventFunctionExecutionSide otherSide;

    /**
     * Information about the component at the other side of the communication:
     * <ul>
     * <li>{@code null} for internal events.</li>
     * <li>The target of the communication, in case of synchronous component composition.</li>
     * <li>The opposite sending or receiving side of the communication (i.e. the opposite side of
     * {@link #asyncDirection}), in case of asynchronous component composition.</li>
     * </ul>
     */
    public final ComponentInfo otherCompInfo;

    public EventInfo(ComponentInfo declCompInfo, EventAsyncDirection asyncDirection, String interfaceName,
            String functionName, EventFunctionExecutionType declType, EventFunctionExecutionSide declSide,
            EventFunctionExecutionType otherType, EventFunctionExecutionSide otherSide, ComponentInfo otherCompInfo)
    {
        // Ensure interface name does not contain a double underscore. If condition is violated, distinguishing the
        // interface name from the function name is infeasible.
        Assert.check(!interfaceName.contains("__"));

        // Ensure function name does not contain a double underscore. If condition is violated, distinguishing the
        // function name from the postfix is infeasible.
        Assert.check(!functionName.contains("__"));

        // Check invariants.
        Assert.ifAndOnlyIf(otherCompInfo == null, otherType == null);
        Assert.ifAndOnlyIf(otherCompInfo == null, otherSide == null);
        Assert.implies(asyncDirection == EventAsyncDirection.INTERNAL, otherCompInfo == null);
        Assert.implies(asyncDirection == EventAsyncDirection.SEND || asyncDirection == EventAsyncDirection.RECEIVE,
                otherCompInfo != null);

        this.declCompInfo = declCompInfo;
        this.asyncDirection = asyncDirection;
        this.interfaceName = interfaceName;
        this.functionName = functionName;
        this.declType = declType;
        this.declSide = declSide;
        this.otherType = otherType;
        this.otherSide = otherSide;
        this.otherCompInfo = otherCompInfo;
    }

    /**
     * Returns a new instance, with the provided value for {@link #declCompInfo}.
     * 
     * @param declCompInfo The new value for {@link #declCompInfo}.
     * @return The new instance.
     */
    public EventInfo withDeclCompInfo(ComponentInfo declCompInfo) {
        return new EventInfo(declCompInfo, this.asyncDirection, this.interfaceName, this.functionName, this.declType,
                this.declSide, this.otherType, this.otherSide, this.otherCompInfo);
    }

    /**
     * Returns a new instance, with the provided values for {@link #declCompInfo}, {@link #declType} and
     * {@link #declSide}.
     * 
     * @param declCompInfo The new value for {@link #declCompInfo}.
     * @param declType The new value for {@link @declType}.
     * @param declSide the new value for {@link @declSide}.
     * @return The new instance.
     */
    public EventInfo withDeclCompInfo(ComponentInfo declCompInfo, EventFunctionExecutionType declType,
            EventFunctionExecutionSide declSide)
    {
        return new EventInfo(declCompInfo, this.asyncDirection, this.interfaceName, this.functionName, declType,
                declSide, this.otherType, this.otherSide, this.otherCompInfo);
    }

    /**
     * Returns a new instance, with the provided value for {@link #otherCompInfo}.
     * 
     * @param otherCompInfo The new value for {@link #otherCompInfo}.
     * @return The new instance.
     */
    public EventInfo withOtherCompInfo(ComponentInfo otherCompInfo) {
        return new EventInfo(this.declCompInfo, this.asyncDirection, this.interfaceName, this.functionName,
                this.declType, this.declSide, this.otherType, this.otherSide, otherCompInfo);
    }

    /**
     * Returns a new instance, with the provided values for {@link #otherType}, {@link #otherSide} and
     * {@link #otherCompInfo}.
     * 
     * @param otherType The new value for {@link #otherType}.
     * @param otherSide The new value for {@link #otherSide}.
     * @param otherCompInfo The new value for {@link #otherCompInfo}.
     * @return The new instance.
     */
    public EventInfo withOtherCompInfo(EventFunctionExecutionType otherType, EventFunctionExecutionSide otherSide,
            ComponentInfo otherCompInfo)
    {
        return new EventInfo(this.declCompInfo, this.asyncDirection, this.interfaceName, this.functionName,
                this.declType, this.declSide, otherType, otherSide, otherCompInfo);
    }

    /**
     * @return Name of the {@link Event}.
     */
    public String getEventName() {
        StringBuilder builder = new StringBuilder();
        if (asyncDirection != null) {
            builder.append(asyncDirection.getPrefix());
        }
        builder.append(interfaceName);
        builder.append("__");
        builder.append(functionName);
        builder.append('_');
        builder.append(declType.getPostfix());
        builder.append(declSide.getPostfix());
        if (otherCompInfo != null) {
            builder.append(otherType.getPostfix());
            builder.append(otherSide.getPostfix());
            builder.append("__");
            builder.append(otherCompInfo);
        }
        return builder.toString();
    }

    /**
     * @return Absolute name of the {@link Event}.
     */
    public String getAbsEventName() {
        StringBuilder builder = new StringBuilder();
        builder.append(declCompInfo);
        builder.append('.');
        builder.append(getEventName());
        return builder.toString();
    }

    @Override
    public String toString() {
        return getAbsEventName();
    }
}
