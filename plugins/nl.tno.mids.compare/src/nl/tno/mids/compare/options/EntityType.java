/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.compare.options;

import java.util.Objects;

import org.eclipse.escet.common.java.Strings;

/** Types of entity represented in model instances. */
public class EntityType {
    private final String name;

    private final String capitalizedName;

    private final String plural;

    private final String capitalizedPlural;

    /**
     * Construct instance with provided name, and a plural created by appending an 's'.
     * 
     * @param name Name of the entity type.
     */
    public EntityType(String name) {
        this.name = name;
        this.capitalizedName = Strings.makeInitialUppercase(name);
        this.plural = name + "s";
        this.capitalizedPlural = Strings.makeInitialUppercase(plural);
    }

    /**
     * Construct instance with provided name and plural.
     * 
     * @param name Name of the entity type.
     * @param plural Plural version of the entity type.
     */
    public EntityType(String name, String plural) {
        this.name = name;
        this.capitalizedName = Strings.makeInitialUppercase(name);
        this.plural = plural;
        this.capitalizedPlural = Strings.makeInitialUppercase(plural);
    }

    /**
     * @return The name of this entity type.
     */
    public String getName() {
        return name;
    }

    /**
     * @return The name of this entity type, starting with a capital letter.
     */
    public String getCapitalizedName() {
        return capitalizedName;
    }

    /**
     * @return The plural name of this entity type.
     */
    public String getPlural() {
        return plural;
    }

    /**
     * @return The plural name of this entity type, starting with a capital letter.
     */
    public String getCapitalizedPlural() {
        return capitalizedPlural;
    }

    @Override
    public int hashCode() {
        return Objects.hash(capitalizedName, capitalizedPlural, name, plural);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof EntityType)) {
            return false;
        }
        EntityType other = (EntityType)obj;
        return Objects.equals(capitalizedName, other.capitalizedName)
                && Objects.equals(capitalizedPlural, other.capitalizedPlural) && Objects.equals(name, other.name)
                && Objects.equals(plural, other.plural);
    }
}
