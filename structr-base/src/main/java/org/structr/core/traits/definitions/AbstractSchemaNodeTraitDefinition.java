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

import org.structr.api.util.Iterables;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.Relation;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.graph.*;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.Traits;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.core.traits.operations.graphobject.OnModification;
import org.structr.core.traits.wrappers.AbstractSchemaNodeTraitWrapper;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 *
 */
public final class AbstractSchemaNodeTraitDefinition extends AbstractNodeTraitDefinition {

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
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		final Map<Class, LifecycleMethod> methods = new LinkedHashMap<>();

		methods.put(

			OnCreation.class,
			new OnCreation() {
				@Override
				public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

					// register transaction post processing that recreates the schema information
					TransactionCommand.postProcess("createDefaultProperties", new CreateBuiltInSchemaEntities(graphObject.as(AbstractSchemaNode.class)));
				}
			}
		);

		methods.put(
			OnModification.class,
			new OnModification() {

				@Override
				public void onModification(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

					// register transaction post processing that recreates the schema information
					TransactionCommand.postProcess("createDefaultProperties", new CreateBuiltInSchemaEntities(graphObject.as(AbstractSchemaNode.class)));
				}
			}
		);

		return methods;
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {
		return newSet();
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<Iterable<NodeInterface>> schemaProperties = new EndNodes("schemaProperties", "SchemaNodeProperty");
		final Property<Iterable<NodeInterface>> schemaMethods    = new EndNodes("schemaMethods", "SchemaNodeMethod");
		final Property<Iterable<NodeInterface>> schemaViews      = new EndNodes("schemaViews", "SchemaNodeView");
		final Property<String[]> tags                            = new ArrayProperty("tags", String.class).indexed();
		final Property<Boolean> includeInOpenAPI                 = new BooleanProperty("includeInOpenAPI").indexed();
		final Property<Boolean> changelogDisabled                = new BooleanProperty("changelogDisabled");
		final Property<String>  icon                             = new StringProperty("icon");
		final Property<String>  summary                          = new StringProperty("summary").indexed();
		final Property<String>  description                      = new StringProperty("description").indexed();
		final Property<Boolean> isServiceClass                   = new BooleanProperty("isServiceClass").indexed();


		return newSet(
			schemaProperties,
			schemaMethods,
			schemaViews,
			includeInOpenAPI,
			changelogDisabled,
			icon,
			tags,
			summary,
			description,
			isServiceClass
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(

			PropertyView.Public,
			newSet("changelogDisabled", "icon", "tags", "summary", "description", "isServiceClass"),

			PropertyView.Ui,
			newSet("tags", "summary", "description", "includeInOpenAPI", "isServiceClass"),

			"schema",
			newSet("tags", "summary", "description", "includeInOpenAPI", "isServiceClass"),

			"export",
			newSet("changelogDisabled", "icon", "tags", "summary", "description", "isServiceClass")
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	private static class CreateBuiltInSchemaEntities implements TransactionPostProcess {

		private AbstractSchemaNode node = null;

		public CreateBuiltInSchemaEntities(final AbstractSchemaNode node) {
			this.node = node;
		}

		@Override
		public boolean execute(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

			final String type = node.getClassName();

			createViewNodesForClass(node, type);

			return true;
		}

		private static void createViewNodesForClass(final AbstractSchemaNode schemaNode, final String type) throws FrameworkException {

			final Set<String> existingViewNames = Iterables.toList(schemaNode.getSchemaViews()).stream().map(v -> v.getName()).collect(Collectors.toSet());
			final Traits traits                 = Traits.of(type);

			for (final String view : traits.getViewNames()) {

				// Don't create duplicate and internal views
				if (existingViewNames.contains(view)) {
					continue;
				}

				final Set<String> viewPropertyNames  = new HashSet<>();
				final List<NodeInterface> properties = new LinkedList<>();

				// collect names of properties in the given view
				for (final PropertyKey key : traits.getPropertyKeysForView(view)) {

					if (!key.isDynamic()) {
						viewPropertyNames.add(key.jsonName());
					}
				}

				// collect schema properties that match the view
				// if parentNode is set, we're adding inherited properties from the parent node
				for (final SchemaProperty schemaProperty : schemaNode.getSchemaProperties()) {

					final String schemaPropertyName = schemaProperty.getName();
					if (viewPropertyNames.contains(schemaPropertyName)) {

						properties.add(schemaProperty);
					}
				}

				// create view node
				StructrApp.getInstance(schemaNode.getSecurityContext()).create("SchemaView",
						new NodeAttribute(Traits.of("SchemaView").key("schemaNode"),       schemaNode),
						new NodeAttribute(Traits.of("SchemaView").key("name"),             view),
						new NodeAttribute(Traits.of("SchemaView").key("schemaProperties"), properties),
						new NodeAttribute(Traits.of("SchemaView").key("isBuiltinView"),    true)
				);
			}
		}
	}
}
