/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.compare.data;

/**
 * @param <T> Type of value represented by this variant.
 */
public class Variant<T> implements Comparable<Variant<T>> {
    /** Identifier that uniquely identifies this variant among other variants. */
    final int identifier;

    /** The representative value for this variant. */
    final T value;

    /** The size of this variant. */
    final int size;

    /***
     * Whether this variant was generated by the compare tool ({@code true}) or is an input variant ({@code false}).
     */
    final boolean computed;

    /**
     * @param identifier Unique identifier of this variant.
     * @param value Item represented by this variant.
     * @param size Size of item represented by this variant.
     * @param computed Whether the variant was generated by the compare tool ({@code true}) or is an input variant
     *     ({@code false}).
     */
    public Variant(int identifier, T value, int size, boolean computed) {
        this.identifier = identifier;
        this.value = value;
        this.size = size;
        this.computed = computed;
    }

    /**
     * @return The identifier that uniquely identifies this variant among other variants.
     */
    public int getIdentifier() {
        return identifier;
    }

    /**
     * @return The representative value for this variant.
     */
    public T getValue() {
        return value;
    }

    /**
     * @return The size of the representative value of this variant.
     */
    public int getSize() {
        return size;
    }

    /**
     * @return Whether this variant has been generated by the compare tool ({@code true}) or is an input variant
     *     ({@code false}).
     */
    public boolean isComputed() {
        return computed;
    }

    /**
     * Compare this variant to another variant.
     * 
     * @param otherVariant {@link Variant} to compare to.
     * @return Compare result as defined for {@link Comparable}.
     */
    @Override
    public int compareTo(Variant<T> otherVariant) {
        return Integer.compare(getIdentifier(), otherVariant.getIdentifier());
    }
}
