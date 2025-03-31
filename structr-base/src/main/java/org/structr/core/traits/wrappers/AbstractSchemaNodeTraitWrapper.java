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
package org.structr.core.traits.wrappers;

import org.structr.api.util.Iterables;
import org.structr.core.entity.*;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.AbstractSchemaNodeTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.SchemaNodeTraitDefinition;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

	/*
	public void createBuiltInSchemaEntities(final ErrorBuffer errorBuffer) throws FrameworkException {
		new CreateBuiltInSchemaEntities(this).execute(securityContext, errorBuffer);
	}

	public void addDynamicView(final String view) {
		dynamicViews.add(view);
	}

	public Set<String> getDynamicViews() {
		return dynamicViews;
	}

	private static class CreateBuiltInSchemaEntities implements TransactionPostProcess {

		private AbstractSchemaNode node = null;

		public CreateBuiltInSchemaEntities(final AbstractSchemaNode node) {
			this.node = node;
		}

		@Override
		public boolean execute(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

			final ConfigurationProvider config  = StructrApp.getConfiguration();

			// determine runtime type
			Class builtinClass = config.getNodeEntityClass(node.getClassName());
			if (builtinClass == null) {

				// second try: relationship class name
				builtinClass = config.getRelationshipEntityClass(node.getClassName());
			}

			if (builtinClass != null) {

				createViewNodesForClass(node, builtinClass, config, null);

				final Class superClass = builtinClass.getSuperclass();
				if (superClass != null) {
					final AbstractSchemaNode parentNode = ((AbstractSchemaNode) node).getProperty(traits.key("extendsClass"));
					createViewNodesForClass(node, superClass, config, parentNode);
				}

			}

			return true;
		}

		private static void createViewNodesForClass(final AbstractSchemaNode schemaNode, final Class cls, final ConfigurationProvider config, final AbstractSchemaNode parentNode) throws FrameworkException {

			final Set<String> existingViewNames = Iterables.toList(schemaNode.getSchemaViews()).stream().map(v -> v.getName()).collect(Collectors.toSet());

			for (final String view : config.getPropertyViewsForType(cls)) {

				// Don't create duplicate and internal views
				if (existingViewNames.contains(view)) {
					continue;
				}

				final Set<String> viewPropertyNames   = new HashSet<>();
				final List<SchemaProperty> properties = new LinkedList<>();

				// collect names of properties in the given view
				for (final PropertyKey key : config.getPropertySet(cls, view)) {

					if (parentNode != null || key.isPartOfBuiltInSchema()) {
						viewPropertyNames.add(key.jsonName());
					}
				}

				// collect schema properties that match the view
				// if parentNode is set, we're adding inherited properties from the parent node
				for (final SchemaProperty schemaProperty : (parentNode != null ? parentNode : schemaNode).getProperty(SchemaNode.schemaProperties)) {

					final String schemaPropertyName = schemaProperty.getProperty(SchemaProperty.name);
					if (viewPropertyNames.contains(schemaPropertyName)) {

						properties.add(schemaProperty);
					}
				}

				// create view node
				StructrApp.getInstance(schemaNode.getSecurityContext()).create(SchemaView.class,
						new NodeAttribute(SchemaView.schemaNode, schemaNode),
						new NodeAttribute(SchemaView.name, view),
						new NodeAttribute(SchemaView.schemaProperties, properties),
						new NodeAttribute(SchemaView.isBuiltinView, true)
				);

			}
		}
	}
	*/
}
