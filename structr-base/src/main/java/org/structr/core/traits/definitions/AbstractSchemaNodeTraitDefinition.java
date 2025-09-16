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
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.TraitsInstance;
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

	public static final String SCHEMA_PROPERTIES_PROPERTY   = "schemaProperties";
	public static final String SCHEMA_METHODS_PROPERTY      = "schemaMethods";
	public static final String SCHEMA_VIEWS_PROPERTY        = "schemaViews";
	public static final String TAGS_PROPERTY                = "tags";
	public static final String INCLUDE_IN_OPEN_API_PROPERTY = "includeInOpenAPI";
	public static final String CHANGELOG_DISABLED_PROPERTY  = "changelogDisabled";
	public static final String ICON_PROPERTY                = "icon";
	public static final String SUMMARY_PROPERTY             = "summary";
	public static final String DESCRIPTION_PROPERTY         = "description";
	public static final String IS_SERVICE_CLASS_PROPERTY    = "isServiceClass";

	public AbstractSchemaNodeTraitDefinition() {
		super(StructrTraits.ABSTRACT_SCHEMA_NODE);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			AbstractSchemaNode.class, (traits, node) -> new AbstractSchemaNodeTraitWrapper(traits, node)
		);
	}

	@Override
	public Map<Class, LifecycleMethod> createLifecycleMethods(final TraitsInstance traitsInstance) {

		final Map<Class, LifecycleMethod> methods = new LinkedHashMap<>();

		methods.put(

			OnCreation.class,
			new OnCreation() {
				@Override
				public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

					graphObject.as(AbstractSchemaNode.class).checkInheritanceConstraints();
				}
			}
		);

		methods.put(
			OnModification.class,
			new OnModification() {

				@Override
				public void onModification(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

					graphObject.as(AbstractSchemaNode.class).checkInheritanceConstraints();
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
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<Iterable<NodeInterface>> schemaProperties = new EndNodes(traitsInstance, SCHEMA_PROPERTIES_PROPERTY, StructrTraits.SCHEMA_NODE_PROPERTY);
		final Property<Iterable<NodeInterface>> schemaMethods    = new EndNodes(traitsInstance, SCHEMA_METHODS_PROPERTY, StructrTraits.SCHEMA_NODE_METHOD);
		final Property<Iterable<NodeInterface>> schemaViews      = new EndNodes(traitsInstance, SCHEMA_VIEWS_PROPERTY, StructrTraits.SCHEMA_NODE_VIEW);
		final Property<String[]> tags                            = new ArrayProperty(TAGS_PROPERTY, String.class).indexed();
		final Property<Boolean> includeInOpenAPI                 = new BooleanProperty(INCLUDE_IN_OPEN_API_PROPERTY).indexed();
		final Property<Boolean> changelogDisabled                = new BooleanProperty(CHANGELOG_DISABLED_PROPERTY);
		final Property<String>  icon                             = new StringProperty(ICON_PROPERTY);
		final Property<String>  summary                          = new StringProperty(SUMMARY_PROPERTY).indexed();
		final Property<String>  description                      = new StringProperty(DESCRIPTION_PROPERTY).indexed();
		final Property<Boolean> isServiceClass                   = new BooleanProperty(IS_SERVICE_CLASS_PROPERTY).indexed();

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
			newSet(CHANGELOG_DISABLED_PROPERTY, ICON_PROPERTY, TAGS_PROPERTY, SUMMARY_PROPERTY, DESCRIPTION_PROPERTY, IS_SERVICE_CLASS_PROPERTY),

			PropertyView.Ui,
			newSet(TAGS_PROPERTY, SUMMARY_PROPERTY, DESCRIPTION_PROPERTY, INCLUDE_IN_OPEN_API_PROPERTY, IS_SERVICE_CLASS_PROPERTY),

			PropertyView.Schema,
			newSet(TAGS_PROPERTY, SUMMARY_PROPERTY, DESCRIPTION_PROPERTY, INCLUDE_IN_OPEN_API_PROPERTY, IS_SERVICE_CLASS_PROPERTY)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	public static void createViewNodesForClass(final TraitsInstance traitsInstance, final AbstractSchemaNode schemaNode, final String type) throws FrameworkException {

		final Set<String> existingViewNames = Iterables.toList(schemaNode.getSchemaViews()).stream().map(v -> v.getName()).collect(Collectors.toSet());
		final Traits traits                 = traitsInstance.getTraits(type);

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
			StructrApp.getInstance(schemaNode.getSecurityContext()).create(StructrTraits.SCHEMA_VIEW,
					new NodeAttribute(Traits.of(StructrTraits.SCHEMA_VIEW).key(SchemaViewTraitDefinition.SCHEMA_NODE_PROPERTY),       schemaNode),
					new NodeAttribute(Traits.of(StructrTraits.SCHEMA_VIEW).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),           view),
					new NodeAttribute(Traits.of(StructrTraits.SCHEMA_VIEW).key(SchemaViewTraitDefinition.SCHEMA_PROPERTIES_PROPERTY), properties),
					new NodeAttribute(Traits.of(StructrTraits.SCHEMA_VIEW).key(SchemaViewTraitDefinition.IS_BUILTIN_VIEW_PROPERTY),   true)
			);
		}
	}
}
