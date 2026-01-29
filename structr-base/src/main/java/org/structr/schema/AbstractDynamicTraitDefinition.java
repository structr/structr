/*
 * Copyright (C) 2010-2026 Structr GmbH
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
import org.structr.common.SecurityContext;
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
import org.structr.core.traits.*;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.LifecycleMethodAdapter;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.core.traits.operations.graphobject.OnCreation;

import java.util.*;

public abstract class AbstractDynamicTraitDefinition<T extends AbstractSchemaNode> implements TraitDefinition {

	protected final Map<Class, LifecycleMethod> lifecycleMethods = new LinkedHashMap<>();
	protected final Map<Class, FrameworkMethod> frameworkMethods = new LinkedHashMap<>();
	protected final Set<AbstractMethod> dynamicMethods           = new LinkedHashSet<>();
	protected final Map<String, Set<String>> views               = new LinkedHashMap<>();
	protected final Set<PropertyKey> propertyKeys                = new LinkedHashSet<>();
	protected final String label;
	protected final String name;

	public AbstractDynamicTraitDefinition(final TraitsInstance traitsInstance, final T schemaNode) {

		this.label = schemaNode.getClassName();
		this.name  = this.label + "." + schemaNode.getUuid();

		initializeLifecycleMethods(schemaNode);
		initializeFrameworkMethods(schemaNode);
		initializeDynamicMethods(schemaNode);
		initializePropertyKeys(traitsInstance, schemaNode);
		initializeViews(schemaNode);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public Map<Class, LifecycleMethod> createLifecycleMethods(TraitsInstance traitsInstance) {
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
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {
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

		if (schemaNode.isServiceClass()) {

			lifecycleMethods.put(
					OnCreation.class,
					new OnCreation() {
						@Override
						public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

							throw new FrameworkException(422, "Cannot instantiate service class");
						}
					}
			);
		}

		// collect methods
		for (final SchemaMethod method : schemaNode.getSchemaMethods()) {

			if (method.isLifecycleMethod()) {

				final Class<LifecycleMethod> type = method.getMethodType();
				if (type != null) {

					if (lifecycleMethods.containsKey(type)) {

						// more than one implementation on a single type!
						final LifecycleMethod existingMethod = lifecycleMethods.get(type);
						if (existingMethod instanceof LifecycleMethodAdapter adapter) {

							adapter.addMethod(method);
						} else {

							throw new RuntimeException("Unexpected lifecycle method " + method.getName() + ", expected LifecycleMethodAdapter!");
						}

					} else {

						lifecycleMethods.put(type, method.asLifecycleMethod());
					}
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

			final String sortOrder = view.getSortOrder();
			if (sortOrder != null) {

				applySortOrder(names, sortOrder);
			}

			views.put(view.getName(), names);
		}
	}

	protected void initializePropertyKeys(final TraitsInstance traitsInstance, final T schemaNode) {

		final String className = schemaNode.getClassName();

		for (final SchemaProperty property : schemaNode.getSchemaProperties()) {

			try {

				final PropertyKey key = property.createKey(className);
				if (key != null) {

					propertyKeys.add(key);
				}

			} catch (FrameworkException e) {
				e.printStackTrace();
			}
		}
	}

	// ----- private methods -----
	private void sortProperties(final Set<String> names, final String sortOrder) {

		if (StringUtils.isNotBlank(sortOrder)) {

			final String[] parts = sortOrder.split(",");

			for (final String part : parts) {

				final String trimmed = part.trim();

				if (StringUtils.isNotBlank(trimmed)) {


				}
			}

		}
	}

	private void applySortOrder(final Set<String> view, final String orderString) {

		final List<String> list = new LinkedList<>();

		if ("alphabetic".equals(orderString)) {

			// copy elements to list for sorting
			list.addAll(view);

			// sort alphabetically
			Collections.sort(list);

		} else {

			// sort according to comma-separated list of property names
			final String[] order = orderString.split("[, ]+");
			for (final String property : order) {

				if (StringUtils.isNotEmpty(property.trim())) {

					// SchemaProperty instances are suffixed with "Property"
					final String suffixedProperty = property + "Property";

					if (view.contains(property)) {

						// move property from view to list
						list.add(property);
						view.remove(property);

					} else if (view.contains(suffixedProperty)) {

						// move property from view to list
						list.add(suffixedProperty);
						view.remove(suffixedProperty);
					}

				}
			}

			// append the rest
			list.addAll(view);
		}

		// clear source view, add sorted list contents
		view.clear();
		view.addAll(list);
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
