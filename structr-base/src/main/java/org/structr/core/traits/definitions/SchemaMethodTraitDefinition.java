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
import org.structr.common.error.SemanticErrorToken;
import org.structr.common.event.RuntimeEventLog;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.Relation;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.function.Functions;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.notion.PropertySetNotion;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.core.traits.operations.graphobject.OnDeletion;
import org.structr.core.traits.operations.graphobject.OnModification;
import org.structr.core.traits.wrappers.SchemaMethodTraitWrapper;
import org.structr.schema.action.Actions;

import java.util.Map;
import java.util.Set;

/**
 *
 *
 */
public final class SchemaMethodTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String PARAMETERS_PROPERTY                 = "parameters";
	public static final String SCHEMA_NODE_PROPERTY                = "schemaNode";
	public static final String STATIC_SCHEMA_NODE_NAME_PROPERTY    = "staticSchemaNodeName";
	public static final String VIRTUAL_FILE_NAME_PROPERTY          = "virtualFileName";
	public static final String OPEN_API_RETURN_TYPE_PROPERTY       = "openAPIReturnType";
	public static final String SOURCE_PROPERTY                     = "source";
	public static final String EXCEPTIONS_PROPERTY                 = "exceptions";
	public static final String CALL_SUPER_PROPERTY                 = "callSuper";
	public static final String OVERRIDES_EXISTING_PROPERTY         = "overridesExisting";
	public static final String DO_EXPORT_PROPERTY                  = "doExport";
	public static final String CODE_TYPE_PROPERTY                  = "codeType";
	public static final String IS_PART_OF_BUILT_IN_SCHEMA_PROPERTY = "isPartOfBuiltInSchema";
	public static final String INCLUDE_IN_OPEN_API_PROPERTY        = "includeInOpenAPI";
	public static final String TAGS_PROPERTY                       = "tags";
	public static final String SUMMARY_PROPERTY                    = "summary";
	public static final String DESCRIPTION_PROPERTY                = "description";
	public static final String IS_STATIC_PROPERTY                  = "isStatic";
	public static final String IS_PRIVATE_PROPERTY                 = "isPrivate";
	public static final String RETURN_RAW_RESULT_PROPERTY          = "returnRawResult";
	public static final String HTTP_VERB_PROPERTY                  = "httpVerb";
	public static final String DELETE_METHOD_PROPERTY              = "deleteMethod";
	public static final String WRAP_JS_IN_MAIN_PROPERTY            = "wrapJsInMain";

	public SchemaMethodTraitDefinition() {
		super(StructrTraits.SCHEMA_METHOD);
	}

	@Override
	public Map<Class, LifecycleMethod> createLifecycleMethods(TraitsInstance traitsInstance) {

		return Map.of(

			IsValid.class,
			new IsValid() {

				@Override
				public Boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer) {

					final SchemaMethod method                      = obj.as(SchemaMethod.class);
					final AbstractSchemaNode schemaNode            = method.getSchemaNode();
					final Traits traits                            = obj.getTraits();
					final PropertyKey<NodeInterface> schemaNodeKey = traits.key(SCHEMA_NODE_PROPERTY);
					final PropertyKey<String> nameKey              = traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
					boolean valid                                  = true;

					valid &= ValidationHelper.isValidStringMatchingRegex(obj, nameKey, SchemaMethod.schemaMethodNamePattern,
						"Method name must match the following pattern: '" + SchemaMethod.schemaMethodNamePattern + "', which means it must start with a lowercase letter and may only contain letters, numbers and underscores.",
						errorBuffer);

					final Set<String> propertyViews = schemaNode != null ? schemaNode.getViewNames() : Set.of();
					final String thisMethodName     = method.getName();

					if (thisMethodName != null && propertyViews.contains(thisMethodName)) {
						errorBuffer.add(
							new SemanticErrorToken(method.getType(), "name", "already_exists")
								.withValue(thisMethodName)
								.withDetail("A view with name '" + thisMethodName + "' already exists, cannot create method with the same name")
						);
						valid = false;
					}

					// check case-insensitive name uniqueness on current level (type or user-defined functions)
					final AbstractSchemaNode parentOrNull = method.getSchemaNode();

					try {

						for (final NodeInterface otherSchemaMethodNode : StructrApp.getInstance().nodeQuery(StructrTraits.SCHEMA_METHOD).key(schemaNodeKey, parentOrNull).getResultStream()) {

							final boolean isDifferentMethod = !(method.getUuid().equals(otherSchemaMethodNode.getUuid()));

							if (isDifferentMethod) {

								final SchemaMethod otherSchemaMethod                = otherSchemaMethodNode.as(SchemaMethod.class);
								final boolean isSameNameIgnoringCase                = thisMethodName.equalsIgnoreCase(otherSchemaMethod.getName());

								if (isSameNameIgnoringCase) {

									errorBuffer.add(new SemanticErrorToken(method.getType(), "name", "already_exists").withValue(thisMethodName).withDetail("Multiple methods with identical names (case-insensitive) are not supported on the same level"));
									valid = false;
								}
							}
						}

					} catch (FrameworkException fex) {

						errorBuffer.add(new SemanticErrorToken(method.getType(),"none", "exception_occurred").withValue(thisMethodName).withDetail("Exception occurred while checking uniqueness of method name - please retry. Cause: " + fex.getMessage()));
						valid = false;
					}

					return valid;
				}
			},

			OnCreation.class,
			new OnCreation() {
				@Override
				public void onCreation(GraphObject graphObject, SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {

					final SchemaMethod schemaMethod = graphObject.as(SchemaMethod.class);

					schemaMethod.handleAutomaticCorrectionOfAttributes();

					if (schemaMethod.getSchemaNode() == null && Functions.getNames().contains(schemaMethod.getName())) {

						schemaMethod.warnAboutShadowingBuiltinFunction();
					}

					Actions.clearCache();
				}
			},

			OnModification.class,
			new OnModification() {
				@Override
				public void onModification(GraphObject graphObject, SecurityContext securityContext, ErrorBuffer errorBuffer, ModificationQueue modificationQueue) throws FrameworkException {

					final SchemaMethod schemaMethod = graphObject.as(SchemaMethod.class);
					final Traits traits             = graphObject.getTraits();

					if (Boolean.TRUE.equals(schemaMethod.getProperty(traits.key(DELETE_METHOD_PROPERTY)))) {

						StructrApp.getInstance().delete(schemaMethod);

					} else {

						schemaMethod.handleAutomaticCorrectionOfAttributes();

						if (schemaMethod.getSchemaNode() == null && Functions.getNames().contains(schemaMethod.getName()) && modificationQueue.isPropertyModified(graphObject, traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY))) {

							schemaMethod.warnAboutShadowingBuiltinFunction();
						}
					}

					// acknowledge all events for this node when it is modified
					RuntimeEventLog.acknowledgeAllEventsForId(schemaMethod.getUuid());

					// clear caches
					Actions.clearCache();
				}
			},

			OnDeletion.class,
			new OnDeletion() {

				@Override
				public void onDeletion(GraphObject graphObject, SecurityContext securityContext, ErrorBuffer errorBuffer, PropertyMap properties) throws FrameworkException {

					// clear caches
					Actions.clearCache();
				}
			}
		);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<Iterable<NodeInterface>> parameters         = new EndNodes(traitsInstance, PARAMETERS_PROPERTY, StructrTraits.SCHEMA_METHOD_PARAMETERS);
		final Property<NodeInterface>      schemaNode              = new StartNode(traitsInstance, SCHEMA_NODE_PROPERTY, StructrTraits.SCHEMA_NODE_METHOD, new PropertySetNotion<>(newSet(GraphObjectTraitDefinition.ID_PROPERTY, NodeInterfaceTraitDefinition.NAME_PROPERTY, AbstractSchemaNodeTraitDefinition.IS_SERVICE_CLASS_PROPERTY)));
		final Property<String>             staticSchemaNodeName    = new StringProperty(STATIC_SCHEMA_NODE_NAME_PROPERTY);
		final Property<String>             virtualFileName         = new StringProperty(VIRTUAL_FILE_NAME_PROPERTY).indexed();
		final Property<String>             openAPIReturnType       = new StringProperty(OPEN_API_RETURN_TYPE_PROPERTY).indexed();
		final Property<String>             source                  = new StringProperty(SOURCE_PROPERTY);
		final Property<String[]>           exceptions              = new ArrayProperty<>(EXCEPTIONS_PROPERTY, String.class).indexed();
		final Property<Boolean>            callSuper               = new BooleanProperty(CALL_SUPER_PROPERTY).indexed();
		final Property<Boolean>            overridesExisting       = new BooleanProperty(OVERRIDES_EXISTING_PROPERTY).indexed();
		final Property<Boolean>            doExport                = new BooleanProperty(DO_EXPORT_PROPERTY).indexed();
		final Property<String>             codeType                = new StringProperty(CODE_TYPE_PROPERTY).indexed();
		final Property<Boolean>            isPartOfBuiltInSchema   = new BooleanProperty(IS_PART_OF_BUILT_IN_SCHEMA_PROPERTY).indexed();
		final Property<Boolean>            includeInOpenAPI        = new BooleanProperty(INCLUDE_IN_OPEN_API_PROPERTY).indexed();
		final Property<String[]>           tags                    = new ArrayProperty<>(TAGS_PROPERTY, String.class).indexed();
		final Property<String>             summary                 = new StringProperty(SUMMARY_PROPERTY);
		final Property<String>             description             = new StringProperty(DESCRIPTION_PROPERTY);
		final Property<Boolean>            isStatic                = new BooleanProperty(IS_STATIC_PROPERTY).defaultValue(false);
		final Property<Boolean>            isPrivate               = new BooleanProperty(IS_PRIVATE_PROPERTY).defaultValue(false).indexed().indexedWhenEmpty();
		final Property<Boolean>            returnRawResult         = new BooleanProperty(RETURN_RAW_RESULT_PROPERTY).defaultValue(false);
		final Property<String>             httpVerb                = new EnumProperty(HTTP_VERB_PROPERTY, newSet("GET", "PUT", "POST", "PATCH", "DELETE")).defaultValue("POST");
		final Property<Boolean>            deleteMethod            = new BooleanProperty(DELETE_METHOD_PROPERTY).defaultValue(Boolean.FALSE);
		final Property<Boolean>            wrapJsInMain            = new BooleanProperty(WRAP_JS_IN_MAIN_PROPERTY).defaultValue(Boolean.TRUE);

		return newSet(
			parameters,
			schemaNode,
			staticSchemaNodeName,
			virtualFileName,
			openAPIReturnType,
			source,
			exceptions,
			callSuper,
			overridesExisting,
			doExport,
			codeType,
			isPartOfBuiltInSchema,
			includeInOpenAPI,
			tags,
			summary,
			description,
			isStatic,
			isPrivate,
			returnRawResult,
			httpVerb,
			deleteMethod,
			wrapJsInMain
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(

			PropertyView.Public,
			newSet(
					SCHEMA_NODE_PROPERTY, STATIC_SCHEMA_NODE_NAME_PROPERTY, SOURCE_PROPERTY,
					EXCEPTIONS_PROPERTY, CALL_SUPER_PROPERTY, OVERRIDES_EXISTING_PROPERTY, DO_EXPORT_PROPERTY, CODE_TYPE_PROPERTY,
					IS_PART_OF_BUILT_IN_SCHEMA_PROPERTY, TAGS_PROPERTY, SUMMARY_PROPERTY, DESCRIPTION_PROPERTY, IS_STATIC_PROPERTY,
					INCLUDE_IN_OPEN_API_PROPERTY, OPEN_API_RETURN_TYPE_PROPERTY,
					IS_PRIVATE_PROPERTY, RETURN_RAW_RESULT_PROPERTY, HTTP_VERB_PROPERTY, WRAP_JS_IN_MAIN_PROPERTY
			),

			PropertyView.Ui,
			newSet(
					SCHEMA_NODE_PROPERTY, STATIC_SCHEMA_NODE_NAME_PROPERTY, SOURCE_PROPERTY,
					EXCEPTIONS_PROPERTY, CALL_SUPER_PROPERTY, OVERRIDES_EXISTING_PROPERTY, DO_EXPORT_PROPERTY, CODE_TYPE_PROPERTY,
					IS_PART_OF_BUILT_IN_SCHEMA_PROPERTY, TAGS_PROPERTY, SUMMARY_PROPERTY, DESCRIPTION_PROPERTY, IS_STATIC_PROPERTY,
					INCLUDE_IN_OPEN_API_PROPERTY, OPEN_API_RETURN_TYPE_PROPERTY,
					IS_PRIVATE_PROPERTY, RETURN_RAW_RESULT_PROPERTY, HTTP_VERB_PROPERTY, WRAP_JS_IN_MAIN_PROPERTY
			),

			PropertyView.Schema,
			newSet(
					GraphObjectTraitDefinition.ID_PROPERTY, GraphObjectTraitDefinition.TYPE_PROPERTY, NodeInterfaceTraitDefinition.NAME_PROPERTY,
					SCHEMA_NODE_PROPERTY, STATIC_SCHEMA_NODE_NAME_PROPERTY, SOURCE_PROPERTY,
					EXCEPTIONS_PROPERTY, CALL_SUPER_PROPERTY, OVERRIDES_EXISTING_PROPERTY, DO_EXPORT_PROPERTY, CODE_TYPE_PROPERTY,
					IS_PART_OF_BUILT_IN_SCHEMA_PROPERTY, TAGS_PROPERTY, SUMMARY_PROPERTY, DESCRIPTION_PROPERTY, IS_STATIC_PROPERTY,
					INCLUDE_IN_OPEN_API_PROPERTY, OPEN_API_RETURN_TYPE_PROPERTY,
					IS_PRIVATE_PROPERTY, RETURN_RAW_RESULT_PROPERTY, HTTP_VERB_PROPERTY, WRAP_JS_IN_MAIN_PROPERTY,
					PARAMETERS_PROPERTY
			)
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			SchemaMethod.class, (traits, node) -> new SchemaMethodTraitWrapper(traits, node)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}

}
