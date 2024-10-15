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
import org.structr.core.GraphObject;
import org.structr.core.entity.*;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graphql.GraphQLListType;
import org.structr.core.property.*;

import java.lang.reflect.Modifier;
import java.util.*;

import static graphql.schema.GraphQLTypeReference.typeRef;

public class GraphQLHelper {

	private final Map<Class, GraphQLScalarType> graphQLTypeMap    = new LinkedHashMap<>();
	private final Map<Class, List<GraphQLInputObjectField>> cache = new LinkedHashMap<>();
	public static final String GraphQLNodeReferenceName           = "StructrNodeReference";

	public static final Set<Class> InternalTypes = Set.of(
		AbstractNode.class, AbstractRelationship.class,
		OneToOne.class, OneToMany.class, ManyToOne.class, ManyToMany.class,
		LinkedTreeNode.class, LinkedListNode.class

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

	public void initializeGraphQLForNodeType(final Class nodeType, final Map<String, GraphQLType> graphQLTypes, final Set<String> blacklist) throws IllegalAccessException {

		// check if some base class already initialized us
		if (graphQLTypes.containsKey(nodeType.getSimpleName())) {

			// nothing to do
			return;
		}

		// register node reference type to filter related nodes
		if (!graphQLTypes.containsKey(GraphQLNodeReferenceName)) {

			graphQLTypes.put(GraphQLNodeReferenceName, GraphQLInputObjectType.newInputObject()
				.name(GraphQLNodeReferenceName)
				.field(GraphQLInputObjectField.newInputObjectField().name("id").type(Scalars.GraphQLString).build())
				.field(GraphQLInputObjectField.newInputObjectField().name("name").type(Scalars.GraphQLString).build())
				.build());
		}

		// variables
		final Map<String, GraphQLFieldDefinition> fields = new LinkedHashMap<>();
		final String className                           = nodeType.getSimpleName();
		final Class parentType                           = nodeType.getSuperclass();

		if (parentType != null && !InternalTypes.contains(parentType) && GraphObject.class.isAssignableFrom(parentType)) {

			// add inherited fields from superclass
			registerParentType(parentType, graphQLTypes, fields, blacklist);
		}

		// add inherited fields from interfaces
		for (final Class ifaceType : getInterfaceClasses(nodeType)) {

			if (!InternalTypes.contains(ifaceType)) {

				registerParentType(ifaceType, graphQLTypes, fields, blacklist);
			}
		}

		// add dynamic fields
		for (final Property property : getNonRelationshipProperties(nodeType)) {

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
		fields.putIfAbsent("name", GraphQLFieldDefinition.newFieldDefinition().name("name").type(Scalars.GraphQLString).arguments(getGraphQLArgumentsForPropertyType(AbstractNode.name)).build());
		fields.putIfAbsent("owner", GraphQLFieldDefinition.newFieldDefinition().name("owner").type(typeRef("Principal")).arguments(getGraphQLArgumentsForRelatedType(PrincipalInterface.class)).build());
		fields.putIfAbsent("createdBy", GraphQLFieldDefinition.newFieldDefinition().name("createdBy").type(Scalars.GraphQLString).build());
		fields.putIfAbsent("createdDate", GraphQLFieldDefinition.newFieldDefinition().name("createdDate").type(Scalars.GraphQLString).build());
		fields.putIfAbsent("lastModifiedBy", GraphQLFieldDefinition.newFieldDefinition().name("lastModifiedBy").type(Scalars.GraphQLString).build());
		fields.putIfAbsent("lastModifiedDate", GraphQLFieldDefinition.newFieldDefinition().name("lastModifiedDate").type(Scalars.GraphQLString).build());
		fields.putIfAbsent("visibleToPublicUsers", GraphQLFieldDefinition.newFieldDefinition().name("visibleToPublicUsers").type(Scalars.GraphQLString).build());
		fields.putIfAbsent("visibleToAuthenticatedUsers", GraphQLFieldDefinition.newFieldDefinition().name("visibleToAuthenticatedUsers").type(Scalars.GraphQLString).build());

		// register type in GraphQL schema
		final GraphQLObjectType.Builder builder = GraphQLObjectType.newObject();

		builder.name(className);
		builder.fields(new LinkedList<>(fields.values()));

		graphQLTypes.put(className, builder.build());
	}

	public void initializeGraphQLForRelationshipType(final Class relType, final Map<String, GraphQLType> graphQLTypes) throws IllegalAccessException {

		final List<GraphQLFieldDefinition> fields = new LinkedList<>();
		final String className                    = relType.getSimpleName();

		// add static fields (id, name etc.)
		fields.add(GraphQLFieldDefinition.newFieldDefinition().name("id").type(Scalars.GraphQLString).build());
		fields.add(GraphQLFieldDefinition.newFieldDefinition().name("type").type(Scalars.GraphQLString).build());

		// add dynamic fields
		for (final Property property : getNonRelationshipProperties(relType)) {

			final GraphQLFieldDefinition field = getGraphQLField(property);
			if (field != null) {

				fields.add(field);
			}
		}

		final GraphQLObjectType graphQLType = GraphQLObjectType.newObject().name(relType.getSimpleName()).fields(fields).build();

		// register type in GraphQL schema
		graphQLTypes.put(className, graphQLType);
	}

	private GraphQLOutputType getGraphQLOutputTypeForProperty(final Property property) {

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

	private GraphQLInputType getGraphQLInputTypeForProperty(final Property property) {

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

	public List<GraphQLArgument> getGraphQLQueryArgumentsForType(final Map<String, GraphQLInputObjectType> selectionTypes, final Set<String> queryTypeNames, final Class type) throws FrameworkException, IllegalAccessException {

		final List<GraphQLArgument> arguments = new LinkedList<>();

		// register parent type arguments as well!
		final Class parentType = type.getSuperclass();

		if (parentType != null && !InternalTypes.contains(parentType) && !parentType.equals(type) && GraphObject.class.isAssignableFrom(parentType)) {

			arguments.addAll(getGraphQLQueryArgumentsForType(selectionTypes, queryTypeNames, parentType));
		}

		// outgoing relationships
		for (final Property outProperty : getRelationshipProperties(type, true)) {

			final RelationProperty relationProperty = (RelationProperty) outProperty;
			final Class targetType                  = relationProperty.getTargetType();
			final String targetTypeName             = targetType.getSimpleName();
			final String propertyName               = outProperty.jsonName();
			final String queryTypeName              = type.getSimpleName() + propertyName + targetTypeName + "InInput";

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
			final Class sourceType                  = relationProperty.getTargetType();
			final String sourceTypeName             = sourceType.getSimpleName();
			final String propertyName               = inProperty.jsonName();
			final String queryTypeName              = type.getSimpleName() + propertyName + sourceTypeName + "OutInput";

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
		for (final Property property : getNonRelationshipProperties(type)) {

			if (property.isIndexed() || property.isCompound()) {

				final String name          = property.jsonName();
				final String selectionName = type.getSimpleName() + name + "Selection";

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

		final String ownerTypeName = type.getSimpleName() + "ownerInput";

		if (!queryTypeNames.contains(ownerTypeName)) {

			// manual registration for built-in relationships that are not dynamic
			arguments.add(GraphQLArgument.newArgument().name("owner").type(GraphQLInputObjectType.newInputObject()
				.name(ownerTypeName)
				.fields(getGraphQLInputFieldsForType(selectionTypes, PrincipalInterface.class))
				.build()
			).build());

			queryTypeNames.add(ownerTypeName);
		}

		return arguments;
	}

	private List<GraphQLInputObjectField> getGraphQLInputFieldsForType(final Map<String, GraphQLInputObjectType> selectionTypes, final Class type) throws IllegalAccessException {

		List<GraphQLInputObjectField> data = cache.get(type);

		if (data == null) {

			final Map<String, GraphQLInputObjectField> fields = new LinkedHashMap<>();

			// register parent type arguments as well!
			final Class parentType = type.getSuperclass();

			if (parentType != null && !InternalTypes.contains(parentType) && !parentType.equals(type) && GraphObject.class.isAssignableFrom(parentType)) {

				for (final GraphQLInputObjectField field : getGraphQLInputFieldsForType(selectionTypes, parentType)) {

					fields.put(field.getName(), field);
				}
			}

			for (final Property property : getNonRelationshipProperties(type)) {

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

	private void registerParentType(final Class parentType, final Map<String, GraphQLType> graphQLTypes, final Map<String, GraphQLFieldDefinition> fields, final Set<String> blacklist) throws IllegalAccessException {

		if (parentType != null && !AbstractNode.class.equals(parentType) && !parentType.equals(this)) {

			final String parentName = parentType.getSimpleName();
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
		final String targetTypeName             = relationProperty.getTargetType().getSimpleName();

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
		final String sourceTypeName             = relationProperty.getTargetType().getSimpleName();

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

	private GraphQLFieldDefinition getGraphQLField(final Property property) {

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

	private List<GraphQLArgument> getGraphQLArgumentsForPropertyType(final Property property) {

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

	private List<Class> getInterfaceClasses(final Class type) {
		return Arrays.stream(type.getInterfaces()).filter(c -> NodeInterface.class.isAssignableFrom(c)).toList();
	}

	private List<Property> getNonRelationshipProperties(final Class graphObjectClass) throws IllegalAccessException {

		final List<Property> properties = new LinkedList<>();

		for (final java.lang.reflect.Field field : graphObjectClass.getDeclaredFields()) {

			final int modifiers = field.getModifiers();

			if (Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers) && !Modifier.isPrivate(modifiers)) {

				field.setAccessible(true);
				final Object value    = field.get(null);
				final Class type      = value.getClass();

				// fetch non-relationship properties only
				if (Property.class.isAssignableFrom(type) && !RelationProperty.class.isAssignableFrom(type)) {

					properties.add((Property)value);
				}
			}
		}

		return properties;
	}

	private List<Property> getRelationshipProperties(final Class graphObjectClass, final boolean outgoing) throws IllegalAccessException {

		final List<Property> properties = new LinkedList<>();

		for (final java.lang.reflect.Field field : graphObjectClass.getDeclaredFields()) {

			final int modifiers = field.getModifiers();

			if (Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers) && !Modifier.isPrivate(modifiers)) {

				field.setAccessible(true);

				final Object value    = field.get(null);
				final Class type      = value.getClass();

				if (RelationProperty.class.isAssignableFrom(type)) {

					final RelationProperty property = (RelationProperty)value;
					final Relation relation         = property.getRelation();

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
		}

		return properties;
	}

	private boolean isMultiple(final RelationProperty property, final boolean outgoing) {

		final Relation relation          = property.getRelation();
		final Relation.Multiplicity mult = outgoing ? relation.getTargetMultiplicity() : relation.getSourceMultiplicity();

		return Relation.Multiplicity.Many.equals(mult);
	}

}
