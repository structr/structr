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
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.ScriptMethod;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.entity.SchemaView;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.TraitDefinition;
import org.structr.core.traits.Traits;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.IsValid;

import java.util.*;

public abstract class AbstractDynamicTraitDefinition<T extends AbstractSchemaNode> implements TraitDefinition {

	protected final Map<Class, LifecycleMethod> lifecycleMethods = new LinkedHashMap<>();
	protected final Map<Class, FrameworkMethod> frameworkMethods = new LinkedHashMap<>();
	protected final Set<AbstractMethod> dynamicMethods           = new LinkedHashSet<>();
	protected final Map<String, Set<String>> views               = new LinkedHashMap<>();
	protected final Set<PropertyKey> propertyKeys                = new LinkedHashSet<>();
	protected final String name;

	public AbstractDynamicTraitDefinition(final T schemaNode) {

		this.name = schemaNode.getClassName();

		initializeLifecycleMethods(schemaNode);
		initializeFrameworkMethods(schemaNode);
		initializeDynamicMethods(schemaNode);
		initializePropertyKeys(schemaNode);
		initializeViews(schemaNode);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {
		return lifecycleMethods;
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {
		return frameworkMethods;
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
		return dynamicMethods;
	}

	@Override
	public Map<String, Set<String>> getViews() {
		return views;
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {
		return propertyKeys;
	}

	// ----- protected methods -----
	protected void initializeFrameworkMethods(final T schemaNode) {
	}

	protected void initializeLifecycleMethods(final T schemaNode) {

		final Set<String> compoundKeys = new LinkedHashSet<>();
		final Validator validator      = new Validator();

		// collect validators
		for (final SchemaProperty property : schemaNode.getSchemaProperties()) {

			try {

				validator.addValidatorsOrNull(property.createValidators(schemaNode));

				if (property.isCompound()) {
					compoundKeys.add(property.getName());
				}

			} catch (FrameworkException e) {
				e.printStackTrace();
			}
		}

		if (!compoundKeys.isEmpty()) {

			validator.addValidatorsOrNull(List.of(

				new IsValid() {
					@Override
					public Boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer) {

						final Traits traits         = obj.getTraits();
						final Set<PropertyKey> keys = new LinkedHashSet<>();

						for (final String compoundKey : compoundKeys) {
							keys.add(traits.key(compoundKey));
						}

						return ValidationHelper.areValidCompoundUniqueProperties(obj, errorBuffer, keys);
					}
				}
			));
		}

		if (validator.hasValidators()) {
			lifecycleMethods.put(IsValid.class, validator);
		}

		// collect methods
		for (final SchemaMethod method : schemaNode.getSchemaMethods()) {

			if (method.isLifecycleMethod()) {

				final Class<LifecycleMethod> type = method.getMethodType();
				if (type != null) {

					lifecycleMethods.put(type, method.asLifecycleMethod());
				}
			}
		}
	}

	protected void initializeDynamicMethods(final T schemaNode) {

		for (final SchemaMethod method : schemaNode.getSchemaMethods()) {

			// dynamic methods can be lifecycle methods which are handled elsewhere
			if (!method.isLifecycleMethod()) {

				dynamicMethods.add(new ScriptMethod(method));
			}
		}
	}

	protected void initializeViews(final T schemaNode) {


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
	}

	protected void initializePropertyKeys(final T schemaNode) {

		for (final SchemaProperty property : schemaNode.getSchemaProperties()) {

			try {

				propertyKeys.add(property.createKey(schemaNode.getClassName()));

			} catch (FrameworkException e) {
				e.printStackTrace();
			}
		}
	}

	// ----- static classes -----
	private static class Validator implements IsValid {

		private final List<IsValid> validators = new LinkedList<>();

		public boolean hasValidators() {
			return !validators.isEmpty();
		}

		@Override
		public Boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer) {

			boolean isValid = true;

			for (final IsValid validator : validators) {

				isValid &= validator.isValid(obj, errorBuffer);
			}

			return isValid;
		}

		public void addValidatorsOrNull(final List<IsValid> input) {

			if (input != null) {

				for (final IsValid v : input) {

					if (v != null) {

						this.validators.add(v);
					}
				}
			}
		}
	}
}
