/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.test.rest.traits.definitions.relationships;

import org.structr.common.PropertyView;
import org.structr.core.entity.Relation;
import org.structr.core.property.*;
import org.structr.core.traits.definitions.AbstractRelationshipTraitDefinition;
import org.structr.core.traits.definitions.RelationshipBaseTraitDefinition;
import org.structr.test.rest.common.TestEnum;

import java.util.Map;
import java.util.Set;

/**
 *
 *
 */
public class TwoOneOneToMany extends AbstractRelationshipTraitDefinition implements RelationshipBaseTraitDefinition { //OneToMany<TestTwo, TestOne> {

	public TwoOneOneToMany() {
		super("TwoOneOneToMany");
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<String>   startNodeId         = new StringProperty("startNodeId");
		final Property<String>   endNodeId           = new StringProperty("endNodeId");
		final Property<String[]> stringArrayProperty = new ArrayProperty<>("stringArrayProperty", String.class);
		final Property<Boolean>  booleanProperty     = new BooleanProperty("booleanProperty").indexed();
		final Property<Double>   doubleProperty      = new DoubleProperty("doubleProperty").indexed();
		final Property<Integer>  integerProperty     = new IntProperty("integerProperty").indexed();
		final Property<Long>     longProperty        = new LongProperty("longProperty").indexed();
		final Property<String>   stringProperty      = new StringProperty("stringProperty").indexed();
		final Property<String> enumProperty          = new EnumProperty("enumProperty", TestEnum.class).indexed();

		return Set.of(
			startNodeId, endNodeId, stringArrayProperty, booleanProperty, doubleProperty, integerProperty, longProperty, stringProperty, enumProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			Set.of("startNodeId", "endNodeId", "stringArrayProperty", "booleanProperty", "doubleProperty", "integerProperty", "longProperty", "stringProperty", "enumProperty")
		);
	}

	@Override
	public String getSourceType() {
		return "TestTwo";
	}

	@Override
	public String getTargetType() {
		return "TestOne";
	}

	@Override
	public String getRelationshipType() {
		return "OWNS";
	}

	/*
	@Override
	public Property<String> getSourceIdProperty() {
		return startNodeId;
	}

	@Override
	public Property<String> getTargetIdProperty() {
		return endNodeId;
	}
	*/

	@Override
	public Relation.Multiplicity getSourceMultiplicity() {
		return Relation.Multiplicity.One;
	}

	@Override
	public Relation.Multiplicity getTargetMultiplicity() {
		return Relation.Multiplicity.Many;
	}

	@Override
	public int getCascadingDeleteFlag() {
		return Relation.NONE;
	}

	@Override
	public int getAutocreationFlag() {
		return Relation.NONE;
	}

	@Override
	public boolean isInternal() {
		return false;
	}
}
