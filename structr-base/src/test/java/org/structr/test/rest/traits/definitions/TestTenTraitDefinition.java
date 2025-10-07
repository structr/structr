/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.test.rest.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.notion.PropertySetNotion;
import org.structr.core.property.EndNode;
import org.structr.core.property.FunctionProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;

import java.util.Map;
import java.util.Set;

public class TestTenTraitDefinition extends AbstractNodeTraitDefinition {

	public TestTenTraitDefinition() {
		super("TestTen");
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<NodeInterface> testSeven = new EndNode(traitsInstance, "testSeven", "TenSevenOneToOne", new PropertySetNotion<>(true, newSet("id", "aString")));
		final Property<Object> functionTest     = new FunctionProperty<>("functionTest").readFunction("{ return ({ name: 'test', value: 123, me: Structr.this }); }");
		final Property<Object> getNameProperty 	= new FunctionProperty<>("getNameProperty").readFunction("{ return Structr.this.name; }").cachingEnabled(true);
		final Property<Object> getRandomNumProp	= new FunctionProperty<>("getRandomNumProp").readFunction("{ return Math.random()*10000; }").cachingEnabled(true);

		return newSet(
			testSeven,
			functionTest,
			getNameProperty,
			getRandomNumProp
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(NodeInterfaceTraitDefinition.NAME_PROPERTY, "testSeven", "functionTest", "getNameProperty", "getRandomNumProp")
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
