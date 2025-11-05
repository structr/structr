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
package org.structr.rest.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.core.entity.Relation;
import org.structr.core.property.ISO8601DateProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.rest.entity.LogEvent;
import org.structr.rest.traits.wrappers.LogEventTraitWrapper;

import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 *
 *
 */
public class LogEventTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String MESSAGE_PROPERTY   = "message";
	public static final String ACTION_PROPERTY    = "action";
	public static final String TIMESTAMP_PROPERTY = "timestamp";
	public static final String SUBJECT_PROPERTY   = "subject";
	public static final String OBJECT_PROPERTY    = "object";

	public LogEventTraitDefinition() {
		super(StructrTraits.LOG_EVENT);
	}

	@Override
	public Map<Class, LifecycleMethod> createLifecycleMethods(TraitsInstance traitsInstance) {
		return Map.of();
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {
		return Map.of();
	}

	@Override
	public Map<Class, RelationshipTraitFactory> getRelationshipTraitFactories() {
		return Map.of();
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			LogEvent.class, (traits, node) -> new LogEventTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<String> messageProperty   = new StringProperty(MESSAGE_PROPERTY);
		final Property<String> actionProperty    = new StringProperty(ACTION_PROPERTY).indexed();
		final Property<Date>   timestampProperty = new ISO8601DateProperty(TIMESTAMP_PROPERTY).indexed();
		final Property<String> subjectProperty   = new StringProperty(SUBJECT_PROPERTY).indexed();
		final Property<String> objectProperty    = new StringProperty(OBJECT_PROPERTY).indexed();

		return newSet(
			messageProperty,
			actionProperty,
			timestampProperty,
			subjectProperty,
			objectProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
				ACTION_PROPERTY, MESSAGE_PROPERTY, TIMESTAMP_PROPERTY, SUBJECT_PROPERTY, OBJECT_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}