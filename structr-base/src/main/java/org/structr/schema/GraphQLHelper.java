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

import graphql.Scalars;
import graphql.schema.*;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Principal;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graphql.GraphQLListType;
import org.structr.core.property.*;
import org.structr.core.traits.Traits;

import java.util.*;

import static graphql.schema.GraphQLTypeReference.typeRef;

public class GraphQLHelper {

	private final Map<Class, GraphQLScalarType> graphQLTypeMap     = new LinkedHashMap<>();
	private final Map<String, List<GraphQLInputObjectField>> cache = new LinkedHashMap<>();
	public static final String GraphQLNodeReferenceName            = "StructrNodeReference";

	public static final Set<String> InternalTypes = Set.of(
		"PropertyContainer", "GraphObject", "NodeInterface", "RelationshipInterface", "Principal",
		"OneToOne", "OneToMany", "ManyToOne", "ManyToMany",
		"LinkedTreeNode", "LinkedListNode"

	);

	public GraphQLHelper() {

		graphQLTypeMap.put(PasswordProperty.class,      Scalars.GraphQLString);
		graphQLTypeMap.put(BooleanProperty.class,       Scalars.GraphQLBoolean);
		graphQLTypeMap.put(IntProperty.class,           Scalars.GraphQLInt);
		graphQLTypeMap.put(StringProperty.class,        Scalars.GraphQLString);
		graphQLTypeMap.put(DoubleProperty.class,        Scalars.GraphQLFloat);
		graphQLTypeMap.put(ElementCounter.class,        Scalars.GraphQLInt);
		graphQLTypeMap.put(LongProperty.class,          Scalars.GraphQLLong);
		graphQLTypeMap.put(EnumProperty.class,          Scalars.GraphQLString);
		graphQLTypeMap.put(EnumArrayProperty.class,     Scalars.GraphQLString);
		graphQLTypeMap.put(DateProperty.class,          Scalars.GraphQLString);
		graphQLTypeMap.put(ZonedDateTimeProperty.class, Scalars.GraphQLString);
	}

	public void initializeGraphQLForNodeType(final String nodeType, final Map<String, GraphQLType> graphQLTypes, final Set<String> blacklist) throws IllegalAccessException {

		// check if some base class already initialized us
		if (graphQLTypes.containsKey(nodeType) || !Traits.exists(nodeType)) {

			// nothing to do
			return;
		}

		// variables
		final Map<String, GraphQLFieldDefinition> fields = new LinkedHashMap<>();

		// register node reference type to filter related nodes
		if (!graphQLTypes.containsKey(GraphQLNodeReferenceName)) {

			graphQLTypes.put(GraphQLNodeReferenceName, GraphQLInputObjectType.newInputObject()
				.name(GraphQLNodeReferenceName)
				.field(GraphQLInputObjectField.newInputObjectField().name("id").type(Scalars.GraphQLString).build())
				.field(GraphQLInputObjectField.newInputObjectField().name("name").type(Scalars.GraphQLString).build())
				.build());
		}

		for (final String trait : Traits.of(nodeType).getAllTraits()) {

			if (!trait.equals(nodeType) && !InternalTypes.contains(trait)) {

				// add inherited fields from superclass
				registerParentType(trait, graphQLTypes, fields, blacklist);
			}
		}

		// add inherited fields from interfaces
		for (final String ifaceType : getInterfaceClasses(nodeType)) {

			if (!InternalTypes.contains(ifaceType)) {

				registerParentType(ifaceType, graphQLTypes, fields, blacklist);
			}
		}

		// add dynamic fields
		for (final PropertyKey property : getNonRelationshipProperties(nodeType)) {

			final GraphQLFieldDefinition field = getGraphQLField(property);
			if (field != null) {

				fields.put(field.getName(), field);
			}
		}

		// outgoing relationships
		for (final Property property : getRelationshipProperties(nodeType, true)) {
			registerOutgoingGraphQLFields(property, fields, blacklist);
		}

		// incoming relationships
		for (final Property property : getRelationshipProperties(nodeType, false)) {
			registerIncomingGraphQLFields(property, fields, blacklist);
		}

		fields.put("id", GraphQLFieldDefinition.newFieldDefinition().name("id").type(Scalars.GraphQLString).arguments(getGraphQLArgumentsForUUID()).build());

		// add static fields (name etc., can be overwritten)
		fields.putIfAbsent("type", GraphQLFieldDefinition.newFieldDefinition().name("type").type(Scalars.GraphQLString).build());
		fields.putIfAbsent("name", GraphQLFieldDefinition.newFieldDefinition().name("name").type(Scalars.GraphQLString).arguments(getGraphQLArgumentsForPropertyType(Traits.of("NodeInterface").key("name"))).build());
		fields.putIfAbsent("owner", GraphQLFieldDefinition.newFieldDefinition().name("owner").type(typeRef("Principal")).arguments(getGraphQLArgumentsForRelatedType(Principal.class)).build());
		fields.putIfAbsent("createdBy", GraphQLFieldDefinition.newFieldDefinition().name("createdBy").type(Scalars.GraphQLString).build());
		fields.putIfAbsent("createdDate", GraphQLFieldDefinition.newFieldDefinition().name("createdDate").type(Scalars.GraphQLString).build());
		fields.putIfAbsent("lastModifiedBy", GraphQLFieldDefinition.newFieldDefinition().name("lastModifiedBy").type(Scalars.GraphQLString).build());
		fields.putIfAbsent("lastModifiedDate", GraphQLFieldDefinition.newFieldDefinition().name("lastModifiedDate").type(Scalars.GraphQLString).build());
		fields.putIfAbsent("visibleToPublicUsers", GraphQLFieldDefinition.newFieldDefinition().name("visibleToPublicUsers").type(Scalars.GraphQLString).build());
		fields.putIfAbsent("visibleToAuthenticatedUsers", GraphQLFieldDefinition.newFieldDefinition().name("visibleToAuthenticatedUsers").type(Scalars.GraphQLString).build());

		// register type in GraphQL schema
		final GraphQLObjectType.Builder builder = GraphQLObjectType.newObject();

		builder.name(nodeType);
		builder.fields(new LinkedList<>(fields.values()));

		graphQLTypes.put(nodeType, builder.build());
	}

	public void initializeGraphQLForRelationshipType(final String className, final Map<String, GraphQLType> graphQLTypes) throws IllegalAccessException {

		final List<GraphQLFieldDefinition> fields = new LinkedList<>();

		// add static fields (id, name etc.)
		fields.add(GraphQLFieldDefinition.newFieldDefinition().name("id").type(Scalars.GraphQLString).build());
		fields.add(GraphQLFieldDefinition.newFieldDefinition().name("type").type(Scalars.GraphQLString).build());

		// add dynamic fields
		for (final PropertyKey property : getNonRelationshipProperties(className)) {

			final GraphQLFieldDefinition field = getGraphQLField(property);
			if (field != null) {

				fields.add(field);
			}
		}

		final GraphQLObjectType graphQLType = GraphQLObjectType.newObject().name(className).fields(fields).build();

		// register type in GraphQL schema
		graphQLTypes.put(className, graphQLType);
	}

	private GraphQLOutputType getGraphQLOutputTypeForProperty(final PropertyKey property) {

		final GraphQLOutputType outputType = graphQLTypeMap.get(property.getClass());
		if (outputType != null) {

			return outputType;
		}

		final String typeHint = property.typeHint();
		if (typeHint != null) {

			final String lowerCaseTypeHint = typeHint.toLowerCase();
			switch (lowerCaseTypeHint) {

				case "boolean": return graphQLTypeMap.get(BooleanProperty.class);
				case "string":  return graphQLTypeMap.get(StringProperty.class);
				case "int":     return graphQLTypeMap.get(IntProperty.class);
				case "long":    return graphQLTypeMap.get(LongProperty.class);
				case "double":  return graphQLTypeMap.get(DoubleProperty.class);
				case "date":    return graphQLTypeMap.get(DateProperty.class);
			}

			// object array type?
			if (typeHint.endsWith("[]")) {

				// list type
				return new GraphQLListType(typeRef(StringUtils.substringBefore(typeHint, "[]")));

			} else {

				// object type
				return typeRef(typeHint);
			}
		}

		return null;
	}

	private GraphQLInputType getGraphQLInputTypeForProperty(final PropertyKey property) {

		final String typeHint = property.typeHint();
		if (typeHint != null) {

			final String lowerCaseTypeHint = typeHint.toLowerCase();
			switch (lowerCaseTypeHint) {

				case "boolean": return graphQLTypeMap.get(BooleanProperty.class);
				case "string":  return graphQLTypeMap.get(StringProperty.class);
				case "int":     return graphQLTypeMap.get(IntProperty.class);
				case "long":    return graphQLTypeMap.get(LongProperty.class);
				case "double":  return graphQLTypeMap.get(DoubleProperty.class);
				case "date":    return graphQLTypeMap.get(DateProperty.class);
			}
		}

		final GraphQLScalarType type = graphQLTypeMap.get(property.getClass());
		if (type != null) {

			return type;
		}

		// default / fallback
		return Scalars.GraphQLString;
	}

	public List<GraphQLArgument> getGraphQLQueryArgumentsForType(final Map<String, GraphQLInputObjectType> selectionTypes, final Set<String> queryTypeNames, final String type) throws FrameworkException, IllegalAccessException {

		final List<GraphQLArgument> arguments = new LinkedList<>();

		if (Traits.exists(type) && !InternalTypes.contains(type)) {

			for (final String trait : Traits.of(type).getAllTraits()) {

				if (!trait.equals(type) && !InternalTypes.contains(trait)) {

					// add inherited fields from superclass
					arguments.addAll(getGraphQLQueryArgumentsForType(selectionTypes, queryTypeNames, trait));
				}
			}

			// outgoing relationships
			for (final Property outProperty : getRelationshipProperties(type, true)) {

				final RelationProperty relationProperty = (RelationProperty) outProperty;
				final String targetType = relationProperty.getTargetType();
				final String propertyName = outProperty.jsonName();
				final String queryTypeName = type + propertyName + targetType + "InInput";

				if (!queryTypeNames.contains(queryTypeName)) {

					arguments.add(GraphQLArgument.newArgument().name(propertyName).type(GraphQLInputObjectType.newInputObject()
						.name(queryTypeName)
						.fields(getGraphQLInputFieldsForType(selectionTypes, targetType))
						.build()
					).build());

					queryTypeNames.add(queryTypeName);
				}
			}

			// incoming relationships
			for (final Property inProperty : getRelationshipProperties(type, false)) {

				final RelationProperty relationProperty = (RelationProperty) inProperty;
				final String sourceType = relationProperty.getTargetType();
				final String propertyName = inProperty.jsonName();
				final String queryTypeName = type + propertyName + sourceType + "OutInput";

				if (!queryTypeNames.contains(queryTypeName)) {

					arguments.add(GraphQLArgument.newArgument().name(propertyName).type(GraphQLInputObjectType.newInputObject()
						.name(queryTypeName)
						.fields(getGraphQLInputFieldsForType(selectionTypes, sourceType))
						.build()
					).build());

					queryTypeNames.add(queryTypeName);
				}
			}

			// properties
			for (final PropertyKey property : getNonRelationshipProperties(type)) {

				if (property.isIndexed() || property.isCompound()) {

					final String name = property.jsonName();
					final String selectionName = type + name + "Selection";

					GraphQLInputObjectType selectionType = selectionTypes.get(selectionName);
					if (selectionType == null) {

						selectionType = GraphQLInputObjectType.newInputObject()
							.name(selectionName)
							.field(GraphQLInputObjectField.newInputObjectField().name("_contains").type(Scalars.GraphQLString).build())
							.field(GraphQLInputObjectField.newInputObjectField().name("_equals").type(getGraphQLInputTypeForProperty(property)).build())
							.field(GraphQLInputObjectField.newInputObjectField().name("_conj").type(Scalars.GraphQLString).build())
							.build();

						selectionTypes.put(selectionName, selectionType);
					}

					arguments.add(GraphQLArgument.newArgument()
						.name(name)
						.type(selectionType)
						.build()
					);
				}
			}

			final String ownerTypeName = type + "ownerInput";

			if (!queryTypeNames.contains(ownerTypeName)) {

				// manual registration for built-in relationships that are not dynamic
				arguments.add(GraphQLArgument.newArgument().name("owner").type(GraphQLInputObjectType.newInputObject()
					.name(ownerTypeName)
					.fields(getGraphQLInputFieldsForType(selectionTypes, "Principal"))
					.build()
				).build());

				queryTypeNames.add(ownerTypeName);
			}
		}

		return arguments;
	}

	private List<GraphQLInputObjectField> getGraphQLInputFieldsForType(final Map<String, GraphQLInputObjectType> selectionTypes, final String type) throws IllegalAccessException {

		if (Traits.exists(type)) {

			List<GraphQLInputObjectField> data = cache.get(type);

			if (data == null) {

				final Map<String, GraphQLInputObjectField> fields = new LinkedHashMap<>();

				for (final String trait : Traits.of(type).getAllTraits()) {

					if (!trait.equals(type) && !InternalTypes.contains(trait)) {

						// add inherited fields from superclass
						for (final GraphQLInputObjectField field : getGraphQLInputFieldsForType(selectionTypes, trait)) {

							fields.put(field.getName(), field);
						}
					}
				}

				for (final PropertyKey property : getNonRelationshipProperties(type)) {

					if (property.isIndexed() || property.isCompound()) {

						final String name = property.jsonName();
						final String selectionName = name + "Selection";

						GraphQLInputObjectType selectionType = selectionTypes.get(selectionName);
						if (selectionType == null) {

							selectionType = GraphQLInputObjectType.newInputObject()
								.name(selectionName)
								.field(GraphQLInputObjectField.newInputObjectField().name("_contains").type(Scalars.GraphQLString).build())
								.field(GraphQLInputObjectField.newInputObjectField().name("_equals").type(getGraphQLInputTypeForProperty(property)).build())
								.field(GraphQLInputObjectField.newInputObjectField().name("_conj").type(Scalars.GraphQLString).build())
								.build();

							selectionTypes.put(selectionName, selectionType);
						}

						fields.put(name, GraphQLInputObjectField.newInputObjectField().name(name).type(selectionType).build());
					}
				}

				if (!fields.containsKey("name")) {

					GraphQLInputObjectType selectionType = selectionTypes.get("nameSelection");
					if (selectionType == null) {

						selectionType = GraphQLInputObjectType.newInputObject()
							.name("nameSelection")
							.field(GraphQLInputObjectField.newInputObjectField().name("_contains").type(Scalars.GraphQLString).build())
							.field(GraphQLInputObjectField.newInputObjectField().name("_equals").type(Scalars.GraphQLString).build())
							.field(GraphQLInputObjectField.newInputObjectField().name("_conj").type(Scalars.GraphQLString).build())
							.build();

						selectionTypes.put("nameSelection", selectionType);
					}

					fields.put("name", GraphQLInputObjectField.newInputObjectField().name("name").type(selectionType).build());
				}

				data = new LinkedList<>(fields.values());

				// put data in cache
				cache.put(type, data);
			}

			return data;
		}

		// fallback: empty list
		return List.of();
	}

	private void registerParentType(final String parentType, final Map<String, GraphQLType> graphQLTypes, final Map<String, GraphQLFieldDefinition> fields, final Set<String> blacklist) throws IllegalAccessException {

		if (parentType != null && !NodeInterface.class.equals(parentType) && !parentType.equals(this)) {

			final String parentName = parentType;
			if (parentName != null && !blacklist.contains(parentName)) {

				if (!graphQLTypes.containsKey(parentName)) {

					// initialize parent type
					initializeGraphQLForNodeType(parentType, graphQLTypes, blacklist);
				}

				// second try: add fields from parent type
				if (graphQLTypes.containsKey(parentName)) {

					final GraphQLObjectType parentGraphQLType = (GraphQLObjectType)graphQLTypes.get(parentName);
					if (parentGraphQLType != null) {

						for (final GraphQLFieldDefinition field : parentGraphQLType.getFieldDefinitions()) {

							fields.put(field.getName(), field);
						}
					}
				}
			}
		}

	}

	private void registerOutgoingGraphQLFields(final Property property, final Map<String, GraphQLFieldDefinition> fields, final Set<String> blacklist) {

		final RelationProperty relationProperty = (RelationProperty)property;
		final String propertyName               = property.jsonName();
		final String targetTypeName             = relationProperty.getTargetType();

		if (blacklist.contains(targetTypeName)) {
			return;
		}

		final GraphQLFieldDefinition.Builder builder = GraphQLFieldDefinition
			.newFieldDefinition()
			.name(propertyName)
			.type(getGraphQLTypeForCardinality(relationProperty, targetTypeName, true))
			.argument(GraphQLArgument.newArgument().name("_page").type(Scalars.GraphQLInt).build())
			.argument(GraphQLArgument.newArgument().name("_pageSize").type(Scalars.GraphQLInt).build())
			.argument(GraphQLArgument.newArgument().name("_sort").type(Scalars.GraphQLString).build())
			.argument(GraphQLArgument.newArgument().name("_desc").type(Scalars.GraphQLBoolean).build());

		// register reference type so that GraphQL can be used to query related nodes
		if (isMultiple(relationProperty, true)) {

			builder.argument(GraphQLArgument.newArgument().name("_contains").type(typeRef(GraphQLNodeReferenceName)).build());

		} else {

			builder.argument(GraphQLArgument.newArgument().name("_equals").type(typeRef(GraphQLNodeReferenceName)).build());
		}


		// register field
		fields.put(propertyName, builder.build());
	}

	private void registerIncomingGraphQLFields(final Property property, final Map<String, GraphQLFieldDefinition> fields, final Set<String> blacklist) {

		final RelationProperty relationProperty = (RelationProperty)property;
		final String propertyName               = property.jsonName();
		final String sourceTypeName             = relationProperty.getTargetType();

		if (blacklist.contains(sourceTypeName)) {
			return;
		}

		final GraphQLFieldDefinition.Builder builder = GraphQLFieldDefinition
			.newFieldDefinition()
			.name(propertyName)
			.type(getGraphQLTypeForCardinality(relationProperty, sourceTypeName, false))
			.argument(GraphQLArgument.newArgument().name("_page").type(Scalars.GraphQLInt).build())
			.argument(GraphQLArgument.newArgument().name("_pageSize").type(Scalars.GraphQLInt).build())
			.argument(GraphQLArgument.newArgument().name("_sort").type(Scalars.GraphQLString).build())
			.argument(GraphQLArgument.newArgument().name("_desc").type(Scalars.GraphQLBoolean).build());

		// register reference type so that GraphQL can be used to query related nodes
		if (isMultiple(relationProperty, false)) {

			builder.argument(GraphQLArgument.newArgument().name("_contains").type(typeRef(GraphQLNodeReferenceName)).build());

		} else {

			builder.argument(GraphQLArgument.newArgument().name("_equals").type(typeRef(GraphQLNodeReferenceName)).build());
		}


		// register field
		fields.put(propertyName, builder.build());
	}

	private GraphQLOutputType getGraphQLTypeForCardinality(final RelationProperty property, final String targetTypeName, final boolean outgoing) {

		if (isMultiple(property, outgoing)) {
			return new GraphQLListType(typeRef(targetTypeName));
		}

		return typeRef(targetTypeName);
	}

	private GraphQLFieldDefinition getGraphQLField(final PropertyKey property) {

		final GraphQLOutputType outputType = getGraphQLOutputTypeForProperty(property);
		if (outputType != null) {

			return GraphQLFieldDefinition
				.newFieldDefinition()
				.name(SchemaHelper.cleanPropertyName(property.jsonName()))
				.type(outputType)
				.arguments(getGraphQLArgumentsForPropertyType(property))
				.build();
		}

		return null;
	}

	private List<GraphQLArgument> getGraphQLArgumentsForPropertyType(final PropertyKey property) {

		final List<GraphQLArgument> arguments = new LinkedList<>();

		switch (property.valueType().getSimpleName()) {

			case "String":
				arguments.add(GraphQLArgument.newArgument().name("_equals").type(Scalars.GraphQLString).build());
				arguments.add(GraphQLArgument.newArgument().name("_contains").type(Scalars.GraphQLString).build());
				arguments.add(GraphQLArgument.newArgument().name("_conj").type(Scalars.GraphQLString).build());
				break;

			case "Integer":
				arguments.add(GraphQLArgument.newArgument().name("_equals").type(Scalars.GraphQLInt).build());
				arguments.add(GraphQLArgument.newArgument().name("_conj").type(Scalars.GraphQLString).build());
				break;

			case "Long":
				arguments.add(GraphQLArgument.newArgument().name("_equals").type(Scalars.GraphQLLong).build());
				arguments.add(GraphQLArgument.newArgument().name("_conj").type(Scalars.GraphQLString).build());
				break;
		}

		return arguments;
	}

	private List<GraphQLArgument> getGraphQLArgumentsForUUID() {

		final List<GraphQLArgument> arguments = new LinkedList<>();

		arguments.add(GraphQLArgument.newArgument().name("_equals").type(Scalars.GraphQLString).build());

		return arguments;
	}

	private List<GraphQLArgument> getGraphQLArgumentsForRelatedType(final Class relatedType) {

		// related type parameter is unused right now
		final List<GraphQLArgument> arguments = new LinkedList<>();

		arguments.add(GraphQLArgument.newArgument().name("_equals").type(typeRef(GraphQLNodeReferenceName)).build());
		arguments.add(GraphQLArgument.newArgument().name("_sort").type(Scalars.GraphQLString).build());
		arguments.add(GraphQLArgument.newArgument().name("_desc").type(Scalars.GraphQLString).build());

		return arguments;
	}

	private List<String> getInterfaceClasses(final String type) {
		return Traits.of(type).getTraitDefinitions().stream().filter(t -> t.isInterface()).map(t -> t.getName()).toList();
	}

	private List<PropertyKey> getNonRelationshipProperties(final String graphObjectClass) {

		final List<PropertyKey> properties = new LinkedList<>();

		for (final PropertyKey key : Traits.getPropertiesOfTrait(graphObjectClass)) {

			if (key.relatedType() == null) {

				properties.add(key);
			}
		}

		return properties;
	}

	private List<Property> getRelationshipProperties(final String graphObjectClass, final boolean outgoing) throws IllegalAccessException {

		final List<Property> properties = new LinkedList<>();

		for (final PropertyKey key : Traits.getPropertiesOfTrait(graphObjectClass)) {

			if (key instanceof RelationProperty property) {

				final Relation relation = property.getRelation();
				if (outgoing) {

					final PropertyKey source = relation.getSourceProperty();

					if (source != null && source.equals(property)) {

						properties.add((Property)property);
					}

				} else {

					final PropertyKey target = relation.getTargetProperty();

					if (target != null && target.equals(property)) {

						properties.add((Property)property);
					}
				}
			}
		}

		return properties;
	}

	private boolean isMultiple(final RelationProperty property, final boolean outgoing) {

		final Relation relation          = property.getRelation();
		final Relation.Multiplicity mult = outgoing ? relation.getTargetMultiplicity() : relation.getSourceMultiplicity();

		return Relation.Multiplicity.Many.equals(mult);
	}

}
