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
package org.structr.core.traits.definitions;

import org.structr.core.api.AbstractMethod;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.wrappers.AbstractSchemaNodeTraitWrapper;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 *
 */
public final class AbstractSchemaNodeTraitDefinition extends AbstractTraitDefinition {

	private static final Map<String, Iterable<SchemaMethod>> cachedSchemaMethods = new HashMap<>();

	private static final Property<Iterable<NodeInterface>> schemaProperties      = new EndNodes("schemaProperties", "SchemaNodeProperty");
	private static final Property<Iterable<NodeInterface>> schemaMethods         = new EndNodes("schemaMethods", "SchemaNodeMethod");
	private static final Property<Iterable<NodeInterface>> schemaViews           = new EndNodes("schemaViews", "SchemaNodeView");

	private static final Property<Boolean>                 changelogDisabled     = new BooleanProperty("changelogDisabled");
	private static final Property<String>                  icon                  = new StringProperty("icon");
	private static final Property<String>                  description           = new StringProperty("description");
	private static final Set<String>                       hiddenPropertyNames   = new LinkedHashSet<>();

	/*
	public static final View defaultView = new View(AbstractSchemaNode.class, PropertyView.Public,
		name, icon, changelogDisabled
	);

	public static final View uiView = new View(AbstractSchemaNode.class, PropertyView.Ui,
		name, schemaProperties, schemaViews, schemaMethods, icon, description, changelogDisabled
	);

	public static final View schemaView = new View(AbstractSchemaNode.class, "schema",
		id, typeHandler, name, schemaProperties, schemaViews, schemaMethods, icon, description, changelogDisabled
	);

	public static final View exportView = new View(AbstractSchemaNode.class, "export",
		id, typeHandler, name, icon, description, changelogDisabled
	);

	static {

		hiddenPropertyNames.add("visibilityStartDate");
		hiddenPropertyNames.add("visibilityEndDate");
		hiddenPropertyNames.add("createdBy");
		hiddenPropertyNames.add("hidden");
		hiddenPropertyNames.add("deleted");
	}
	*/

	public AbstractSchemaNodeTraitDefinition() {
		super("AbstractSchemaNode");
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			AbstractSchemaNode.class, (traits, node) -> new AbstractSchemaNodeTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {
		return Set.of();
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		return Set.of(
			schemaProperties,
			schemaMethods,
			schemaViews,
			changelogDisabled,
			icon,
			description
		);
	}

	/*
	@Override
	public void onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		super.onCreation(securityContext, errorBuffer);

		// register transaction post processing that recreates the schema information
		TransactionCommand.postProcess("createDefaultProperties", new CreateBuiltInSchemaEntities(this));
	}

	@Override
	public void onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		super.onModification(securityContext, errorBuffer, modificationQueue);

		// register transaction post processing that recreates the schema information
		TransactionCommand.postProcess("createDefaultProperties", new CreateBuiltInSchemaEntities(this));
	}

	public void createBuiltInSchemaEntities(final ErrorBuffer errorBuffer) throws FrameworkException {
		new CreateBuiltInSchemaEntities(this).execute(securityContext, errorBuffer);
	}

	public void addDynamicView(final String view) {
		dynamicViews.add(view);
	}

	public Set<String> getDynamicViews() {
		return dynamicViews;
	}

	public void clearCachedSchemaMethodsForInstance() {

		cachedSchemaMethods.remove(getUuid());
	}


	public static void clearCachedSchemaMethods() {

		cachedSchemaMethods.clear();
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
					final AbstractSchemaNode parentNode = ((AbstractSchemaNode) node).getProperty(SchemaNode.extendsClass);
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
