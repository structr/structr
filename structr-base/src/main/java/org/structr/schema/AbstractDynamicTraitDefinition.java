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
package org.structr.schema;

import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.ScriptMethod;
import org.structr.core.entity.*;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.definitions.TraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.IsValid;

import java.util.*;

public abstract class AbstractDynamicTraitDefinition<T extends AbstractSchemaNode> implements TraitDefinition {

	protected final T schemaNode;

	public AbstractDynamicTraitDefinition(final T schemaNode) {
		this.schemaNode = schemaNode;
	}

	@Override
	public String getName() {
		return schemaNode.getName();
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		final Validator validator = new Validator();

		// collect validators
		for (final SchemaProperty property : schemaNode.getSchemaProperties()) {

			try {

				validator.addValidatorsOrNull(property.createValidators(schemaNode));

			} catch (FrameworkException e) {
				e.printStackTrace();
			}
		}

		return Map.of(
			IsValid.class, validator
		);
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
		return Map.of();
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {

		final Set<AbstractMethod> methods = new LinkedHashSet<>();

		for (final SchemaMethod method : schemaNode.getSchemaMethods()) {

			methods.add(new ScriptMethod(method));
		}

		return methods;
	}

	@Override
	public Map<String, Set<String>> getViews() {

		final Map<String, Set<String>> views = new LinkedHashMap<>();

		for (final SchemaView view : schemaNode.getSchemaViews()) {

			final Set<String> names = new LinkedHashSet<>();

			for (final SchemaProperty property : view.getSchemaProperties()) {

				names.add(property.getName());
			}

			final String ngp = view.getNonGraphProperties();
			if (StringUtils.isNotBlank(ngp)) {

				for (final String name : ngp.split(",")) {

					final String trimmed = name.trim();

					if (StringUtils.isNotBlank(trimmed)) {

						names.add(trimmed);
					}
				}
			}

			views.put(view.getName(), names);
		}

		return views;
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Set<PropertyKey> keys = new LinkedHashSet<>();

		for (final SchemaProperty property : schemaNode.getSchemaProperties()) {

			try {

				keys.add(property.createKey(schemaNode));

			} catch (FrameworkException e) {
				e.printStackTrace();
			}
		}

		return keys;
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	// ----- static classes -----
	private static class Validator implements IsValid {

		private final List<IsValid> validators = new LinkedList<>();

		@Override
		public Boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer) {

			boolean isValid = true;

			for (final IsValid validator : validators) {

				isValid &= validator.isValid(obj, errorBuffer);
			}

			return isValid;
		}

		public void addValidatorsOrNull(final List<IsValid> validators) {

			if (validators != null) {

				for (final IsValid v : validators) {

					if (v != null) {
						validators.add(v);
					}
				}
			}
		}
	}
}
