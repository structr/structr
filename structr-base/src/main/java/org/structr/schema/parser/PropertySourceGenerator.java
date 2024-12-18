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
package org.structr.schema.parser;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.ErrorToken;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.Traits;
import org.structr.schema.Schema;
import org.structr.schema.SchemaHelper;
import org.structr.schema.SchemaHelper.Type;
import org.structr.schema.SourceFile;
import org.structr.schema.SourceLine;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 *
 */
public abstract class PropertySourceGenerator {

	private final Set<String> compoundIndexKeys   = new LinkedHashSet<>();
	private final Set<Validator> globalValidators = new LinkedHashSet<>();
	private final Set<String> enumDefinitions     = new LinkedHashSet<>();
	protected PropertyDefinition source           = null;
	protected ErrorBuffer errorBuffer             = null;
	protected String className                    = "";


	public abstract Type getKey();
	public abstract String getPropertyType();
	public abstract String getValueType();
	public abstract String getUnqualifiedValueType();
	public abstract String getPropertyParameters();
	public abstract void parseFormatString(final Map<String, SchemaNode> schemaNodes, final NodeInterface entity, final String expression) throws FrameworkException;

	public PropertySourceGenerator(final ErrorBuffer errorBuffer, final String className, final PropertyDefinition propertyDefinition) {
		this.errorBuffer  = errorBuffer;
		this.className    = className;
		this.source       = propertyDefinition;
	}

	public void getPropertySource(final Map<String, SchemaNode> schemaNodes, final SourceFile buf, final NodeInterface entity) throws FrameworkException {

		parseFormatString(schemaNodes, entity, source.getFormat());

		if (source.isNotNull()) {

			globalValidators.add(new Validator("isValidPropertyNotNull", className, source.getPropertyName()));
		}

		if (source.isUnique()) {

			globalValidators.add(new Validator("isValidUniqueProperty", className, source.getPropertyName()));
		}

		if (source.isCompound()) {

			compoundIndexKeys.add(SchemaHelper.cleanPropertyName(source.getPropertyName()) + "Property");
		}

		getPropertySource(buf);
	}

	public String getClassName() {
		return className;
	}

	public Set<String> getCompoundIndexKeys() {
		return compoundIndexKeys;
	}

	public Set<Validator> getGlobalValidators() {
		return globalValidators;
	}

	public void addGlobalValidator(final Validator validator) {
		globalValidators.add(validator);
	}

	public Set<String> getEnumDefinitions() {
		return enumDefinitions;
	}

	public String getSourcePropertyName() {
		return source.getPropertyName();
	}

	public void reportError(final ErrorToken error) {
		errorBuffer.add(error);
	}

	public ErrorBuffer getErrorBuffer() {
		return errorBuffer;
	}

	public void addEnumDefinition(final String item) {
		enumDefinitions.add(item);
	}

	public String getSourceDefaultValue() {
		return source.getDefaultValue();
	}

	public String getDefaultValue() {
		return getSourceDefaultValue();
	}

	public void createSchemaPropertyNode(final NodeInterface node, final String underscorePropertyName) throws FrameworkException {

		final App app                       = StructrApp.getInstance();
		final String propertyName           = getSourcePropertyName();
		final AbstractSchemaNode schemaNode = node.as(AbstractSchemaNode.class);
		final Traits traits                 = Traits.of("SchemaProperty");

		if (schemaNode.getSchemaProperty(propertyName) == null) {

			app.create("SchemaProperty",
				new NodeAttribute<>(traits.key("name"),                  propertyName),
				new NodeAttribute<>(traits.key("schemaNode"),            schemaNode),
				new NodeAttribute<>(traits.key("propertyType"),          getKey().name()),
				new NodeAttribute<>(traits.key("contentType"),           source.getContentType()),
				new NodeAttribute<>(traits.key("dbName"),                source.getDbName()),
				new NodeAttribute<>(traits.key("defaultValue"),          source.getDefaultValue()),
				new NodeAttribute<>(traits.key("format"),                source.getFormat()),
				new NodeAttribute<>(traits.key("typeHint"),              source.getTypeHint()),
				new NodeAttribute<>(traits.key("hint"),                  source.getHint()),
				new NodeAttribute<>(traits.key("category"),              source.getCategory()),
				new NodeAttribute<>(traits.key("fqcn"),                  source.getFqcn()),
				new NodeAttribute<>(traits.key("compound"),              source.isCompound()),
				new NodeAttribute<>(traits.key("unique"),                source.isUnique()),
				new NodeAttribute<>(traits.key("indexed"),               source.isIndexed()),
				new NodeAttribute<>(traits.key("notNull"),               source.isNotNull()),
				new NodeAttribute<>(traits.key("isPartOfBuiltInSchema"), source.isPartOfBuiltInSchema()),
				new NodeAttribute<>(traits.key("isCachingEnabled"),      source.isCachingEnabled()),
				new NodeAttribute<>(traits.key("readFunction"),          source.getReadFunction()),
				new NodeAttribute<>(traits.key("writeFunction"),         source.getWriteFunction()),
				new NodeAttribute<>(traits.key("openAPIReturnType"),     source.getOpenAPIReturnType()),
				new NodeAttribute<>(traits.key("transformers"),          source.getTransformators()),
				new NodeAttribute<>(traits.key("validators"),            source.getValidators())
			);

			node.removeProperty(new StringProperty(underscorePropertyName));
		}
	}

	// ----- protected methods -----
	protected void getPropertySource(final SourceFile src) {

		final String sourceUuid = source.getUuid();

		final SourceLine line = src.line(source, "public static final");

		line.append(" Property<");
		line.append(getValueType());
		line.append("> ");
		line.append(SchemaHelper.cleanPropertyName(source.getPropertyName()));
		line.append("Property");
		line.append(" = new ");
		line.append(getPropertyType());
		line.append("(");
		line.quoted(source.getPropertyName());

		if (StringUtils.isNotBlank(source.getDbName())) {
			line.append(", ");
			line.quoted(source.getDbName());
		}

		if (getPropertyParameters() != null) {
			line.append(getPropertyParameters());
		}

		line.append(")");

		if (StringUtils.isNotBlank(source.getContentType())) {
			line.append(".contentType(").quoted(source.getContentType()).append(")");
		}

		if (StringUtils.isNotBlank(source.getDefaultValue())) {
			line.append(".defaultValue(").append(getDefaultValue()).append(")");
		}

		if (StringUtils.isNotBlank(source.getFormat())) {
			line.append(".format(").quoted(StringEscapeUtils.escapeJava(source.getFormat())).append(")");
		}

		if (StringUtils.isNotBlank(sourceUuid)) {

			line.append(".setSourceUuid(").quoted(sourceUuid).append(")");

		} else {

			if (StringUtils.isNotBlank(source.getReadFunction())) {
				line.append(".readFunction(").quoted(StringEscapeUtils.escapeJava(source.getReadFunction())).append(")");
			}

			if (StringUtils.isNotBlank(source.getWriteFunction())) {
				line.append(".writeFunction(").quoted(StringEscapeUtils.escapeJava(source.getWriteFunction())).append(")");
			}
		}

		if (StringUtils.isNotBlank(source.getTypeHint())) {
			line.append(".typeHint(").quoted(StringEscapeUtils.escapeJava(source.getTypeHint())).append(")");
		}

		if (source.isUnique()) {
			line.append(".unique()");
		}

		if (source.isCompound()) {
			line.append(".compound()");
		}

		if (source.isNotNull()) {
			line.append(".notNull()");
		}

		if (source.isCachingEnabled()) {
			line.append(".cachingEnabled(true)");
		}

		if (source.isIndexed()) {

			if (StringUtils.isNotBlank(source.getDefaultValue())) {

				line.append(".indexedWhenEmpty()");

			} else {

				line.append(".indexed()");
			}
		}

		if (source.isReadOnly()) {

			line.append(".readOnly()");
		}

		final String[] transformators = source.getTransformators();
		if (transformators != null && transformators.length > 0) {

			line.append(".transformators(");
			line.quoted(StringUtils.join(transformators, "\", \""));
			line.append(")");
		}

		if (source.isPartOfBuiltInSchema()) {
			line.append(".partOfBuiltInSchema()");
		}

		line.append(".dynamic()");

		if (StringUtils.isNotBlank(source.getHint())) {
			line.append(".hint(").quoted(StringEscapeUtils.escapeJava(source.getHint())).append(")");
		}

		if (StringUtils.isNotBlank(source.getCategory())) {
			line.append(".category(").quoted(StringEscapeUtils.escapeJava(source.getCategory())).append(")");
		}

		line.append(";");
	}
}
