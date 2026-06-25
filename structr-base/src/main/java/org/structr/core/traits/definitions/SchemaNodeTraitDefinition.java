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
import org.structr.api.util.ResultStream;
import org.structr.common.ChannelInput;
import org.structr.common.PathResolvingComparator;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.SemanticErrorToken;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.api.JavaMethod;
import org.structr.core.app.Query;
import org.structr.core.app.QueryGroup;
import org.structr.core.app.StructrApp;
import org.structr.core.datasources.ExampleData;
import org.structr.core.datasources.SortInfo;
import org.structr.core.entity.DataSource;
import org.structr.core.entity.Relation;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.datasource.DataSourceOperations;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.core.traits.operations.graphobject.OnModification;
import org.structr.core.traits.operations.nodeinterface.OnNodeDeletion;
import org.structr.core.traits.wrappers.SchemaNodeTraitWrapper;
import org.structr.schema.ReloadSchema;
import org.structr.schema.action.ActionContext;
import org.structr.web.datasource.DataField;
import org.structr.web.datasource.FieldDefinition;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 *
 */
public class SchemaNodeTraitDefinition extends AbstractNodeTraitDefinition {

	public static final Set<String> PROPERTY_KEY_BLACKLIST_FOR_COMPONENTS = Set.of(
		NodeInterfaceTraitDefinition.GRANTEES_PROPERTY,
		NodeInterfaceTraitDefinition.HIDDEN_PROPERTY,
		NodeInterfaceTraitDefinition.OWNER_ID_PROPERTY
	);

	public static final String RELATED_TO_PROPERTY                = "relatedTo";
	public static final String RELATED_FROM_PROPERTY              = "relatedFrom";
	public static final String SCHEMA_GRANTS_PROPERTY             = "schemaGrants";
	public static final String INHERITED_TRAITS_PROPERTY          = "inheritedTraits";
	public static final String NAME_PROPERTY                      = "name";
	public static final String DEFAULT_SORT_KEY_PROPERTY          = "defaultSortKey";
	public static final String DEFAULT_SORT_ORDER_PROPERTY        = "defaultSortOrder";
	public static final String DEFAULT_VISIBLE_TO_PUBLIC_PROPERTY = "defaultVisibleToPublic";
	public static final String DEFAULT_VISIBLE_TO_AUTH_PROPERTY   = "defaultVisibleToAuth";
	public static final String HIERARCHY_LEVEL_PROPERTY           = "hierarchyLevel";
	public static final String REL_COUNT_PROPERTY                 = "relCount";
	public static final String IS_INTERFACE_PROPERTY              = "isInterface";
	public static final String IS_ABSTRACT_PROPERTY               = "isAbstract";
	public static final String CATEGORY_PROPERTY                  = "category";

	private static final Set<String> EntityNameBlacklist = new LinkedHashSet<>(Arrays.asList(new String[] {
		"Relation", "Property"
	}));

	public SchemaNodeTraitDefinition() {
		super(StructrTraits.SCHEMA_NODE);
	}

	@Override
	public Map<Class, LifecycleMethod> createLifecycleMethods(TraitsInstance traitsInstance) {

		return Map.of(

			IsValid.class,
			new IsValid() {

				@Override
				public Boolean isValid(GraphObject obj, ErrorBuffer errorBuffer) {

					boolean valid = true;

					valid &= ValidationHelper.isValidUniqueProperty(obj, Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), errorBuffer);
					valid &= ValidationHelper.isValidStringMatchingRegex(obj, Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), SchemaNode.schemaNodeNamePattern,
						"Type name must match the following pattern: '" + SchemaNode.schemaNodeNamePattern + "', which means it must begin with an uppercase letter and may only contain letters, numbers and underscores.",
						errorBuffer);

					// Make sure a (local) property does not illegally override a relation property or a
					// property inherited from another trait. This is checked here on the schema node
					// (rather than on the schema property), because the schema node is modified both when
					// a property is added or removed and when a trait is added or removed - so a single
					// validation location covers both cases. The keys are looked up on the already-existing
					// traits (this type and its inherited traits), because the schema is only reloaded after
					// the transaction commits, i.e. the type's own trait does not yet reflect the change.
					final SchemaNode schemaNode    = obj.as(SchemaNode.class);
					final String typeName          = schemaNode.getName();
					final Set<String> sourceTraits = new LinkedHashSet<>();

					// every node type is composed of these base traits in addition to its explicitly
					// inherited traits and its own trait. Including the base traits also catches clashes
					// (e.g. overriding the built-in "name" property with an incompatible type) for a type
					// that is being created in the current transaction and is therefore not registered yet.
					sourceTraits.add(StructrTraits.PROPERTY_CONTAINER);
					sourceTraits.add(StructrTraits.GRAPH_OBJECT);
					sourceTraits.add(StructrTraits.NODE_INTERFACE);
					sourceTraits.add(StructrTraits.ACCESS_CONTROLLABLE);
					sourceTraits.addAll(schemaNode.getInheritedTraits());
					sourceTraits.add(typeName);

					for (final SchemaProperty property : schemaNode.getSchemaProperties()) {

						final String thisPropertyName = property.getName();
						if (thisPropertyName == null) {
							continue;
						}

						try {
							final PropertyKey key = property.createKey(typeName);

							for (final String traitName : sourceTraits) {

								if (!Traits.exists(traitName) || !Traits.of(traitName).hasKey(thisPropertyName)) {
									continue;
								}

								final PropertyKey existingKey = Traits.of(traitName).key(thisPropertyName);

								// don't allow overriding relation properties
								if (existingKey instanceof RelationProperty) {

									errorBuffer.add(new SemanticErrorToken(StructrTraits.SCHEMA_PROPERTY, "name", "cannot_override").withValue(thisPropertyName).withDetail(existingKey.jsonName() + " on " + existingKey.getDeclaringTrait().getName() + " is not overridable"));
									valid = false;
									break;

								// a key declared by this type's own trait may be a local property that is being
								// replaced within the current transaction, so its (stale) type must be ignored.
								// a function property without a type hint resolves to "Object"; its concrete type
								// can still be set afterwards, so it must not be rejected for a type mismatch yet.
								} else if (!key.typeName().equals(existingKey.typeName())
									&& !existingKey.getDeclaringTrait().getLabel().equals(typeName)
									&& !(key instanceof FunctionProperty && key.typeHint() == null)) {

									errorBuffer.add(new SemanticErrorToken(StructrTraits.SCHEMA_PROPERTY, "name", "cannot_override").withValue(thisPropertyName).withDetail("Type mismatch, " + key.typeName() + " cannot override " + existingKey.typeName() + " from " + traitName));
									valid = false;
									break;
								}
							}

						} catch (FrameworkException fex) {

							// could not build the key for validation, skip this property
						}
					}

					return valid;
				}
			},

			OnCreation.class,
			new OnCreation() {

				@Override
				public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

					throwExceptionIfTypeAlreadyExists(graphObject);

					TransactionCommand.postProcess("reloadSchema", new ReloadSchema(true));
				}
			},

			OnModification.class,
			new OnModification() {

				@Override
				public void onModification(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

					if (modificationQueue.isPropertyModified(graphObject, Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY))) {
						throwExceptionIfTypeAlreadyExists(graphObject);
					}

					TransactionCommand.postProcess("reloadSchema", new ReloadSchema(true));
				}
			},

			OnNodeDeletion.class,
			new OnNodeDeletion() {

				@Override
				public void onNodeDeletion(final NodeInterface nodeInterface, final SecurityContext securityContext) throws FrameworkException {

					TransactionCommand.postProcess("reloadSchema", new ReloadSchema(true));
				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			SchemaNode.class, (traits, node) -> new SchemaNodeTraitWrapper(traits, node)
		);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

			DataSourceOperations.class, new DataSourceOperations<NodeInterface>() {

				@Override
				public ResultStream<NodeInterface> getValues(final ActionContext actionContext, final DataSource provider, final ChannelInput input) throws FrameworkException {

					final SecurityContext securityContext = actionContext.getSecurityContext();
					final SchemaNode schemaNode           = provider.as(SchemaNode.class);
					final String name                     = schemaNode.getName();
					final Traits traits                   = Traits.of(name);
					final int pageSize                    = input != null ? input.pageSize() : Integer.MAX_VALUE;
					final int page                        = input != null ? input.page() : 1;
					final Query<NodeInterface> query      = StructrApp.getInstance(securityContext).nodeQuery(name);

					query.includeHidden(provider.includeHidden());

					if (input != null) {

						final List<SortInfo> sortKeys = input.sortKeys();
						if (sortKeys != null && !sortKeys.isEmpty()) {

							for (final SortInfo sortInfo : sortKeys) {

								if (sortInfo.sortKey.contains(".")) {

									// this is the place where path-based sort keys are resolved in this data source
									query.comparator((Comparator) new PathResolvingComparator(actionContext, sortInfo.sortKey, sortInfo.descending));

								} else {

									if (traits.hasKey(sortInfo.sortKey)) {

										final PropertyKey sortKey = traits.key(sortInfo.sortKey);
										if (sortKey != null) {

											query.sort(sortKey, sortInfo.descending);
										}
									}
								}
							}

						} else {

							// sort by name by default
							query.sort(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), false);
						}

						// use contains query over the searchable fields
						if (input.filter() != null) {

							for (final String part : input.filter().split(" ")) {

								final String trimmed = part.trim();

								if (StringUtils.isNotBlank(trimmed)) {

									// we AND together the individual parts of the filter string
									final QueryGroup<NodeInterface> andGroup = query.and();
									final QueryGroup<NodeInterface> orGroup = andGroup.or();

									for (final DataField searchableField : input.searchableFields()) {

										searchableField.configureQuery(traits, orGroup, trimmed);
									}
								}
							}
						}

						// apply pagination etc.
						query.pageSize(pageSize).page(page);

					} else {

						// sort by name by default
						query.sort(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), false);
					}

					return query.getResultStream();
				}

				@Override
				public Map<String, FieldDefinition> getFields(final ActionContext actionContext, final DataSource provider) throws FrameworkException {

					final Map<String, FieldDefinition> output = new LinkedHashMap<>();
					final String name                         = provider.getDataType(actionContext);
					final Traits traits                       = Traits.of(name);

					// transform input
					for (final PropertyKey key : traits.getPropertyKeysForView(PropertyView.All)) {

						// hide some internal properties
						if (!PROPERTY_KEY_BLACKLIST_FOR_COMPONENTS.contains(key.jsonName())) {
							output.put(key.jsonName(), key.getFieldDefinition());
						}
					}

					return output;
				}

				@Override
				public String getDataType(final ActionContext actionContext, final DataSource provider) throws FrameworkException {
					return provider.as(SchemaNode.class).getTypeName();
				}

				@Override
				public int getDimension(final DataSource provider) {
					return 1;
				}
			}
		);
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {

		return newSet(
			new JavaMethod("checkValidity", false, true) {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {

					final String name = (String) arguments.get(0);
					if (name != null) {

						final Pattern pattern = Pattern.compile(SchemaNode.schemaNodeNamePattern);
						final Matcher matcher = pattern.matcher(name);

						if (matcher.matches()) {

							if (StructrApp.getInstance().nodeQuery(StructrTraits.SCHEMA_NODE).name(name).getFirst() != null) {
								return "Type '" + name + "' already exists.";
							}

						} else {

							return "Type name must begin with an uppercase letter and may only contain letters, numbers and underscores.";
						}
					}

					return null;
				}
			},
			new JavaMethod("getExampleData", false, true) {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {

					final String typeOfExampleData = (String) arguments.get("typeOfExampleData");
					final String resultFormat      = (String) arguments.get("resultFormat");
					final String inputValue        = (String) arguments.get("inputValue");

					if (StringUtils.isNotBlank(typeOfExampleData) && StringUtils.isNotBlank(inputValue) && StringUtils.isNotBlank(resultFormat)) {

						return ExampleData.get(typeOfExampleData, resultFormat, inputValue);
					}

					return null;
				}
			}
		);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<Iterable<NodeInterface>>          relatedTo              = new EndNodes(traitsInstance, RELATED_TO_PROPERTY, StructrTraits.SCHEMA_RELATIONSHIP_SOURCE_NODE);
		final Property<Iterable<NodeInterface>>          relatedFrom            = new StartNodes(traitsInstance, RELATED_FROM_PROPERTY, StructrTraits.SCHEMA_RELATIONSHIP_TARGET_NODE);
		final Property<Iterable<NodeInterface>>          schemaGrants           = new StartNodes(traitsInstance, SCHEMA_GRANTS_PROPERTY, StructrTraits.SCHEMA_GRANT_SCHEMA_NODE_RELATIONSHIP);
		final Property<String[]>                         inheritedTraits        = new ArrayProperty(INHERITED_TRAITS_PROPERTY, String.class);
		final Property<String>                           uniqueNameKey          = new StringProperty(NAME_PROPERTY).unique().indexed();
		final Property<String>                           defaultSortKey         = new StringProperty(DEFAULT_SORT_KEY_PROPERTY);
		final Property<String>                           defaultSortOrder       = new StringProperty(DEFAULT_SORT_ORDER_PROPERTY);
		final Property<Boolean>                          defaultVisibleToPublic = new BooleanProperty(DEFAULT_VISIBLE_TO_PUBLIC_PROPERTY).readOnly().indexed();
		final Property<Boolean>                          defaultVisibleToAuth   = new BooleanProperty(DEFAULT_VISIBLE_TO_AUTH_PROPERTY).readOnly().indexed();
		final Property<Integer>                          hierarchyLevel         = new IntProperty(HIERARCHY_LEVEL_PROPERTY).indexed();
		final Property<Integer>                          relCount               = new IntProperty(REL_COUNT_PROPERTY).indexed();
		final Property<Boolean>                          isInterface            = new BooleanProperty(IS_INTERFACE_PROPERTY).indexed();
		final Property<Boolean>                          isAbstract             = new BooleanProperty(IS_ABSTRACT_PROPERTY).indexed();
		final Property<String>                           category               = new StringProperty(CATEGORY_PROPERTY).indexed();

		return newSet(
			relatedTo,
			relatedFrom,
			schemaGrants,
			inheritedTraits,
			defaultSortKey,
			defaultSortOrder,
			defaultVisibleToPublic,
			defaultVisibleToAuth,
			hierarchyLevel,
			relCount,
			isInterface,
			isAbstract,
			category,
			uniqueNameKey
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(

			PropertyView.Public,
			newSet(
					NAME_PROPERTY, INHERITED_TRAITS_PROPERTY, RELATED_TO_PROPERTY, RELATED_FROM_PROPERTY, DEFAULT_SORT_KEY_PROPERTY,
					DEFAULT_SORT_ORDER_PROPERTY, HIERARCHY_LEVEL_PROPERTY, REL_COUNT_PROPERTY, IS_INTERFACE_PROPERTY, IS_ABSTRACT_PROPERTY,
					DEFAULT_VISIBLE_TO_PUBLIC_PROPERTY, DEFAULT_VISIBLE_TO_AUTH_PROPERTY
			),

			PropertyView.Ui,
			newSet(
					NAME_PROPERTY, INHERITED_TRAITS_PROPERTY, RELATED_TO_PROPERTY, RELATED_FROM_PROPERTY, DEFAULT_SORT_KEY_PROPERTY,
					DEFAULT_SORT_ORDER_PROPERTY, HIERARCHY_LEVEL_PROPERTY, REL_COUNT_PROPERTY, IS_INTERFACE_PROPERTY, IS_ABSTRACT_PROPERTY,
					DEFAULT_VISIBLE_TO_PUBLIC_PROPERTY, DEFAULT_VISIBLE_TO_AUTH_PROPERTY,
					CATEGORY_PROPERTY,
					AbstractSchemaNodeTraitDefinition.SCHEMA_PROPERTIES_PROPERTY, AbstractSchemaNodeTraitDefinition.SCHEMA_VIEWS_PROPERTY,
					AbstractSchemaNodeTraitDefinition.SCHEMA_METHODS_PROPERTY, AbstractSchemaNodeTraitDefinition.ICON_PROPERTY,
					AbstractSchemaNodeTraitDefinition.CHANGELOG_DISABLED_PROPERTY, AbstractSchemaNodeTraitDefinition.INCLUDE_IN_OPEN_API_PROPERTY
			),

			PropertyView.Schema,
			newSet(
					GraphObjectTraitDefinition.ID_PROPERTY, GraphObjectTraitDefinition.TYPE_PROPERTY,
					NAME_PROPERTY, INHERITED_TRAITS_PROPERTY, RELATED_TO_PROPERTY, RELATED_FROM_PROPERTY, DEFAULT_SORT_KEY_PROPERTY,
					DEFAULT_SORT_ORDER_PROPERTY, HIERARCHY_LEVEL_PROPERTY, REL_COUNT_PROPERTY, IS_INTERFACE_PROPERTY, IS_ABSTRACT_PROPERTY,
					DEFAULT_VISIBLE_TO_PUBLIC_PROPERTY, DEFAULT_VISIBLE_TO_AUTH_PROPERTY,
					CATEGORY_PROPERTY,
					SCHEMA_GRANTS_PROPERTY,
					AbstractSchemaNodeTraitDefinition.SCHEMA_PROPERTIES_PROPERTY, AbstractSchemaNodeTraitDefinition.SCHEMA_VIEWS_PROPERTY,
					AbstractSchemaNodeTraitDefinition.SCHEMA_METHODS_PROPERTY, AbstractSchemaNodeTraitDefinition.ICON_PROPERTY,
					AbstractSchemaNodeTraitDefinition.CHANGELOG_DISABLED_PROPERTY, AbstractSchemaNodeTraitDefinition.INCLUDE_IN_OPEN_API_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	/**
	* If the system is fully initialized (and no schema replacement is currently active), we disallow overriding (known) existing types so we can prevent unwanted behavior.
	* If a user were to create a type 'Html', he could cripple Structrs Page rendering completely.
	* This is a fix for all types in the Structr context - this does not help if the user creates a type named 'String' or 'Object'.
	* That could still lead to unexpected behavior.
	*
	* @throws FrameworkException if a pre-existing type is encountered
	*/
	private void throwExceptionIfTypeAlreadyExists(final GraphObject graphObject) throws FrameworkException {

		if (Services.getInstance().isInitialized() && ! Services.getInstance().isOverridingSchemaTypesAllowed()) {

			final String typeName = graphObject.getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY));

			// add type names to list of forbidden entity names
			if (EntityNameBlacklist.contains(typeName)) {
				throw new FrameworkException(422, "Type '" + typeName + "' already exists. To prevent unwanted/unexpected behavior this is forbidden.");
			}

			/*
			// add type names to list of forbidden entity names
			if (StructrApp.getConfiguration().getNodeEntities().containsKey(typeName)) {
				throw new FrameworkException(422, "Type '" + typeName + "' already exists. To prevent unwanted/unexpected behavior this is forbidden.");
			}

			// add interfaces to list of forbidden entity names
			if (StructrApp.getConfiguration().getInterfaces().containsKey(typeName)) {
				throw new FrameworkException(422, "Type '" + typeName + "' already exists. To prevent unwanted/unexpected behavior this is forbidden.");
			}
			*/
		}
	}
}
