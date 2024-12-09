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

import org.structr.core.entity.*;
import org.structr.core.graph.NodeInterface;
import org.structr.core.notion.PropertySetNotion;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.Traits;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.wrappers.SchemaMethodTraitWrapper;

import java.util.Map;
import java.util.Set;

/**
 *
 *
 */
public final class SchemaMethodTraitDefinition extends AbstractTraitDefinition {


	private static final Property<Iterable<NodeInterface>> parameters         = new EndNodes("parameters", "SchemaMethodParameters").partOfBuiltInSchema();
	private static final Property<NodeInterface>      schemaNode              = new StartNode("schemaNode", "SchemaNodeMethod", new PropertySetNotion(Traits.idProperty(), Traits.nameProperty(), Traits.of("SchemaNode").key("isBuiltinType"))).partOfBuiltInSchema();
	private static final Property<String>             staticSchemaNodeName    = new StringProperty("staticSchemaNodeName").partOfBuiltInSchema();
	private static final Property<String>             signature               = new StringProperty("signature").indexed().partOfBuiltInSchema();
	private static final Property<String>             virtualFileName         = new StringProperty("virtualFileName").indexed().partOfBuiltInSchema();
	private static final Property<String>             returnType              = new StringProperty("returnType").indexed().partOfBuiltInSchema();
	private static final Property<String>             openAPIReturnType       = new StringProperty("openAPIReturnType").indexed().partOfBuiltInSchema();
	private static final Property<String>             source                  = new StringProperty("source").partOfBuiltInSchema();
	private static final Property<String[]>           exceptions              = new ArrayProperty("exceptions", String.class).indexed().partOfBuiltInSchema();
	private static final Property<Boolean>            callSuper               = new BooleanProperty("callSuper").indexed().partOfBuiltInSchema();
	private static final Property<Boolean>            overridesExisting       = new BooleanProperty("overridesExisting").indexed().partOfBuiltInSchema();
	private static final Property<Boolean>            doExport                = new BooleanProperty("doExport").indexed().partOfBuiltInSchema();
	private static final Property<String>             codeType                = new StringProperty("codeType").indexed().partOfBuiltInSchema();
	private static final Property<Boolean>            isPartOfBuiltInSchema   = new BooleanProperty("isPartOfBuiltInSchema").indexed().partOfBuiltInSchema();
	private static final Property<Boolean>            includeInOpenAPI        = new BooleanProperty("includeInOpenAPI").indexed().partOfBuiltInSchema();
	private static final Property<String[]>           tags                    = new ArrayProperty("tags", String.class).indexed().partOfBuiltInSchema();
	private static final Property<String>             summary                 = new StringProperty("summary").partOfBuiltInSchema();
	private static final Property<String>             description             = new StringProperty("description").partOfBuiltInSchema();
	private static final Property<Boolean>            isStatic                = new BooleanProperty("isStatic").defaultValue(false).partOfBuiltInSchema();
	private static final Property<Boolean>            isPrivate               = new BooleanProperty("isPrivate").defaultValue(false).indexed().indexedWhenEmpty().partOfBuiltInSchema();
	private static final Property<Boolean>            returnRawResult         = new BooleanProperty("returnRawResult").defaultValue(false).partOfBuiltInSchema();
	private static final Property<SchemaMethod.HttpVerb> httpVerb               = new EnumProperty<>("httpVerb", SchemaMethod.HttpVerb.class).defaultValue(SchemaMethod.HttpVerb.POST).partOfBuiltInSchema();
	// Note: if you add properties here, make sure to add the in Deployment3Test.java#test33SchemaMethods as well!

	// property which is only used to mark a schema method as "will be deleted"
	private static final Property<Boolean>            deleteMethod             = new BooleanProperty("deleteMethod").defaultValue(Boolean.FALSE);

	/*
	private static final Set<PropertyKey> schemaRebuildTriggerKeys = new LinkedHashSet<>(Arrays.asList(
		name, schemaNode, staticSchemaNodeName, exceptions, callSuper, overridesExisting, doExport, codeType, isPartOfBuiltInSchema, isStatic, isPrivate, returnRawResult, httpVerb
	));

	public static final View defaultView = new View(SchemaMethod.class, PropertyView.Public,
		name, schemaNode, staticSchemaNodeName, source, returnType, exceptions, callSuper, overridesExisting, doExport, codeType, isPartOfBuiltInSchema, tags, summary, description, isStatic, isPrivate, returnRawResult, httpVerb
	);

	public static final View uiView = new View(SchemaMethod.class, PropertyView.Ui,
		name, schemaNode, staticSchemaNodeName, source, returnType, exceptions, callSuper, overridesExisting, doExport, codeType, isPartOfBuiltInSchema, tags, summary, description, isStatic, includeInOpenAPI, openAPIReturnType, isPrivate, returnRawResult, httpVerb
	);

	public static final View schemaView = new View(SchemaNode.class, "schema",
		id, type, schemaNode, staticSchemaNodeName, name, source, returnType, exceptions, callSuper, overridesExisting, doExport, codeType, isPartOfBuiltInSchema, tags, summary, description, isStatic, includeInOpenAPI, openAPIReturnType, isPrivate, returnRawResult, httpVerb
	);

	public static final View exportView = new View(SchemaMethod.class, "export",
		id, type, schemaNode, staticSchemaNodeName, name, source, returnType, exceptions, callSuper, overridesExisting, doExport, codeType, isPartOfBuiltInSchema, tags, summary, description, isStatic, includeInOpenAPI, openAPIReturnType, isPrivate, returnRawResult, httpVerb
	);
	*/

	public SchemaMethodTraitDefinition() {
		super("SchemaMethod");
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {
		return Map.of();
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {
		return Map.of();
	}

	@Override
	public Map<Class, RelationshipTraitFactory> getRelationshipTraitFactories() {
		return Map.of();
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(

			SchemaMethod.class, (traits, node) -> new SchemaMethodTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		return Set.of(

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
	public Relation getRelation() {
		return null;
	}

	/*
	@Override public Iterable<SchemaMethodParameter> getParameters() {
		return getProperty(parameters);
	}

	@Override public ActionEntry getActionEntry(final Map<String, SchemaNode> schemaNodes, final AbstractSchemaNode schemaEntity) throws FrameworkException {

		final ActionEntry entry                  = new ActionEntry("___" + SchemaHelper.cleanPropertyName(getProperty(AbstractNode.name)), getProperty(SchemaMethod.source), getProperty(SchemaMethod.codeType));
		final List<SchemaMethodParameter> params = Iterables.toList(getProperty(parameters));

		// add UUID
		entry.setCodeSource(this);

		// Parameters must be sorted by index
		//Collections.sort(params, new GraphObjectComparator(SchemaMethodParameter.index, false));
		Collections.sort(params, SchemaMethodParameter.index.sorted(false));

		for (final SchemaMethodParameter parameter : params) {

			entry.addParameter(parameter.getParameterType(), parameter.getName());
		}

		entry.setReturnType(getProperty(returnType));
		entry.setCallSuper(getProperty(callSuper));

		final String[] _exceptions = getProperty(exceptions);
		if (_exceptions != null) {

			for (final String exception : _exceptions) {
				entry.addException(exception);
			}
		}

		// check for overridden methods and determine method signature etc. from superclass(es)
		if (getProperty(overridesExisting)) {
			determineSignature(schemaNodes, schemaEntity, entry, getProperty(name));
		}

		// check for overridden methods and determine method signature etc. from superclass(es)
		if (getProperty(doExport)) {
			entry.setDoExport(true);
		}

		// check for overridden methods and determine method signature etc. from superclass(es)
		if (getProperty(isStatic)) {
			entry.setIsStatic(true);
		}

		return entry;
	}

	@Override public boolean isStaticMethod() {
		return getProperty(isStatic);
	}

	@Override public boolean isPrivateMethod() {
		return getProperty(isPrivate);
	}

	@Override public boolean returnRawResult() {
		return getProperty(returnRawResult);
	}

	@Override public HttpVerb getHttpVerb() {
		return getProperty(httpVerb);
	}

	@Override public boolean isJava() {
		return "java".equals(getProperty(codeType));
	}

	@Override
	public boolean isValid(final ErrorBuffer errorBuffer) {

		boolean valid = super.isValid(errorBuffer);

		valid &= ValidationHelper.isValidStringMatchingRegex(this, name, schemaMethodNamePattern, errorBuffer);

		final Set<String> propertyViews = Services.getInstance().getConfigurationProvider().getPropertyViews();
		final String thisMethodName     = getProperty(AbstractNode.name);

		if (thisMethodName != null && propertyViews.contains(thisMethodName)) {
			errorBuffer.add(new SemanticErrorToken(this.getType(), "name", "already_exists").withValue(thisMethodName).withDetail("A method cannot have the same name as a view"));
		}

		// check case-insensitive name uniqueness on current level (type or user-defined functions)
		final AbstractSchemaNode parentOrNull = this.getProperty(SchemaMethod.schemaNode);

		try {

			final List<SchemaMethod> methodsOnCurrentLevel = StructrApp.getInstance().nodeQuery("SchemaMethod").and(SchemaMethod.schemaNode, parentOrNull).getAsList();
			final List<SchemaMethodParameter> params       = Iterables.toList(this.getParameters());
			// param comparison is required because otherwise this would fail for at least "getScaledImage" and "updateFeedTask"
			final String paramsAsString                    = params.stream().map(p -> p.getName() + ":" + p.getParameterType()).collect(Collectors.joining(";"));

			for (final SchemaMethod otherSchemaMethod : methodsOnCurrentLevel) {

				final boolean isDifferentMethod = !(this.getUuid().equals(otherSchemaMethod.getUuid()));

				if (isDifferentMethod) {

					final boolean isSameNameIgnoringCase                = thisMethodName.equalsIgnoreCase(otherSchemaMethod.getName());
					final List<SchemaMethodParameter> otherMethodParams = Iterables.toList(otherSchemaMethod.getParameters());
					final String otherParamsAsString                    = otherMethodParams.stream().map(p -> p.getName() + ":" + p.getParameterType()).collect(Collectors.joining(";"));

					final boolean hasSameParameters = (params.size() == otherMethodParams.size() && paramsAsString.equals(otherParamsAsString));

					if (isSameNameIgnoringCase && hasSameParameters) {

						errorBuffer.add(new SemanticErrorToken(this.getType(), "name", "already_exists").withValue(thisMethodName).withDetail("Multiple methods with identical names (case-insensitive) are not supported on the same level"));
						valid = false;
					}
				}
			}

		} catch (FrameworkException fex) {

			errorBuffer.add(new SemanticErrorToken(this.getType(),"none", "exception_occurred").withValue(thisMethodName).withDetail("Exception occurred while checking uniqueness of method name - please retry. Cause: " + fex.getMessage()));
			valid = false;
		}

		return valid;
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

		// superclass is AbstractNode
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
