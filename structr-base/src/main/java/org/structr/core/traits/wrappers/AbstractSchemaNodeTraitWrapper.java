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
package org.structr.core.traits.wrappers;

import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.*;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.PropertyInfo;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.TypeInfo;
import org.structr.core.traits.definitions.AbstractSchemaNodeTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.SchemaNodeTraitDefinition;

import java.util.*;

/**
 *
 *
 */
public class AbstractSchemaNodeTraitWrapper extends AbstractNodeTraitWrapper implements AbstractSchemaNode {

	public AbstractSchemaNodeTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public String getName() {
		return wrappedObject.getProperty(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY));
	}

	@Override
	public Iterable<SchemaProperty> getSchemaProperties() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key(AbstractSchemaNodeTraitDefinition.SCHEMA_PROPERTIES_PROPERTY);

		return Iterables.map(n -> n.as(SchemaProperty.class), wrappedObject.getProperty(key));
	}

	@Override
	public Iterable<SchemaView> getSchemaViews() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key(AbstractSchemaNodeTraitDefinition.SCHEMA_VIEWS_PROPERTY);

		return Iterables.map(n -> n.as(SchemaView.class), wrappedObject.getProperty(key));
	}

	@Override
	public Iterable<SchemaMethod> getSchemaMethods() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key(AbstractSchemaNodeTraitDefinition.SCHEMA_METHODS_PROPERTY);

		return Iterables.map(n -> n.as(SchemaMethod.class), wrappedObject.getProperty(key));
	}

	@Override
	public Iterable<SchemaGrant> getSchemaGrants() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key(SchemaNodeTraitDefinition.SCHEMA_GRANTS_PROPERTY);

		return Iterables.map(n -> n.as(SchemaGrant.class), wrappedObject.getProperty(key));
	}

	@Override
	public Iterable<SchemaMethod> getSchemaMethodsIncludingInheritance() {

		List<SchemaMethod> methods = Iterables.toList(getSchemaMethods());
		NodeInterface parentNode    = wrappedObject.getProperty(traits.key("extendsClass"));

		if (parentNode != null) {

			final AbstractSchemaNode asn = parentNode.as(AbstractSchemaNode.class);

			for (final SchemaMethod m : asn.getSchemaMethodsIncludingInheritance()) {

				if (!methods.contains(m)) {

					methods.add(m);
				}
			}
		}

		return methods;
	}

	@Override
	public SchemaMethod getSchemaMethod(final String name) {

		for (final SchemaMethod method : getSchemaMethods()) {

			if (name.equals(method.getName())) {
				return method;
			}
		}

		return null;
	}

	@Override
	public List<SchemaMethod> getSchemaMethodsByName(final String name) {

		final List<SchemaMethod> result = new ArrayList<>();

		for (final SchemaMethod method : getSchemaMethods()) {

			if (name.equals(method.getName())) {
				result.add(method);
			}
		}

		return result;
	}

	@Override
	public SchemaProperty getSchemaProperty(final String name) {

		for (final SchemaProperty property : getSchemaProperties()) {

			if (name.equals(property.getName())) {
				return property;
			}
		}

		return null;
	}

	@Override
	public SchemaView getSchemaView(final String name) {

		for (final SchemaView view : getSchemaViews()) {

			if (name.equals(view.getName())) {
				return view;
			}
		}

		return null;
	}

	@Override
	public String getSummary() {
		return wrappedObject.getProperty(traits.key(AbstractSchemaNodeTraitDefinition.SUMMARY_PROPERTY));
	}

	@Override
	public String getIcon() {
		return wrappedObject.getProperty(traits.key(AbstractSchemaNodeTraitDefinition.ICON_PROPERTY));
	}

	@Override
	public String getDescription() {
		return wrappedObject.getProperty(traits.key(AbstractSchemaNodeTraitDefinition.DESCRIPTION_PROPERTY));
	}

	@Override
	public String getCategory() {
		return wrappedObject.getProperty(traits.key(SchemaNodeTraitDefinition.CATEGORY_PROPERTY));
	}

	@Override
	public String getClassName() {
		return getName();
	}

	@Override
	public String getDefaultSortKey() {
		return wrappedObject.getProperty(traits.key(SchemaNodeTraitDefinition.DEFAULT_SORT_KEY_PROPERTY));
	}

	@Override
	public String getDefaultSortOrder() {
		return wrappedObject.getProperty(traits.key(SchemaNodeTraitDefinition.DEFAULT_SORT_ORDER_PROPERTY));
	}

	@Override
	public boolean isInterface() {
		return wrappedObject.getProperty(traits.key(SchemaNodeTraitDefinition.IS_INTERFACE_PROPERTY));
	}

	@Override
	public boolean isAbstract() {
		return wrappedObject.getProperty(traits.key(SchemaNodeTraitDefinition.IS_ABSTRACT_PROPERTY));
	}

	@Override
	public boolean isServiceClass() {
		return wrappedObject.getProperty(traits.key(AbstractSchemaNodeTraitDefinition.IS_SERVICE_CLASS_PROPERTY));
	}

	@Override
	public boolean changelogDisabled() {
		return wrappedObject.getProperty(traits.key(AbstractSchemaNodeTraitDefinition.CHANGELOG_DISABLED_PROPERTY));
	}

	@Override
	public boolean includeInOpenAPI() {
		return wrappedObject.getProperty(traits.key(AbstractSchemaNodeTraitDefinition.INCLUDE_IN_OPEN_API_PROPERTY));
	}

	@Override
	public String[] getTags() {
		return wrappedObject.getProperty(traits.key(AbstractSchemaNodeTraitDefinition.TAGS_PROPERTY));
	}

	@Override
	public Set<String> getViewNames() {

		final Set<String> viewNames = new LinkedHashSet<>();

		for (final SchemaView view : getSchemaViews()) {

			viewNames.add(view.getName());
		}

		return viewNames;
	}

	@Override
	public void checkInheritanceConstraints() throws FrameworkException {

		if (this.is(StructrTraits.SCHEMA_NODE)) {

			final App app                = StructrApp.getInstance();
			final Set<String> traitNames = this.as(SchemaNode.class).getInheritedTraits();
			final Set<TypeInfo> types    = new LinkedHashSet<>();

			// check inheriting node as well
			//nodes.add(this.as(SchemaNode.class));

			for (final String name : traitNames) {

				final NodeInterface node = app.nodeQuery(StructrTraits.SCHEMA_NODE).name(name).getFirst();
				if (node != null && node.is(StructrTraits.SCHEMA_NODE)) {

					types.add(node.as(SchemaNode.class));

				} else if (Traits.getTrait(name) != null) {

					types.add(Traits.getTrait(name));
				}
			}

			for (final TypeInfo type1 : types) {

				for (final TypeInfo type2 : types) {

					checkCompatibilityWith(type1, type2);
				}
			}
		}
	}

	// ----- private methods -----
	private void checkCompatibilityWith(final TypeInfo type1, final TypeInfo type2) throws FrameworkException {

		final String label1 = type1.getTypeName();
		final String label2 = type2.getTypeName();

		if (!label1.equals(label2)) {

			checkPropertyCompatibility(type1, type2);

			/* we don't check method compatibility here!
			final Set<FrameworkMethod> frameworkMethodIntersection = new HashSet<>();
			frameworkMethodIntersection.addAll(frameworkMethods.values());
			frameworkMethodIntersection.retainAll(otherTrait.frameworkMethods.values());

			if (!frameworkMethodIntersection.isEmpty()){

				throw new FrameworkException(422, "Incompatible traits: trait " + name + " clashes with trait " + otherTrait.name + " because both define the same methods " + frameworkMethodIntersection.stream().map(m -> m.getClass().getSuperclass().getName()).collect(Collectors.toList()));
			}

			final Set<AbstractMethod> dynamicMethodIntersection = new HashSet<>();
			dynamicMethodIntersection.addAll(dynamicMethods.values());
			dynamicMethodIntersection.retainAll(otherTrait.dynamicMethods.values());

			if (!dynamicMethodIntersection.isEmpty()){

				throw new FrameworkException(422, "Incompatible traits: trait " + name + " clashes with trait " + otherTrait.name + " because both define the same methods " + dynamicMethodIntersection.stream().map(m -> m.getFullMethodName()).collect(Collectors.toList()));
			}

			 */
		}
	}

	private void checkPropertyCompatibility(final TypeInfo type1, final TypeInfo type2) throws FrameworkException {

		final Map<String, PropertyInfo> properties1 = new LinkedHashMap<>();
		final Map<String, PropertyInfo> properties2 = new LinkedHashMap<>();

		properties1.putAll(collectPropertiesAndTypes(type1));
		properties2.putAll(collectPropertiesAndTypes(type2));

		final Set<String> intersection = new LinkedHashSet<>(properties1.keySet());
		intersection.retainAll(properties2.keySet());

		if (!intersection.isEmpty()) {

			// now we can check the types as well
			for (final String propertyName : intersection) {

				final PropertyInfo propertyType1 = properties1.get(propertyName);
				final PropertyInfo propertyType2 = properties2.get(propertyName);

				if (!propertyType1.canOverride(propertyType2)) {

					throw new FrameworkException(422, "Incompatible property inheritance in type " + getName() + ": traits " + type1.getTypeName() + " and " + type2.getTypeName() + " define the same property " + propertyName + " with different types (" + propertyType1 + " vs. " + propertyType2 + ")");

				} else {

					System.out.println("ignoring property " + propertyName + " because types are identical: " + propertyType1);
				}
			}
		}
	}

	private Map<String, PropertyInfo> collectPropertiesAndTypes(final TypeInfo type) {

		final Map<String, PropertyInfo> propertiesAndTheirTypes = new LinkedHashMap<>();

		if (type != null) {

			final Iterable<PropertyInfo> propertyInfos = type.getPropertyInfo();
			if (propertyInfos != null) {

				for (final PropertyInfo p : propertyInfos) {

					// abstract properties are exempt from overwrite check
					if (p.getPropertyName() != null && !p.isAbstract()) {

						propertiesAndTheirTypes.put(p.getPropertyName(), p);
					}
				}
			}
		}

		return propertiesAndTheirTypes;
	}
}
