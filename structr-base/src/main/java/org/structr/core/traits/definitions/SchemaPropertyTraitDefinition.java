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

import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.entity.Relation;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.notion.PropertySetNotion;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.core.traits.operations.graphobject.OnModification;
import org.structr.core.traits.wrappers.SchemaPropertyTraitWrapper;
import org.structr.schema.ReloadSchema;

import java.util.Map;
import java.util.Set;

public class SchemaPropertyTraitDefinition extends AbstractNodeTraitDefinition {

	private static final String schemaPropertyNamePattern = "[a-z][\\-_0-9A-Za-z]*";

	public static final String SCHEMA_NODE_PROPERTY                = "schemaNode";
	public static final String SCHEMA_VIEWS_PROPERTY               = "schemaViews";
	public static final String EXCLUDED_VIEWS_PROPERTY             = "excludedViews";
	public static final String DECLARING_UUID_PROPERTY             = "declaringUuid";
	public static final String STATIC_SCHEMA_NODE_NAME_PROPERTY    = "staticSchemaNodeName";
	public static final String DECLARING_CLASS_PROPERTY            = "declaringClass";
	public static final String DEFAULT_VALUE_PROPERTY              = "defaultValue";
	public static final String PROPERTY_TYPE_PROPERTY              = "propertyType";
	public static final String DB_NAME_PROPERTY                    = "dbName";
	public static final String FQCN_PROPERTY                       = "fqcn";
	public static final String FORMAT_PROPERTY                     = "format";
	public static final String TYPE_HINT_PROPERTY                  = "typeHint";
	public static final String HINT_PROPERTY                       = "hint";
	public static final String CATEGORY_PROPERTY                   = "category";
	public static final String NOT_NULL_PROPERTY                   = "notNull";
	public static final String COMPOUND_PROPERTY                   = "compound";
	public static final String UNIQUE_PROPERTY                     = "unique";
	public static final String INDEXED_PROPERTY                    = "indexed";
	public static final String FULLTEXT_PROPERTY                   = "fulltext";
	public static final String READ_ONLY_PROPERTY                  = "readOnly";
	public static final String IS_ABSTRACT_PROPERTY                = "isAbstract";
	public static final String IS_DYNAMIC_PROPERTY                 = "isDynamic";
	public static final String IS_BUILTIN_PROPERTY_PROPERTY        = "isBuiltinProperty";
	public static final String IS_PART_OF_BUILT_IN_SCHEMA_PROPERTY = "isPartOfBuiltInSchema";
	public static final String IS_CACHING_ENABLED_PROPERTY         = "isCachingEnabled";
	public static final String CONTENT_HASH_PROPERTY               = "contentHash";
	public static final String READ_FUNCTION_PROPERTY              = "readFunction";
	public static final String WRITE_FUNCTION_PROPERTY             = "writeFunction";
	public static final String WRITE_FUNCTION_WRAP_JS_PROPERTY     = "writeFunctionWrapJS";
	public static final String READ_FUNCTION_WRAP_JS_PROPERTY      = "readFunctionWrapJS";
	public static final String IS_SERIALIZATION_DISABLED_PROPERTY  = "isSerializationDisabled";
	public static final String OPEN_API_RETURN_TYPE_PROPERTY       = "openAPIReturnType";
	public static final String VALIDATORS_PROPERTY                 = "validators";
	public static final String TRANSFORMERS_PROPERTY               = "transformers";


	public SchemaPropertyTraitDefinition() {
		super(StructrTraits.SCHEMA_PROPERTY);
	}

	@Override
	public Map<Class, LifecycleMethod> createLifecycleMethods(TraitsInstance traitsInstance) {

		return Map.of(

			IsValid.class,
			new IsValid() {

				@Override
				public Boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer) {
					return ValidationHelper.isValidStringMatchingRegex(obj, Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),
						schemaPropertyNamePattern,
						"Property name must match the following pattern: '" + schemaPropertyNamePattern + "', which means it must begin with a lowercase letter and may only contain letters, numbers, underscores and hyphens.",
						errorBuffer);
				}
			},

			OnCreation.class,
			new OnCreation() {

				@Override
				public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {
					TransactionCommand.postProcess("reloadSchema", new ReloadSchema(true));
				}
			},

			OnModification.class,
			new OnModification() {

				@Override
				public void onModification(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {
					TransactionCommand.postProcess("reloadSchema", new ReloadSchema(true));
				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			SchemaProperty.class, (traits, node) -> new SchemaPropertyTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<NodeInterface>           schemaNode    = new StartNode(traitsInstance, SCHEMA_NODE_PROPERTY, StructrTraits.SCHEMA_NODE_PROPERTY, new PropertySetNotion<>(newSet(GraphObjectTraitDefinition.ID_PROPERTY, NodeInterfaceTraitDefinition.NAME_PROPERTY)));
		final Property<Iterable<NodeInterface>> schemaViews   = new StartNodes(traitsInstance, SCHEMA_VIEWS_PROPERTY, StructrTraits.SCHEMA_VIEW_PROPERTY, new PropertySetNotion<>(newSet(GraphObjectTraitDefinition.ID_PROPERTY, NodeInterfaceTraitDefinition.NAME_PROPERTY)));
		final Property<Iterable<NodeInterface>> excludedViews = new StartNodes(traitsInstance, EXCLUDED_VIEWS_PROPERTY, StructrTraits.SCHEMA_EXCLUDED_VIEW_PROPERTY, new PropertySetNotion<>(newSet(GraphObjectTraitDefinition.ID_PROPERTY, NodeInterfaceTraitDefinition.NAME_PROPERTY)));

		final Property<String>             declaringUuid           = new StringProperty(DECLARING_UUID_PROPERTY);
		final Property<String>             staticSchemaNodeName    = new StringProperty(STATIC_SCHEMA_NODE_NAME_PROPERTY);
		final Property<String>             declaringClass          = new StringProperty(DECLARING_CLASS_PROPERTY);
		final Property<String>             defaultValue            = new StringProperty(DEFAULT_VALUE_PROPERTY);
		final Property<String>             propertyType            = new StringProperty(PROPERTY_TYPE_PROPERTY).indexed();
		final Property<String>             dbName                  = new StringProperty(DB_NAME_PROPERTY);
		final Property<String>             fqcn                    = new StringProperty(FQCN_PROPERTY);
		final Property<String>             format                  = new StringProperty(FORMAT_PROPERTY);
		final Property<String>             typeHint                = new StringProperty(TYPE_HINT_PROPERTY);
		final Property<String>             hint                    = new StringProperty(HINT_PROPERTY);
		final Property<String>             category                = new StringProperty(CATEGORY_PROPERTY);
		final Property<Boolean>            notNull                 = new BooleanProperty(NOT_NULL_PROPERTY);
		final Property<Boolean>            compound                = new BooleanProperty(COMPOUND_PROPERTY);
		final Property<Boolean>            unique                  = new BooleanProperty(UNIQUE_PROPERTY);
		final Property<Boolean>            indexed                 = new BooleanProperty(INDEXED_PROPERTY);
		final Property<Boolean>            fulltext                = new BooleanProperty(FULLTEXT_PROPERTY);
		final Property<Boolean>            readOnly                = new BooleanProperty(READ_ONLY_PROPERTY);
		final Property<Boolean>            isAbstract              = new BooleanProperty(IS_ABSTRACT_PROPERTY);
		final Property<Boolean>            isDynamic               = new BooleanProperty(IS_DYNAMIC_PROPERTY);
		final Property<Boolean>            isBuiltinProperty       = new BooleanProperty(IS_BUILTIN_PROPERTY_PROPERTY);
		final Property<Boolean>            isPartOfBuiltInSchema   = new BooleanProperty(IS_PART_OF_BUILT_IN_SCHEMA_PROPERTY);
		final Property<Boolean>            isCachingEnabled        = new BooleanProperty(IS_CACHING_ENABLED_PROPERTY).defaultValue(false);
		final Property<String>             contentHash             = new StringProperty(CONTENT_HASH_PROPERTY);
		final Property<String>             readFunction            = new StringProperty(READ_FUNCTION_PROPERTY);
		final Property<String>             writeFunction           = new StringProperty(WRITE_FUNCTION_PROPERTY);
		final Property<Boolean>            writeFunctionWrapJS     = new BooleanProperty(WRITE_FUNCTION_WRAP_JS_PROPERTY).defaultValue(true);
		final Property<Boolean>            readFunctionWrapJS      = new BooleanProperty(READ_FUNCTION_WRAP_JS_PROPERTY).defaultValue(true);
		final Property<Boolean>            isSerializationDisabled = new BooleanProperty(IS_SERIALIZATION_DISABLED_PROPERTY);
		final Property<String>             openAPIReturnType       = new StringProperty(OPEN_API_RETURN_TYPE_PROPERTY);
		final Property<String[]>           validators              = new ArrayProperty(VALIDATORS_PROPERTY, String.class);
		final Property<String[]>           transformers            = new ArrayProperty(TRANSFORMERS_PROPERTY, String.class);

		return newSet(

			schemaNode,
			staticSchemaNodeName,
			schemaViews,
			excludedViews,
			declaringUuid,
			declaringClass,
			defaultValue,
			propertyType,
			dbName,
			fqcn,
			format,
			typeHint,
			hint,
			category,
			notNull,
			compound,
			unique,
			indexed,
			fulltext,
			readOnly,
			isAbstract,
			isDynamic,
			isBuiltinProperty,
			isPartOfBuiltInSchema,
			isCachingEnabled,
			contentHash,
			readFunction,
			writeFunction,
			readFunctionWrapJS,
			writeFunctionWrapJS,
			isSerializationDisabled,
			openAPIReturnType,
			validators,
			transformers
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(

				PropertyView.Public,
				newSet(
						DB_NAME_PROPERTY, SCHEMA_NODE_PROPERTY, SCHEMA_VIEWS_PROPERTY, EXCLUDED_VIEWS_PROPERTY, PROPERTY_TYPE_PROPERTY,
						FORMAT_PROPERTY, FQCN_PROPERTY, TYPE_HINT_PROPERTY, HINT_PROPERTY, CATEGORY_PROPERTY,
						NOT_NULL_PROPERTY, COMPOUND_PROPERTY, UNIQUE_PROPERTY, INDEXED_PROPERTY, FULLTEXT_PROPERTY, READ_ONLY_PROPERTY,
						DEFAULT_VALUE_PROPERTY, IS_BUILTIN_PROPERTY_PROPERTY, DECLARING_CLASS_PROPERTY, IS_ABSTRACT_PROPERTY, IS_DYNAMIC_PROPERTY,
						READ_FUNCTION_PROPERTY, WRITE_FUNCTION_PROPERTY, READ_FUNCTION_WRAP_JS_PROPERTY, WRITE_FUNCTION_WRAP_JS_PROPERTY,
						IS_SERIALIZATION_DISABLED_PROPERTY, OPEN_API_RETURN_TYPE_PROPERTY, VALIDATORS_PROPERTY, TRANSFORMERS_PROPERTY, IS_CACHING_ENABLED_PROPERTY
				),

				PropertyView.Ui,
				newSet(
						DB_NAME_PROPERTY, SCHEMA_NODE_PROPERTY, SCHEMA_VIEWS_PROPERTY, EXCLUDED_VIEWS_PROPERTY, PROPERTY_TYPE_PROPERTY,
						FORMAT_PROPERTY, FQCN_PROPERTY, TYPE_HINT_PROPERTY, HINT_PROPERTY, CATEGORY_PROPERTY,
						NOT_NULL_PROPERTY, COMPOUND_PROPERTY, UNIQUE_PROPERTY, INDEXED_PROPERTY, FULLTEXT_PROPERTY, READ_ONLY_PROPERTY,
						DEFAULT_VALUE_PROPERTY, IS_BUILTIN_PROPERTY_PROPERTY, DECLARING_CLASS_PROPERTY, IS_ABSTRACT_PROPERTY, IS_DYNAMIC_PROPERTY,
						READ_FUNCTION_PROPERTY, WRITE_FUNCTION_PROPERTY, READ_FUNCTION_WRAP_JS_PROPERTY, WRITE_FUNCTION_WRAP_JS_PROPERTY,
						IS_SERIALIZATION_DISABLED_PROPERTY, OPEN_API_RETURN_TYPE_PROPERTY, VALIDATORS_PROPERTY, TRANSFORMERS_PROPERTY, IS_CACHING_ENABLED_PROPERTY
				),

				PropertyView.Schema,
				newSet(
						GraphObjectTraitDefinition.ID_PROPERTY, GraphObjectTraitDefinition.TYPE_PROPERTY, NodeInterfaceTraitDefinition.NAME_PROPERTY,
						DB_NAME_PROPERTY, SCHEMA_NODE_PROPERTY, SCHEMA_VIEWS_PROPERTY, EXCLUDED_VIEWS_PROPERTY, PROPERTY_TYPE_PROPERTY,
						FORMAT_PROPERTY, FQCN_PROPERTY, TYPE_HINT_PROPERTY, HINT_PROPERTY, CATEGORY_PROPERTY,
						NOT_NULL_PROPERTY, COMPOUND_PROPERTY, UNIQUE_PROPERTY, INDEXED_PROPERTY, FULLTEXT_PROPERTY, READ_ONLY_PROPERTY,
						DEFAULT_VALUE_PROPERTY, IS_BUILTIN_PROPERTY_PROPERTY, DECLARING_CLASS_PROPERTY, IS_ABSTRACT_PROPERTY, IS_DYNAMIC_PROPERTY,
						READ_FUNCTION_PROPERTY, WRITE_FUNCTION_PROPERTY, READ_FUNCTION_WRAP_JS_PROPERTY, WRITE_FUNCTION_WRAP_JS_PROPERTY,
						IS_SERIALIZATION_DISABLED_PROPERTY, OPEN_API_RETURN_TYPE_PROPERTY, VALIDATORS_PROPERTY, TRANSFORMERS_PROPERTY, IS_CACHING_ENABLED_PROPERTY
				)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
