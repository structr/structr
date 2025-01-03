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
package org.structr.schema.export;

import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.schema.JsonMethod;
import org.structr.api.schema.JsonParameter;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.SchemaMethodParameter;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.Traits;
import org.structr.schema.openapi.common.OpenAPIResponseReference;
import org.structr.schema.openapi.operation.OpenAPIMethodOperation;
import org.structr.schema.openapi.operation.OpenAPIStaticMethodOperation;
import org.structr.schema.openapi.operation.OpenAPIUserDefinedFunctionOperation;
import org.structr.schema.openapi.parameter.OpenAPIPathParameter;
import org.structr.schema.openapi.request.OpenAPIRequestResponse;
import org.structr.schema.openapi.schema.OpenAPIObjectSchema;
import org.structr.schema.openapi.schema.OpenAPIPrimitiveSchema;
import org.structr.schema.openapi.schema.OpenAPIResultSchema;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.Map.Entry;

/**
 *
 *
 */
public class StructrMethodDefinition implements JsonMethod, StructrDefinition {

	private static final Logger logger = LoggerFactory.getLogger(StructrMethodDefinition.class.getName());

	private final List<StructrParameterDefinition> parameters = new LinkedList<>();
	private final List<String> exceptions                     = new LinkedList<>();
	private final Set<String> tags                            = new TreeSet<>();
	private SchemaMethod schemaMethod                         = null;
	private boolean includeInOpenAPI                          = false;
	private boolean overridesExisting                         = false;
	private boolean doExport                                  = false;
	private boolean callSuper                                 = false;
	private boolean isStatic                                  = false;
	private boolean isPrivate                                 = false;
	private boolean returnRawResult                           = false;
	private JsonType parent                                   = null;
	private String httpVerb                                   = "POST";
	private String returnType                                 = null;
	private String openAPIReturnType                          = null;
	private String codeType                                   = null;
	private String name                                       = null;
	private String description                                = null;
	private String summary                                    = null;
	private String source                                     = null;

	StructrMethodDefinition(final JsonType parent, final String name) {

		this.parent = parent;
		this.name   = name;
	}

	@Override
	public String toString() {
		return getSignature();
	}

	@Override
	public int hashCode() {
		return getSignature().hashCode();
	}

	@Override
	public boolean equals(final Object other) {

		if (other instanceof StructrMethodDefinition) {

			return other.hashCode() == hashCode();
		}

		return false;
	}

	@Override
	public URI getId() {

		final URI parentId = parent.getId();
		if (parentId != null) {

			try {
				final URI containerURI = new URI(parentId.toString() + "/");
				return containerURI.resolve("properties/" + getName());

			} catch (URISyntaxException urex) {
				logger.warn("", urex);
			}
		}

		return null;
	}

	@Override
	public JsonType getParent() {
		return parent;
	}

	public String getUniqueName() {
		return name + "." + this.parameters.size();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public JsonMethod setName(String name) {

		this.name = name;
		return this;
	}

	@Override
	public String getSource() {
		return source;
	}

	@Override
	public JsonMethod setSource(final String source) {
		this.source = source;
		return this;
	}

	@Override
	public String getSummary() {
		return summary;
	}

	@Override
	public JsonMethod setSummary(String summary) {
		this.summary = summary;
		return this;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public JsonMethod setDescription(String description) {
		this.description = description;
		return this;
	}

	@Override
	public List<JsonParameter> getParameters() {
		return (List)parameters;
	}

	@Override
	public JsonMethod addParameter(final String name, String type) {

		final StructrParameterDefinition param = new StructrParameterDefinition(this, name);

		param.setType(type);

		parameters.add(param);

		return this;
	}

	@Override
	public String getReturnType() {
		return returnType;
	}

	@Override
	public JsonMethod setReturnType(final String returnType) {
		this.returnType = returnType;
		return this;
	}

	@Override
	public String getOpenAPIReturnType() { return openAPIReturnType; }

	@Override
	public JsonMethod setOpenAPIReturnType(final String openAPIReturnType) {
		this.openAPIReturnType = openAPIReturnType;
		return this;
	}

	@Override
	public boolean callSuper() {
		return callSuper;
	}

	@Override
	public JsonMethod setCallSuper(final boolean callSuper) {
		this.callSuper = callSuper;
		return this;
	}

	@Override
	public boolean isStatic() {
		return isStatic;
	}

	@Override
	public JsonMethod setIsStatic(final boolean isStatic) {
		this.isStatic = isStatic;
		return this;
	}

	@Override
	public boolean isPrivate() {
		return isPrivate;
	}

	@Override
	public JsonMethod setIsPrivate(final boolean isPrivate) {
		this.isPrivate = isPrivate;
		return this;
	}

	@Override
	public boolean returnRawResult() {
		return returnRawResult;
	}

	@Override
	public JsonMethod setReturnRawResult(final boolean returnRawResult) {
		this.returnRawResult = returnRawResult;
		return this;
	}

	@Override
	public JsonMethod setHttpVerb(final String httpVerb) {
		this.httpVerb = httpVerb;
		return this;
	}

	@Override
	public String getHttpVerb() {
		return httpVerb;
	}

	@Override
	public boolean overridesExisting() {
		return overridesExisting;
	}

	@Override
	public JsonMethod setOverridesExisting(final boolean overridesExisting) {
		this.overridesExisting = overridesExisting;
		return this;
	}

	@Override
	public boolean doExport() {
		return doExport;
	}

	@Override
	public JsonMethod setDoExport(final boolean doExport) {
		this.doExport = doExport;
		return this;
	}

	@Override
	public List<String> getExceptions() {
		return exceptions;
	}

	@Override
	public JsonMethod addException(final String exception) {
		this.exceptions.add(exception);
		return this;
	}

	@Override
	public String getCodeType() {
		return codeType;
	}

	@Override
	public JsonMethod setCodeType(String codeType) {
		this.codeType = codeType;
		return this;
	}

	@Override
	public int compareTo(final JsonMethod o) {
		return getName().compareTo(o.getName());
	}

	@Override
	public Set<String> getTags() {
		return tags;
	}

	@Override
	public JsonMethod addTags(final String... tags) {
		this.tags.addAll(Arrays.asList(tags));
		return this;
	}

	@Override
	public boolean includeInOpenAPI() {
		return includeInOpenAPI;
	}

	@Override
	public JsonMethod setIncludeInOpenAPI(final boolean includeInOpenAPI) {
		this.includeInOpenAPI = includeInOpenAPI;
		return this;
	}

	@Override
	public StructrDefinition resolveJsonPointerKey(final String key) {
		return null;
	}

	// ----- package methods -----
	SchemaMethod getSchemaMethod() {
		return schemaMethod;
	}

	SchemaMethod createDatabaseSchema(final App app, final AbstractSchemaNode schemaNode) throws FrameworkException {

		final Traits traits                = Traits.of("SchemaMethod");
		final PropertyMap updateProperties = new PropertyMap();
		SchemaMethod method                = null;
		int index                          = 0;

		for (final SchemaMethod m : schemaNode.getSchemaMethodsByName(getName())) {

			if (getSignature().equals(m.getSignature())) {

				method = m;
				break;
			}
		}

		if (method == null) {

			final PropertyMap getOrCreateProperties = new PropertyMap();
			getOrCreateProperties.put(traits.key("name"),                  getName());
			getOrCreateProperties.put(traits.key("signature"),             getSignature());
			getOrCreateProperties.put(traits.key("codeType"),              getCodeType());
			getOrCreateProperties.put(traits.key("returnType"),            getReturnType());
			getOrCreateProperties.put(traits.key("schemaNode"),            schemaNode.getWrappedNode());
			getOrCreateProperties.put(traits.key("exceptions"),            listToArray(getExceptions()));
			getOrCreateProperties.put(traits.key("overridesExisting"),     overridesExisting());
			getOrCreateProperties.put(traits.key("callSuper"),             callSuper());
			getOrCreateProperties.put(traits.key("doExport"),              doExport());

			method = app.create("SchemaMethod", getOrCreateProperties).as(SchemaMethod.class);
		}

		updateProperties.put(traits.key("summary"),               getSummary());
		updateProperties.put(traits.key("description"),           getDescription());
		updateProperties.put(traits.key("source"),                getSource());
		updateProperties.put(traits.key("isPartOfBuiltInSchema"), true);
		updateProperties.put(traits.key("isStatic"),              isStatic());
		updateProperties.put(traits.key("isPrivate"),             isPrivate());
		updateProperties.put(traits.key("returnRawResult"),       returnRawResult());
		updateProperties.put(traits.key("httpVerb"),              SchemaMethod.HttpVerb.valueOf(getHttpVerb()));
		updateProperties.put(traits.key("includeInOpenAPI"),      includeInOpenAPI());
		updateProperties.put(traits.key("openAPIReturnType"),     getOpenAPIReturnType());

		final Set<String> mergedTags     = new LinkedHashSet<>(this.tags);
		final String[] existingTagsArray = method.getTags();

		if (existingTagsArray != null) {

			mergedTags.addAll(Arrays.asList(existingTagsArray));
		}

		if (!mergedTags.isEmpty()) {
			updateProperties.put(traits.key("tags"), listToArray(mergedTags));
		}

		method.getWrappedNode().setProperties(SecurityContext.getSuperUserInstance(), updateProperties);

		// create database schema for method parameters
		for (final StructrParameterDefinition param : parameters) {
			param.createDatabaseSchema(app, method, index++);
		}


		this.schemaMethod = method;

		// return modified property
		return method;
	}


	void deserialize(final Map<String, Object> source) {

		final Object _source = source.get(JsonSchema.KEY_SOURCE);
		if (_source != null && _source instanceof String) {

			this.source = (String)_source;
		}

		final Object _summary = source.get(JsonSchema.KEY_SUMMARY);
		if (_summary != null && _summary instanceof String) {

			this.summary = (String)_summary;
		}

		final Object _description = source.get(JsonSchema.KEY_DESCRIPTION);
		if (_description != null && _description instanceof String) {

			this.description = (String)_description;
		}

		final Object _codeType = source.get(JsonSchema.KEY_CODE_TYPE);
		if (_codeType != null && _codeType instanceof String) {

			this.codeType = (String)_codeType;
		}

		final Object _returnType = source.get(JsonSchema.KEY_RETURN_TYPE);
		if (_returnType != null && _returnType instanceof String) {

			this.returnType = (String)_returnType;
		}

		final Object _openAPIReturnType = source.get(JsonSchema.KEY_OPENAPI_RETURN_TYPE);
		if (_openAPIReturnType != null && _openAPIReturnType instanceof String) {

			this.openAPIReturnType = (String)_openAPIReturnType;
		}

		final Object _exceptions = source.get(JsonSchema.KEY_EXCEPTIONS);
		if (_exceptions != null && _exceptions instanceof List) {

			final List<String> list = (List)_exceptions;
			for (final String fqcn : list) {

				this.exceptions.add(fqcn);
			}
		}

		final Object _callSuper = source.get(JsonSchema.KEY_CALL_SUPER);
		if (_callSuper != null && _callSuper instanceof Boolean) {

			this.callSuper = (Boolean)_callSuper;
		}

		final Object _isStatic = source.get(JsonSchema.KEY_IS_STATIC);
		if (_isStatic != null && _isStatic instanceof Boolean) {

			this.isStatic = (Boolean)_isStatic;
		}

		final Object _isPrivate = source.get(JsonSchema.KEY_IS_PRIVATE);
		if (_isPrivate != null && _isPrivate instanceof Boolean) {

			this.isPrivate = (Boolean)_isPrivate;
		}

		final Object _returnRawResult = source.get(JsonSchema.KEY_RETURN_RAW_RESULT);
		if (_returnRawResult != null && _returnRawResult instanceof Boolean) {

			this.returnRawResult = (Boolean)_returnRawResult;
		}

		final Object _httpVerb = source.get(JsonSchema.KEY_HTTP_VERB);
		if (_httpVerb != null && _httpVerb instanceof String) {

			this.httpVerb = (String)_httpVerb;
		}

		final Object _overridesExisting = source.get(JsonSchema.KEY_OVERRIDES_EXISTING);
		if (_overridesExisting != null && _overridesExisting instanceof Boolean) {

			this.overridesExisting = (Boolean)_overridesExisting;
		}

		final Object _doExport = source.get(JsonSchema.KEY_DO_EXPORT);
		if (_doExport != null && _doExport instanceof Boolean) {

			this.doExport = (Boolean)_doExport;
		}

		final Map<String, Object> params = (Map<String, Object>)source.get(JsonSchema.KEY_PARAMETERS);
		if (params != null) {

			for (final Entry<String, Object> entry : params.entrySet()) {

				final String paramName = entry.getKey();
				final Object value     = entry.getValue();

				if (value instanceof Map) {

					final StructrParameterDefinition parameter = StructrParameterDefinition.deserialize(this, paramName, (Map)value);
					if (parameter != null) {

						this.parameters.add(parameter);
					}

				} else {

					throw new IllegalStateException("Method parameter definition " + paramName + " must be of type string or map.");
				}
			}

			// sort parameters
			Collections.sort(parameters, (o1, o2) -> Integer.valueOf(o1.getIndex()).compareTo(o2.getIndex()));
		}

		if (source.containsKey(JsonSchema.KEY_TAGS)) {

			final Object tagsValue = source.get(JsonSchema.KEY_TAGS);
			if (tagsValue instanceof List) {

				tags.addAll((List<String>)tagsValue);
			}
		}

		final Object _includeInOpenAPI = source.get(JsonSchema.KEY_INCLUDE_IN_OPENAPI);
		if (_includeInOpenAPI != null && _includeInOpenAPI instanceof Boolean) {

			this.includeInOpenAPI = (Boolean)_includeInOpenAPI;
		}
	}

	void deserialize(final SchemaMethod method) {

		this.schemaMethod = method;

		setName(method.getName());
		setSource(method.getSource());
		setSummary(method.getSummary());
		setDescription(method.getDescription());
		setCodeType(method.getCodeType());
		setReturnType(method.getReturnType());
		setCallSuper(method.callSuper());
		setIsStatic(method.isStaticMethod());
		setIsPrivate(method.isPrivateMethod());
		setReturnRawResult(method.returnRawResult());
		setHttpVerb(method.getHttpVerb().name());
		setOverridesExisting(method.overridesExisting());
		setDoExport(method.doExport());
		setIncludeInOpenAPI(method.includeInOpenAPI());
		setOpenAPIReturnType(method.getOpenAPIReturnType());

		final String[] exceptionArray = method.getExceptions();
		if (exceptionArray != null) {

			for (final String fqcn : exceptionArray) {
				addException(fqcn);
			}
		}

		for (final SchemaMethodParameter param : method.getParameters()) {

			final StructrParameterDefinition parameter = StructrParameterDefinition.deserialize(this, param);
			if (parameter != null) {

				parameters.add(parameter);
			}
		}

		Collections.sort(parameters, (p1, p2) -> {
			return Integer.valueOf(p1.getIndex()).compareTo(p2.getIndex());
		});

		final String[] tagArray = method.getTags();
		if (tagArray != null) {

			this.tags.addAll(Arrays.asList(tagArray));
		}
	}

	Map<String, Object> serialize() {

		final Map<String, Object> map    = new TreeMap<>();
		final Map<String, Object> params = new LinkedHashMap<>();

		map.put(JsonSchema.KEY_SOURCE, source);
		map.put(JsonSchema.KEY_SUMMARY, summary);
		map.put(JsonSchema.KEY_DESCRIPTION, description);
		map.put(JsonSchema.KEY_CODE_TYPE, codeType);
		map.put(JsonSchema.KEY_RETURN_TYPE, returnType);
		map.put(JsonSchema.KEY_EXCEPTIONS, exceptions);
		map.put(JsonSchema.KEY_CALL_SUPER, callSuper);
		map.put(JsonSchema.KEY_IS_STATIC, isStatic);
		map.put(JsonSchema.KEY_IS_PRIVATE, isPrivate);
		map.put(JsonSchema.KEY_RETURN_RAW_RESULT, returnRawResult);
		map.put(JsonSchema.KEY_HTTP_VERB, httpVerb);
		map.put(JsonSchema.KEY_OVERRIDES_EXISTING, overridesExisting);
		map.put(JsonSchema.KEY_DO_EXPORT, doExport);
		map.put(JsonSchema.KEY_INCLUDE_IN_OPENAPI, includeInOpenAPI);
		map.put(JsonSchema.KEY_OPENAPI_RETURN_TYPE, openAPIReturnType);

		for (final StructrParameterDefinition param : parameters) {
			params.put(param.getName(), param.serialize());
		}

		if (!params.isEmpty()) {
			map.put(JsonSchema.KEY_PARAMETERS, params);
		}

		if (!tags.isEmpty()) {
			map.put(JsonSchema.KEY_TAGS, tags);
		}

		return map;
	}

	String getSignature() {

		final StringBuilder buf = new StringBuilder();

		buf.append(getReturnType());
		buf.append(" ");
		buf.append(getName());
		buf.append("(");
		buf.append(StringUtils.join(parameters, ", "));
		buf.append(")");

		if (!exceptions.isEmpty()) {
			buf.append(" throws ");
			buf.append(StringUtils.join(exceptions, ", "));
		}

		return buf.toString();
	}

	void diff(final StructrMethodDefinition other) {
	}

	// ----- OpenAPI -----
	public Map<String, Object> serializeOpenAPI(final StructrTypeDefinition parentType, Set<String> viewNames) {

		final Map<String, Object> operations = new LinkedHashMap<>();

		if (includeInOpenAPI()) {

			final SchemaMethod method = this.getSchemaMethod();

			final boolean isLifecycleMethod = method.isLifecycleMethod();
			final boolean isTypeMethod      = (parent != null);

			if (!isLifecycleMethod) {

				final String verb = StringUtils.defaultIfBlank(this.getHttpVerb(), "post").toLowerCase();

				if (!isPrivate()) {

					if (isStatic()) {

						operations.put(createPath(verb, true), Map.of(verb, new OpenAPIStaticMethodOperation(this, parentType, viewNames)));

					} else {

						if (isTypeMethod) {

							operations.put(createPath(verb, false), Map.of(verb, new OpenAPIMethodOperation(this, parentType, viewNames)));

						} else {

							final String path = "/" + getName();

							operations.put(path, Map.of(verb, new OpenAPIUserDefinedFunctionOperation(this)));
						}
					}
				}
			}
		}

		return operations;
	}

	public String createPath(final String verb, final boolean isStatic) {

		final StringBuilder pathBuilder = new StringBuilder("/");

		pathBuilder.append(getParent().getName());

		if (!isStatic) {
			pathBuilder.append("/{uuid}");
		}

		pathBuilder.append("/");
		pathBuilder.append(getName());

		// GET calls take their arguments from the URL
		if ("get".equals(verb)) {

			for (final JsonParameter param : this.getParameters()) {

				pathBuilder.append("/{");
				pathBuilder.append(param.getName());
				pathBuilder.append("}");
			}
		}

		pathBuilder.append("/{view}");

		return pathBuilder.toString();
	}

	public boolean isSelected(final String tag) {

		final Set<String> tags = getTags();
		boolean selected       = tag == null || tags.contains(tag);

		// don't show types without tags
		if (tags.isEmpty()) {
			return false;
		}

		// skip blacklisted tags
		if (intersects(StructrTypeDefinition.TagBlacklist, tags)) {

			// if a tag is selected, it overrides the blacklist
			selected = tag != null && tags.contains(tag);
		}

		return selected;
	}

	private boolean intersects(final Set<String> set1, final Set<String> set2) {

		final Set<String> intersection = new LinkedHashSet<>(set1);

		intersection.retainAll(set2);

		return !intersection.isEmpty();
	}

	public Map<String, Object> getOpenAPIRequestSchema() {

		final Map<String, Object> schema = new LinkedHashMap<>();

		for (final JsonParameter param : getParameters()) {

			schema.putAll(new OpenAPIPrimitiveSchema(param.getDescription(), param.getName(), param.getType()));
		}

		return new OpenAPIObjectSchema("Parameters", schema);
	}

	public Map<String, Object> getOpenAPIRequestBodyExample() {

		final Map<String, Object> schema = new LinkedHashMap<>();

		for (final JsonParameter param : getParameters()) {

			String exampleValue = param.getExampleValue();
			String type         = param.getType();
			Object typedValue   = null;

			if (type == null || StringUtils.isBlank(type)) {
				type = "string";
			}

			switch (type.toLowerCase()) {

				case "int":
				case "integer":
				case "number":
					if (StringUtils.isBlank(exampleValue)) { exampleValue = "1"; }
					typedValue = Integer.valueOf(exampleValue);
					break;

				case "float":
				case "double":
					if (StringUtils.isBlank(exampleValue)) { exampleValue = "1.0"; }
					typedValue = Double.valueOf(exampleValue);
					break;

				case "string":
					if (StringUtils.isBlank(exampleValue)) { exampleValue = "abc"; }
					typedValue = exampleValue;
					break;

				case "boolean":
					if (StringUtils.isBlank(exampleValue)) { exampleValue = "true"; }
					typedValue = Boolean.valueOf(exampleValue);
					break;
			}

			schema.put(param.getName(), typedValue);
		}

		return schema;
	}

	public List<Map<String, Object>> getOpenAPIRequestParameters(final Set<String> viewNames) {

		final String verb                    = StringUtils.defaultIfBlank(this.getHttpVerb(), "post").toLowerCase();
		final List<Map<String, Object>> list = new LinkedList<>();

		list.add(new OpenAPIPathParameter("uuid", "The UUID of the target object", Map.of("type", "string"), true));

		if ("get".equals(verb)) {

			for (final JsonParameter param : this.getParameters()) {

				list.add(new OpenAPIPathParameter(param.getName(), param.getDescription(), Map.of("type", param.getType()), true));
			}
		}

		list.add(new OpenAPIPathParameter("view", "Changes the response schema to the selected views schema", Map.of("type", "string", "enum", viewNames), false));

		return list;
	}

	public Map<String, Object> getOpenAPIRequestBody() {

		final String verb = StringUtils.defaultIfBlank(this.getHttpVerb(), "post").toLowerCase();

		if (!"get".equals(verb) && !getParameters().isEmpty()) {
			return new OpenAPIRequestResponse("Parameters", getOpenAPIRequestSchema(), getOpenAPIRequestBodyExample(), null, false);
		}

		return null;
	}

	public Map<String, Object> getOpenAPISuccessResponse() {

		final Map<String, Object> schemaFromJsonString = new LinkedHashMap<>();
		final String returnType                        = getOpenAPIReturnType();

		if (returnType != null) {

			try {

				schemaFromJsonString.putAll(new GsonBuilder().create().fromJson(returnType, Map.class));

			} catch (Throwable ignore) {}

			return new OpenAPIResultSchema(schemaFromJsonString, false);
			/*return new OpenAPIRequestResponse("The request was executed successfully.",

			);*/

		} else {

			return new OpenAPIResponseReference("#/components/responses/ok");
		}
	}

	// ----- static methods -----
	static StructrMethodDefinition deserialize(final StructrTypeDefinition parent, final String name, final Map<String, Object> source) {

		final StructrMethodDefinition newMethod = new StructrMethodDefinition(parent, name);

		newMethod.deserialize(source);

		return newMethod;
	}

	static StructrMethodDefinition deserialize(final StructrTypeDefinition parent, final SchemaMethod method) {

		final StructrMethodDefinition newMethod = new StructrMethodDefinition(parent, method.getName());

		newMethod.deserialize(method);

		return newMethod;
	}
}
