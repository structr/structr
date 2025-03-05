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
import org.structr.common.error.SemanticErrorToken;
import org.structr.common.event.RuntimeEventLog;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.Relation;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.SchemaMethodParameter;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.notion.PropertySetNotion;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.core.traits.operations.graphobject.OnDeletion;
import org.structr.core.traits.operations.graphobject.OnModification;
import org.structr.core.traits.wrappers.SchemaMethodTraitWrapper;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 *
 */
public final class SchemaMethodTraitDefinition extends AbstractNodeTraitDefinition {

	/*
	private static final Set<PropertyKey> schemaRebuildTriggerKeys = new LinkedHashSet<>(Arrays.asList(
		name, schemaNode, staticSchemaNodeName, exceptions, callSuper, overridesExisting, doExport, codeType, isPartOfBuiltInSchema, isStatic, isPrivate, returnRawResult, httpVerb
	));
	*/

	public static final String PARAMETERS_PROPERTY                 = "parameters";
	public static final String SCHEMA_NODE_PROPERTY                = "schemaNode";
	public static final String STATIC_SCHEMA_NODE_NAME_PROPERTY    = "staticSchemaNodeName";
	public static final String SIGNATURE_PROPERTY                  = "signature";
	public static final String VIRTUAL_FILE_NAME_PROPERTY          = "virtualFileName";
	public static final String RETURN_TYPE_PROPERTY                = "returnType";
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

	public SchemaMethodTraitDefinition() {
		super(StructrTraits.SCHEMA_METHOD);
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

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

					valid &= ValidationHelper.isValidStringMatchingRegex(obj, nameKey, SchemaMethod.schemaMethodNamePattern, errorBuffer);

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

						final List<NodeInterface> methodsOnCurrentLevel = StructrApp.getInstance().nodeQuery(StructrTraits.SCHEMA_METHOD).and(schemaNodeKey, parentOrNull).getAsList();
						final List<SchemaMethodParameter> params        = Iterables.toList(method.getParameters());
						// param comparison is required because otherwise this would fail for at least "getScaledImage" and "updateFeedTask"
						final String paramsAsString                     = params.stream().map(p -> p.getName() + ":" + p.getParameterType()).collect(Collectors.joining(";"));

						for (final NodeInterface otherSchemaMethodNode : methodsOnCurrentLevel) {

							final boolean isDifferentMethod = !(method.getUuid().equals(otherSchemaMethodNode.getUuid()));

							if (isDifferentMethod) {

								final SchemaMethod otherSchemaMethod                = otherSchemaMethodNode.as(SchemaMethod.class);
								final boolean isSameNameIgnoringCase                = thisMethodName.equalsIgnoreCase(otherSchemaMethod.getName());
								final List<SchemaMethodParameter> otherMethodParams = Iterables.toList(otherSchemaMethod.getParameters());
								final String otherParamsAsString                    = otherMethodParams.stream().map(p -> p.getName() + ":" + p.getParameterType()).collect(Collectors.joining(";"));

								final boolean hasSameParameters = (params.size() == otherMethodParams.size() && paramsAsString.equals(otherParamsAsString));

								if (isSameNameIgnoringCase && hasSameParameters) {

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
					}

					final String uuid = schemaMethod.getUuid();
					if (uuid != null) {

						// acknowledge all events for this node when it is modified
						RuntimeEventLog.getEvents(e -> uuid.equals(e.getData().get("id"))).stream().forEach(e -> e.acknowledge());
					}

					// FIXME: need to clear schema method cache if this method was deleted (see Actions.methodCache)
//					// Ensure AbstractSchemaNode methodCache is invalidated when a schema method changes
//					if (!TransactionCommand.isDeleted(getNode())) {
//
//						final AbstractSchemaNode schemaNode = getProperty(SchemaMethod.schemaNode);
//						if (schemaNode != null) {
//
//							schemaNode.clearCachedSchemaMethodsForInstance();
//
//							this.clearMethodCacheOfExtendingNodes();
//						}
//					}
				}
			},

			OnDeletion.class,
			new OnDeletion() {

				@Override
				public void onDeletion(GraphObject graphObject, SecurityContext securityContext, ErrorBuffer errorBuffer, PropertyMap properties) throws FrameworkException {

					// FIXME: need to clear schema method cache (see Actions.methodCache)
//					super.onNodeDeletion(securityContext);
//					AbstractSchemaNode.clearCachedSchemaMethods();
				}
			}
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<Iterable<NodeInterface>> parameters         = new EndNodes(PARAMETERS_PROPERTY, StructrTraits.SCHEMA_METHOD_PARAMETERS);
		final Property<NodeInterface>      schemaNode              = new StartNode(SCHEMA_NODE_PROPERTY, StructrTraits.SCHEMA_NODE_METHOD, new PropertySetNotion<>(newSet(GraphObjectTraitDefinition.ID_PROPERTY, NodeInterfaceTraitDefinition.NAME_PROPERTY, AbstractSchemaNodeTraitDefinition.IS_SERVICE_CLASS_PROPERTY)));
		final Property<String>             staticSchemaNodeName    = new StringProperty(STATIC_SCHEMA_NODE_NAME_PROPERTY);
		final Property<String>             signature               = new StringProperty(SIGNATURE_PROPERTY).indexed();
		final Property<String>             virtualFileName         = new StringProperty(VIRTUAL_FILE_NAME_PROPERTY).indexed();
		final Property<String>             returnType              = new StringProperty(RETURN_TYPE_PROPERTY).indexed();
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

		return newSet(
			parameters,
			schemaNode,
			staticSchemaNodeName,
			signature,
			virtualFileName,
			returnType,
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
			deleteMethod
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(

			PropertyView.Public,
			newSet(
				NodeInterfaceTraitDefinition.NAME_PROPERTY, SCHEMA_NODE_PROPERTY, STATIC_SCHEMA_NODE_NAME_PROPERTY,
					SOURCE_PROPERTY, RETURN_TYPE_PROPERTY, EXCEPTIONS_PROPERTY, CALL_SUPER_PROPERTY, OVERRIDES_EXISTING_PROPERTY,
					DO_EXPORT_PROPERTY, CODE_TYPE_PROPERTY, IS_PART_OF_BUILT_IN_SCHEMA_PROPERTY, TAGS_PROPERTY, SUMMARY_PROPERTY,
					DESCRIPTION_PROPERTY, IS_STATIC_PROPERTY, IS_PRIVATE_PROPERTY, RETURN_RAW_RESULT_PROPERTY, HTTP_VERB_PROPERTY
			),

			PropertyView.Ui,
			newSet(
					NodeInterfaceTraitDefinition.NAME_PROPERTY, SCHEMA_NODE_PROPERTY, STATIC_SCHEMA_NODE_NAME_PROPERTY,
					SOURCE_PROPERTY, RETURN_TYPE_PROPERTY, EXCEPTIONS_PROPERTY, CALL_SUPER_PROPERTY, OVERRIDES_EXISTING_PROPERTY,
					DO_EXPORT_PROPERTY, CODE_TYPE_PROPERTY, IS_PART_OF_BUILT_IN_SCHEMA_PROPERTY, TAGS_PROPERTY, SUMMARY_PROPERTY,
					DESCRIPTION_PROPERTY, IS_STATIC_PROPERTY, INCLUDE_IN_OPEN_API_PROPERTY, OPEN_API_RETURN_TYPE_PROPERTY,
					IS_PRIVATE_PROPERTY, RETURN_RAW_RESULT_PROPERTY, HTTP_VERB_PROPERTY
			),

			"schema",
			newSet(
				GraphObjectTraitDefinition.ID_PROPERTY, GraphObjectTraitDefinition.TYPE_PROPERTY, SCHEMA_NODE_PROPERTY,
					STATIC_SCHEMA_NODE_NAME_PROPERTY, NodeInterfaceTraitDefinition.NAME_PROPERTY, SOURCE_PROPERTY, RETURN_TYPE_PROPERTY,
					EXCEPTIONS_PROPERTY, CALL_SUPER_PROPERTY, OVERRIDES_EXISTING_PROPERTY, DO_EXPORT_PROPERTY, CODE_TYPE_PROPERTY,
					IS_PART_OF_BUILT_IN_SCHEMA_PROPERTY, TAGS_PROPERTY, SUMMARY_PROPERTY, DESCRIPTION_PROPERTY, IS_STATIC_PROPERTY,
					INCLUDE_IN_OPEN_API_PROPERTY, OPEN_API_RETURN_TYPE_PROPERTY, IS_PRIVATE_PROPERTY, RETURN_RAW_RESULT_PROPERTY, HTTP_VERB_PROPERTY
			),

			"export",
			newSet(
					GraphObjectTraitDefinition.ID_PROPERTY, GraphObjectTraitDefinition.TYPE_PROPERTY, SCHEMA_NODE_PROPERTY,
					STATIC_SCHEMA_NODE_NAME_PROPERTY, NodeInterfaceTraitDefinition.NAME_PROPERTY, SOURCE_PROPERTY, RETURN_TYPE_PROPERTY,
					EXCEPTIONS_PROPERTY, CALL_SUPER_PROPERTY, OVERRIDES_EXISTING_PROPERTY, DO_EXPORT_PROPERTY, CODE_TYPE_PROPERTY,
					IS_PART_OF_BUILT_IN_SCHEMA_PROPERTY, TAGS_PROPERTY, SUMMARY_PROPERTY, DESCRIPTION_PROPERTY, IS_STATIC_PROPERTY,
					INCLUDE_IN_OPEN_API_PROPERTY, OPEN_API_RETURN_TYPE_PROPERTY, IS_PRIVATE_PROPERTY, RETURN_RAW_RESULT_PROPERTY, HTTP_VERB_PROPERTY
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

	/*
	@Override
	public boolean isValid(final ErrorBuffer errorBuffer) {
	}

	@Override
	public void onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {

		super.onCreation(securityContext, errorBuffer);

		handleAutomaticCorrectionOfAttributes(securityContext, errorBuffer);
	}

	@Override
	public void onModification(SecurityContext securityContext, ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		super.onModification(securityContext, errorBuffer, modificationQueue);

		if (Boolean.TRUE.equals(getProperty(deleteMethod))) {

			StructrApp.getInstance().delete(this);

		} else {

			handleAutomaticCorrectionOfAttributes(securityContext, errorBuffer);
		}

		final String uuid = getUuid();
		if (uuid != null) {

			// acknowledge all events for this node when it is modified
			RuntimeEventLog.getEvents(e -> uuid.equals(e.getData().get("id"))).stream().forEach(e -> e.acknowledge());
		}

		// Ensure AbstractSchemaNode methodCache is invalidated when a schema method changes
		if (!TransactionCommand.isDeleted(getNode())) {

			final AbstractSchemaNode schemaNode = getProperty(SchemaMethod.schemaNode);
			if (schemaNode != null) {

				schemaNode.clearCachedSchemaMethodsForInstance();

				this.clearMethodCacheOfExtendingNodes();
			}
		}
	}

	@Override
	public void onNodeDeletion(SecurityContext securityContext) throws FrameworkException {
		super.onNodeDeletion(securityContext);
		AbstractSchemaNode.clearCachedSchemaMethods();
	}

	public SchemaMethodParameter getSchemaMethodParameter(final String name) {

		for (final SchemaMethodParameter param : getProperty(SchemaMethod.parameters)) {

			if (name.equals(param.getName())) {
				return param;
			}
		}

		return null;
	}

	// ----- private methods -----
	private void clearMethodCacheOfExtendingNodes() throws FrameworkException {

		final AbstractSchemaNode node = getProperty(schemaNode);
		if (node != null) {

			for (final SchemaNode extendingNode : node.getProperty(SchemaNode.extendedByClasses)) {

				extendingNode.clearCachedSchemaMethodsForInstance();
			}
		}
	}

	private void addType(final Queue<String> typeQueue, final AbstractSchemaNode schemaNode) {

		final SchemaNode _extendsClass = schemaNode.getProperty(SchemaNode.extendsClass);
		if (_extendsClass != null) {

			typeQueue.add(_extendsClass.getName());
		}

		final String _interfaces = schemaNode.getProperty(SchemaNode.implementsInterfaces);
		if (_interfaces != null) {

			for (final String iface : _interfaces.split("[, ]+")) {

				typeQueue.add(iface);
			}
		}
	}

	private void determineSignature(final Map<String, SchemaNode> schemaNodes, final AbstractSchemaNode schemaEntity, final ActionEntry entry, final String methodName) throws FrameworkException {

		final App app                  = StructrApp.getInstance();
		final Set<String> visitedTypes = new LinkedHashSet<>();
		final Queue<String> typeQueue  = new LinkedList<>();
		final String structrPackage    = "org.structr.dynamic.";

		// initial type
		addType(typeQueue, schemaEntity);

		while (!typeQueue.isEmpty()) {

			final String typeName = typeQueue.poll();
			String shortTypeName  = typeName;

			if (typeName != null && !visitedTypes.contains(typeName)) {

				visitedTypes.add(typeName);

				if (typeName.startsWith(structrPackage)) {
					shortTypeName = typeName.substring(structrPackage.length());
				}

				// try to find schema node for the given type
				final SchemaNode typeNode = schemaNodes.get(shortTypeName);
				if (typeNode != null && !typeNode.equals(schemaEntity)) {

					final SchemaMethod superMethod = typeNode.getSchemaMethod(methodName);
					if (superMethod != null) {

						final ActionEntry superEntry = superMethod.getActionEntry(schemaNodes, typeNode);

						entry.copy(superEntry);

						// done
						return;
					}

					// next type in queue
					addType(typeQueue, typeNode);

				} else {

					// no schema node for the given type found, try internal types
					final Class internalType = SchemaHelper.classForName(typeName);
					if (internalType != null) {

						if (getSignature(internalType, methodName, entry)) {

							return;
						}

						final Class superclass = internalType.getSuperclass();
						if (superclass != null) {

							// examine superclass as well
							typeQueue.add(superclass.getName());

							// collect interfaces
							for (final Class iface : internalType.getInterfaces()) {
								typeQueue.add(iface.getName());
							}
						}
					}
				}
			}
		}
	}

	@Override public boolean isLifecycleMethod () {

		final AbstractSchemaNode parent = getProperty(SchemaMethod.schemaNode);
		final boolean hasParent         = (parent != null);
		final String methodName         = getName();

		if (hasParent) {

			final List<String> typeBasedLifecycleMethods = List.of("onNodeCreation", "onCreate", "afterCreate", "onSave", "afterSave", "onDelete", "afterDelete");
			final List<String> fileLifecycleMethods      = List.of("onUpload", "onDownload");
			final List<String> userLifecycleMethods      = List.of("onOAuthLogin");

			for (final String lifecycleMethodPrefix : typeBasedLifecycleMethods) {

				if (methodName.startsWith(lifecycleMethodPrefix)) {
					return true;
				}
			}

			boolean inheritsFromFile = false;
			boolean inheritsFromUser = false;

			final Class type = SchemaHelper.getEntityClassForRawType(parent.getName());

			if (type != null) {

				inheritsFromFile = AbstractFile.class.isAssignableFrom(type);
				inheritsFromUser = User.class.isAssignableFrom(type);
			}

			if (inheritsFromFile) {

				for (final String lifecycleMethodName : fileLifecycleMethods) {

					if (methodName.equals(lifecycleMethodName)) {
						return true;
					}
				}
			}

			if (inheritsFromUser) {

				for (final String lifecycleMethodName : userLifecycleMethods) {

					if (methodName.equals(lifecycleMethodName)) {
						return true;
					}
				}
			}

		} else {

			final List<String> globalLifecycleMethods = List.of("onStructrLogin", "onStructrLogout", "onAcmeChallenge");

			for (final String lifecycleMethodName : globalLifecycleMethods) {

				if (methodName.equals(lifecycleMethodName)) {
					return true;
				}
			}

		}

		return false;
	}

	// ----- interface Favoritable -----
	@Override
	public String getContext() {

		final AbstractSchemaNode parent = getProperty(SchemaMethod.schemaNode);
		final StringBuilder buf = new StringBuilder();

		if (parent != null) {

			buf.append(parent.getProperty(SchemaNode.name));
			buf.append(".");
			buf.append(getProperty(name));
		}

		return buf.toString();
	}

	@Override
	public String getFavoriteContent() {
		return getProperty(SchemaMethod.source);
	}

	@Override
	public String getFavoriteContentType() {
		return "application/x-structr-javascript";
	}

	@Override
	public void setFavoriteContent(String content) throws FrameworkException {
		setProperty(SchemaMethod.source, content);
	}

	private boolean getSignature(final Class type, final String methodName, final ActionEntry entry) {

		// superclass is NodeInterface
		for (final Method method : type.getMethods()) {

			if (methodName.equals(method.getName()) && (method.getModifiers() & Modifier.STATIC) == 0) {

				final Type[] parameterTypes = method.getGenericParameterTypes();
				final Type returnType       = method.getGenericReturnType();
				final List<Type> types      = new LinkedList<>();

				// compile list of types to check for generic type parameter
				types.addAll(Arrays.asList(parameterTypes));
				types.add(returnType);

				final String genericTypeParameter = getGenericMethodParameter(types, method);

				// check for generic return type, and if the method defines its own generic type
				if (returnType instanceof TypeVariable && ((TypeVariable)returnType).getGenericDeclaration().equals(method)) {

					// method defines its own generic type
					entry.setReturnType(genericTypeParameter + returnType.getTypeName());

				} else {

					// non-generic return type
					final Class returnClass = method.getReturnType();
					if (returnClass.isArray()) {

						entry.setReturnType(genericTypeParameter + returnClass.getComponentType().getName() + "[]");

					} else {

						entry.setReturnType(genericTypeParameter + method.getReturnType().getName());
					}
				}

				for (final Parameter parameter : method.getParameters()) {

					String typeName = parameter.getParameterizedType().getTypeName();
					String name     = parameter.getType().getSimpleName();

					if (typeName.contains("$")) {
						typeName = typeName.replace("$", ".");
					}

					entry.addParameter(typeName, parameter.getName());
				}

				for (final Class exception : method.getExceptionTypes()) {
					entry.addException(exception.getName());
				}

				entry.setOverrides(getProperty(overridesExisting));
				entry.setCallSuper(getProperty(callSuper));

				// success
				return true;
			}
		}

		return false;
	}

	private String getGenericMethodParameter(final List<Type> types, final Method method) {

		final List<String> typeParameterNames = new LinkedList<>();

		for (final Type type : types) {

			if (type instanceof TypeVariable && ((TypeVariable)type).getGenericDeclaration().equals(method)) {

				// method defines its own generic type
				typeParameterNames.add(type.getTypeName());
			}
		}

		if (typeParameterNames.isEmpty()) {
			return "";
		}

		return "<" + StringUtils.join(typeParameterNames, ", ") + "> ";
	}

	// ----- private static methods -----
	public static String getCachedSourceCode(final String uuid) throws FrameworkException {

		// this method is called from generated Java code in ActionEntry.java line 242

		final SchemaMethod method = StructrApp.getInstance().getNodeById(SchemaMethod.class, uuid);
		if (method != null) {

			final String source = method.getProperty(SchemaMethod.source);
			if (source != null) {

				return "${" + source.trim() + "}";
			}
		}

		return "${}";
	}
	*/
}
