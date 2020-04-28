/**
 * Copyright (C) 2010-2020 Structr GmbH
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.SchemaMethodParameter;
import org.structr.core.property.PropertyMap;
import org.structr.api.schema.JsonMethod;
import org.structr.api.schema.JsonParameter;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;

/**
 *
 *
 */
public class StructrMethodDefinition implements JsonMethod, StructrDefinition {

	private static final Logger logger = LoggerFactory.getLogger(StructrMethodDefinition.class.getName());

	private final List<StructrParameterDefinition> parameters = new LinkedList<>();
	private final List<String> exceptions                     = new LinkedList<>();
	private SchemaMethod schemaMethod                         = null;
	private boolean overridesExisting                         = false;
	private boolean doExport                                  = false;
	private boolean callSuper                                 = false;
	private JsonType parent                                   = null;
	private String returnType                                 = "void";
	private String codeType                                   = null;
	private String name                                       = null;
	private String comment                                    = null;
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
	public String getComment() {
		return comment;
	}

	@Override
	public JsonMethod setComment(String comment) {
		this.comment = comment;
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
	public boolean callSuper() {
		return callSuper;
	}

	@Override
	public JsonMethod setCallSuper(final boolean callSuper) {
		this.callSuper = callSuper;
		return this;
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
	public StructrDefinition resolveJsonPointerKey(final String key) {
		return null;
	}

	// ----- package methods -----
	SchemaMethod getSchemaMethod() {
		return schemaMethod;
	}

	SchemaMethod createDatabaseSchema(final App app, final AbstractSchemaNode schemaNode) throws FrameworkException {

		final PropertyMap getOrCreateProperties = new PropertyMap();
		final PropertyMap updateProperties      = new PropertyMap();
		int index                               = 0;

		getOrCreateProperties.put(SchemaMethod.name,                  getName());
		getOrCreateProperties.put(SchemaMethod.signature,             getSignature());
		getOrCreateProperties.put(SchemaMethod.codeType,              getCodeType());
		getOrCreateProperties.put(SchemaMethod.returnType,            getReturnType());
		getOrCreateProperties.put(SchemaMethod.schemaNode,            schemaNode);
		getOrCreateProperties.put(SchemaMethod.comment,               getComment());
		getOrCreateProperties.put(SchemaMethod.exceptions,            getExceptions().toArray(new String[0]));
		getOrCreateProperties.put(SchemaMethod.overridesExisting,     overridesExisting());
		getOrCreateProperties.put(SchemaMethod.callSuper,             callSuper());
		getOrCreateProperties.put(SchemaMethod.doExport,              doExport());

		SchemaMethod method = app.nodeQuery(SchemaMethod.class).and(getOrCreateProperties).getFirst();
		if (method == null) {

			method = app.create(SchemaMethod.class, getOrCreateProperties);
		}

		updateProperties.put(SchemaMethod.source,                getSource());
		updateProperties.put(SchemaMethod.isPartOfBuiltInSchema, true);

		method.setProperties(SecurityContext.getSuperUserInstance(), updateProperties);

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

		final Object _comment = source.get(JsonSchema.KEY_COMMENT);
		if (_comment != null && _comment instanceof String) {

			this.comment = (String)_comment;
		}

		final Object _codeType = source.get(JsonSchema.KEY_CODE_TYPE);
		if (_codeType != null && _codeType instanceof String) {

			this.codeType = (String)_codeType;
		}

		final Object _returnType = source.get(JsonSchema.KEY_RETURN_TYPE);
		if (_returnType != null && _returnType instanceof String) {

			this.returnType = (String)_returnType;
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
	}

	void deserialize(final SchemaMethod method) {

		this.schemaMethod = method;

		setName(method.getName());
		setSource(method.getProperty(SchemaMethod.source));
		setComment(method.getProperty(SchemaMethod.comment));
		setCodeType(method.getProperty(SchemaMethod.codeType));
		setReturnType(method.getProperty(SchemaMethod.returnType));
		setCallSuper(method.getProperty(SchemaMethod.callSuper));
		setOverridesExisting(method.getProperty(SchemaMethod.overridesExisting));
		setDoExport(method.getProperty(SchemaMethod.doExport));

		final String[] exceptionArray = method.getProperty(SchemaMethod.exceptions);
		if (exceptionArray != null) {

			for (final String fqcn : exceptionArray) {
				addException(fqcn);
			}
		}

		for (final SchemaMethodParameter param : method.getProperty(SchemaMethod.parameters)) {

			final StructrParameterDefinition parameter = StructrParameterDefinition.deserialize(this, param);
			if (parameter != null) {

				parameters.add(parameter);
			}
		}

		Collections.sort(parameters, (p1, p2) -> {
			return Integer.valueOf(p1.getIndex()).compareTo(p2.getIndex());
		});
	}

	Map<String, Object> serialize() {

		final Map<String, Object> map    = new TreeMap<>();
		final Map<String, Object> params = new LinkedHashMap<>();

		map.put(JsonSchema.KEY_SOURCE, source);
		map.put(JsonSchema.KEY_COMMENT, comment);
		map.put(JsonSchema.KEY_CODE_TYPE, codeType);
		map.put(JsonSchema.KEY_RETURN_TYPE, returnType);
		map.put(JsonSchema.KEY_EXCEPTIONS, exceptions);
		map.put(JsonSchema.KEY_CALL_SUPER, callSuper);
		map.put(JsonSchema.KEY_OVERRIDES_EXISTING, overridesExisting);
		map.put(JsonSchema.KEY_DO_EXPORT, doExport);

		for (final StructrParameterDefinition param : parameters) {
			params.put(param.getName(), param.serialize());
		}

		if (!params.isEmpty()) {
			map.put(JsonSchema.KEY_PARAMETERS, params);
		}

		return map;
	}

	void initializeReferences() {
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
