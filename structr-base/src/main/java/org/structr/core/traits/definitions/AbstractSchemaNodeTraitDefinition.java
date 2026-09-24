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
package org.structr.core.traits.definitions;

import org.apache.commons.lang3.StringUtils;
import org.structr.api.util.Iterables;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.Relation;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.entity.SchemaView;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Trait;
import org.structr.core.traits.TraitDefinition;
import org.structr.core.traits.Traits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.core.traits.operations.graphobject.OnModification;
import org.structr.core.traits.wrappers.AbstractSchemaNodeTraitWrapper;
import org.structr.schema.AbstractDynamicTraitDefinition;

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

		return Map.of(AbstractSchemaNode.class, (traits, node) -> new AbstractSchemaNodeTraitWrapper(traits, node));
	}

	@Override
	public Map<Class, LifecycleMethod> createLifecycleMethods(final TraitsInstance traitsInstance) {

		final Map<Class, LifecycleMethod> methods = new LinkedHashMap<>();

		methods.put(

			OnCreation.class, new OnCreation() {
				@Override
				public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

					graphObject.as(AbstractSchemaNode.class).checkInheritanceConstraints();
				}
			}
		);

		methods.put(OnModification.class, new OnModification() {

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

		return newSet(schemaProperties, schemaMethods, schemaViews, includeInOpenAPI, changelogDisabled, icon, tags, summary, description, isServiceClass);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(

			PropertyView.Public, newSet(CHANGELOG_DISABLED_PROPERTY, ICON_PROPERTY, TAGS_PROPERTY, SUMMARY_PROPERTY, DESCRIPTION_PROPERTY, IS_SERVICE_CLASS_PROPERTY),

			PropertyView.Ui, newSet(TAGS_PROPERTY, SUMMARY_PROPERTY, DESCRIPTION_PROPERTY, INCLUDE_IN_OPEN_API_PROPERTY, IS_SERVICE_CLASS_PROPERTY),

			PropertyView.Schema, newSet(TAGS_PROPERTY, SUMMARY_PROPERTY, DESCRIPTION_PROPERTY, INCLUDE_IN_OPEN_API_PROPERTY, IS_SERVICE_CLASS_PROPERTY));
	}

	@Override
	public Relation getRelation() {

		return null;
	}

	/**
	 * @return true if the view carries content of the type's own: schema properties, or nonGraphProperties.
	 * A copy of an inherited view is created without either, so this is what tells a copy that nobody has
	 * touched from one that was extended on the subtype. The schema editor writes nonGraphProperties whenever
	 * it saves a view row, also when only the order was changed, so a re-sorted copy counts as content. A
	 * sortOrder on its own does not: it orders nothing.
	 */
	public static boolean hasOwnContent(final Iterable<SchemaProperty> schemaProperties, final String nonGraphProperties) {

		return schemaProperties.iterator().hasNext() || StringUtils.isNotBlank(nonGraphProperties);
	}

	/**
	 * Brings the SchemaView nodes of a type in line with the schema that has just been compiled.
	 *
	 * The views a type inherits - from the Java traits it is built on and from the SchemaView nodes of its
	 * parent types - are materialized as nodes with isBuiltinView = true, so that the schema editor can list
	 * them and add properties to them. These nodes mirror the compiled schema, and a mirror has to follow
	 * its original in both directions: a view that no parent provides anymore has to leave the subtype too,
	 * otherwise an empty view stays behind on every inheriting type and survives restarts (ticket 776).
	 * Whether a view is inherited is not stored anywhere, it is derived: inherited is what one of the type's
	 * other traits contributes. Pure copies contribute nothing (AbstractDynamicTraitDefinition.initializeViews),
	 * so a copy whose origin is gone does not keep the copies below it alive, and a chain A - B - C with the
	 * view on B resolves in this one pass once B is gone. That matters because a schema reload requested
	 * while the schema is being replaced is dropped (SchemaService.schemaIsBeingReplaced), so a cascade of
	 * compilations cannot be relied on.
	 */
	public static void createViewNodesForClass(final TraitsInstance traitsInstance, final AbstractSchemaNode schemaNode) throws FrameworkException {

		final App app                       = StructrApp.getInstance(schemaNode.getSecurityContext());
		final Traits traits                 = traitsInstance.getTraits(schemaNode.getName());
		final Set<String> providedViewNames = getProvidedViewNames(traitsInstance, traits, AbstractDynamicTraitDefinition.nameFor(schemaNode));
		final Set<String> existingViewNames = new HashSet<>();

		// copy first: a view is deleted while we iterate
		for (final SchemaView view : Iterables.toList(schemaNode.getSchemaViews())) {

			final String viewName = view.getName();

			existingViewNames.add(viewName);

			if (view.isBuiltinView() && !PropertyView.isManagedView(viewName) && !providedViewNames.contains(viewName)) {

				// nothing provides this view anymore
				if (hasOwnContent(view.getSchemaProperties(), view.getNonGraphProperties())) {

					// properties were added on the subtype, so it is a view of the subtype's own now
					view.setIsBuiltinView(false);

				} else {

					app.delete(view);
				}
			}
		}

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
			app.create(StructrTraits.SCHEMA_VIEW,
					new NodeAttribute(Traits.of(StructrTraits.SCHEMA_VIEW).key(SchemaViewTraitDefinition.SCHEMA_NODE_PROPERTY),       schemaNode),
					new NodeAttribute(Traits.of(StructrTraits.SCHEMA_VIEW).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),           view),
					new NodeAttribute(Traits.of(StructrTraits.SCHEMA_VIEW).key(SchemaViewTraitDefinition.SCHEMA_PROPERTIES_PROPERTY), properties),
					new NodeAttribute(Traits.of(StructrTraits.SCHEMA_VIEW).key(SchemaViewTraitDefinition.IS_BUILTIN_VIEW_PROPERTY),   true)
			);
		}
	}

	/**
	 * @return the names of the views that something other than the type's own SchemaView nodes provides,
	 * which is what a materialized copy may mirror. After resolveTraitHierarchies() the type's trait
	 * definitions are transitive, so every ancestor is asked directly, by its exact trait name: a type that
	 * extends a built-in type shares the label with it, and both traits have to be heard. Each other trait
	 * contributes its compiled views, which for a parent type excludes its pure copies. Of the type's own
	 * trait only the views count that were registered on it through staticSchemaNodeName - they are in the
	 * trait but not in its definition - because the definition's views come from the very nodes being checked.
	 */
	private static Set<String> getProvidedViewNames(final TraitsInstance traitsInstance, final Traits traits, final String ownTraitName) {

		final Set<String> names = new HashSet<>();

		for (final TraitDefinition definition : traits.getTraitDefinitions()) {

			final String traitName = definition.getName();
			final Trait trait      = traitsInstance.getTrait(traitName);

			if (trait == null) {

				continue;
			}

			if (traitName.equals(ownTraitName)) {

				for (final String viewName : trait.getViewNames()) {

					if (!definition.getViews().containsKey(viewName)) {

						names.add(viewName);
					}
				}

			} else {

				names.addAll(trait.getViewNames());
			}
		}

		return names;
	}
}
