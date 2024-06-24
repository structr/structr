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
package org.structr.core.entity;

import graphql.Scalars;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLOutputType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.util.Iterables;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.event.RuntimeEventLog;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.relationship.SchemaExcludedViewProperty;
import org.structr.core.entity.relationship.SchemaNodeProperty;
import org.structr.core.entity.relationship.SchemaViewProperty;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.notion.PropertySetNotion;
import org.structr.core.property.*;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.SchemaHelper;
import org.structr.schema.SchemaHelper.Type;
import org.structr.schema.SourceFile;
import org.structr.schema.parser.*;

import java.util.*;

import static graphql.schema.GraphQLTypeReference.typeRef;
import org.structr.common.helper.ValidationHelper;
import static org.structr.core.entity.SchemaNode.GraphQLNodeReferenceName;

public class SchemaProperty extends SchemaReloadingNode implements PropertyDefinition {

	private static final Logger logger = LoggerFactory.getLogger(SchemaProperty.class.getName());

	private static final String schemaPropertyNamePattern = "[_A-Za-z][\\-_0-9A-Za-z]*";

	public static final Property<AbstractSchemaNode> schemaNode            = new StartNode<>("schemaNode", SchemaNodeProperty.class, new PropertySetNotion(AbstractNode.id, AbstractNode.name, SchemaNode.isBuiltinType));
	public static final Property<Iterable<SchemaView>> schemaViews         = new StartNodes<>("schemaViews", SchemaViewProperty.class, new PropertySetNotion(AbstractNode.id, AbstractNode.name));
	public static final Property<Iterable<SchemaView>> excludedViews       = new StartNodes<>("excludedViews", SchemaExcludedViewProperty.class, new PropertySetNotion(AbstractNode.id, AbstractNode.name));

	public static final Property<String>             declaringUuid         = new StringProperty("declaringUuid");
	public static final Property<String>             declaringClass        = new StringProperty("declaringClass");
	public static final Property<String>             defaultValue          = new StringProperty("defaultValue");
	public static final Property<String>             propertyType          = new StringProperty("propertyType").indexed();
	public static final Property<String>             contentType           = new StringProperty("contentType");
	public static final Property<String>             dbName                = new StringProperty("dbName");
	public static final Property<String>             fqcn                  = new StringProperty("fqcn");
	public static final Property<String>             format                = new StringProperty("format");
	public static final Property<String>             typeHint              = new StringProperty("typeHint");
	public static final Property<String>             hint                  = new StringProperty("hint");
	public static final Property<String>             category              = new StringProperty("category");
	public static final Property<Boolean>            notNull               = new BooleanProperty("notNull");
	public static final Property<Boolean>            compound              = new BooleanProperty("compound");
	public static final Property<Boolean>            unique                = new BooleanProperty("unique");
	public static final Property<Boolean>            indexed               = new BooleanProperty("indexed");
	public static final Property<Boolean>            readOnly              = new BooleanProperty("readOnly");
	public static final Property<Boolean>            isDynamic             = new BooleanProperty("isDynamic");
	public static final Property<Boolean>            isBuiltinProperty     = new BooleanProperty("isBuiltinProperty");
	public static final Property<Boolean>            isPartOfBuiltInSchema = new BooleanProperty("isPartOfBuiltInSchema");
	public static final Property<Boolean>            isDefaultInUi         = new BooleanProperty("isDefaultInUi");
	public static final Property<Boolean>            isDefaultInPublic     = new BooleanProperty("isDefaultInPublic");
	public static final Property<Boolean>            isCachingEnabled      = new BooleanProperty("isCachingEnabled").defaultValue(false);
	public static final Property<String>             contentHash           = new StringProperty("contentHash");
	public static final Property<String>             readFunction          = new StringProperty("readFunction");
	public static final Property<String>             writeFunction         = new StringProperty("writeFunction");
	public static final Property<String>             openAPIReturnType     = new StringProperty("openAPIReturnType");
	public static final Property<String[]>           validators            = new ArrayProperty("validators", String.class);
	public static final Property<String[]>           transformers          = new ArrayProperty("transformers", String.class);

	private static final Set<PropertyKey> schemaRebuildTriggerKeys = new LinkedHashSet<>(Arrays.asList(
		name, declaringUuid, declaringClass, defaultValue, propertyType, contentType, dbName, fqcn, format, typeHint, hint, category, notNull, compound, unique, indexed, readOnly,
		isDynamic, isBuiltinProperty, isPartOfBuiltInSchema, isDefaultInUi, isDefaultInPublic, isCachingEnabled, contentHash, validators, transformers

	));

	public static final View defaultView = new View(SchemaProperty.class, PropertyView.Public,
		id, type, name, dbName, schemaNode, schemaViews, excludedViews, propertyType, contentType, format, fqcn, typeHint, hint, category, notNull, compound, unique, indexed, readOnly, defaultValue, isBuiltinProperty, declaringClass, isDynamic, readFunction, writeFunction, openAPIReturnType, validators, transformers, isCachingEnabled
	);

	public static final View uiView = new View(SchemaProperty.class, PropertyView.Ui,
		id, type, name, dbName, createdBy, hidden, createdDate, lastModifiedDate, visibleToPublicUsers, visibleToAuthenticatedUsers, schemaNode, schemaViews, excludedViews, propertyType, contentType, fqcn, format, typeHint, hint, category, notNull, compound, unique, indexed, readOnly, defaultValue, isBuiltinProperty, declaringClass, isDynamic, readFunction, writeFunction, openAPIReturnType, validators, transformers, isCachingEnabled
	);

	public static final View schemaView = new View(SchemaProperty.class, "schema",
		id, type, name, dbName, schemaNode, excludedViews, schemaViews, propertyType, contentType, format, fqcn, typeHint, hint, category, notNull, compound, unique, indexed, readOnly, defaultValue, isBuiltinProperty, isDefaultInUi, isDefaultInPublic, declaringClass, isDynamic, readFunction, writeFunction, openAPIReturnType, validators, transformers, isCachingEnabled
	);

	public static final View exportView = new View(SchemaProperty.class, "export",
		id, type, name, schemaNode, schemaViews, excludedViews, dbName, propertyType, contentType, format, fqcn, typeHint, hint, category, notNull, compound, unique, indexed, readOnly, defaultValue, isBuiltinProperty, isDefaultInUi, isDefaultInPublic, declaringClass, isDynamic, readFunction, writeFunction, openAPIReturnType, validators, transformers, isCachingEnabled
	);

	private NotionPropertyParser notionPropertyParser           = null;
	private DoublePropertyParser doublePropertyParser           = null;
	private LongPropertyParser longPropertyParser               = null;
	private IntPropertyParser intPropertyParser                 = null;
	private DoubleArrayPropertyParser doubleArrayPropertyParser = null;
	private LongArrayPropertyParser longArrayPropertyParser     = null;
	private IntegerArrayPropertyParser intArrayPropertyParser   = null;

	@Override
	public String getPropertyName() {
		return getProperty(name);
	}

	@Override
	public Type getPropertyType() {

		final String _type = getProperty(propertyType);
		if (_type != null) {

			try {

				return Type.valueOf(_type);

			} catch (IllegalArgumentException ex) {

				throw new IllegalStateException("Invalid property type " + _type + " for property " + getPropertyName() + ".");

			}

		} else {

			throw new IllegalStateException("Invalid property type null for property " + getPropertyName() + ".");
		}
	}

	@Override
	public String getContentType() {
		return getProperty(contentType);
	}

	@Override
	public boolean isNotNull() {

		final Boolean isNotNull = getProperty(notNull);
		if (isNotNull != null && isNotNull) {

			return true;
		}

		return false;
	}

	@Override
	public boolean isCompound() {

		final Boolean isCompoundUnique = getProperty(compound);
		if (isCompoundUnique != null && isCompoundUnique) {

			return true;
		}

		return false;
	}

	@Override
	public boolean isUnique() {

		final Boolean isUnique = getProperty(unique);
		if (isUnique != null && isUnique) {

			return true;
		}

		return false;
	}

	@Override
	public boolean isIndexed() {

		final Boolean isIndexed = getProperty(indexed);
		if (isIndexed != null && isIndexed) {

			return true;
		}

		return false;
	}

	@Override
	public boolean isReadOnly() {

		final Boolean isReadOnly = getProperty(readOnly);
		if (isReadOnly != null && isReadOnly) {

			return true;
		}

		return false;
	}

	@Override
	public boolean isPartOfBuiltInSchema() {

		final Boolean _isPartOfBuiltInSchema = getProperty(SchemaProperty.isPartOfBuiltInSchema);
		if (_isPartOfBuiltInSchema != null && _isPartOfBuiltInSchema) {

			return true;
		}

		return false;
	}

	@Override
	public boolean isCachingEnabled() {

		final Boolean _isCachingEnabled = getProperty(SchemaProperty.isCachingEnabled);
		if (_isCachingEnabled != null && _isCachingEnabled) {

			return true;
		}

		return false;
	}

	@Override
	public String getRawSource() {
		return "";
	}

	@Override
	public String getSource() {
		return "";
	}

	@Override
	public String getDbName() {
		return getProperty(dbName);
	}

	@Override
	public String getDefaultValue() {
		return getProperty(defaultValue);
	}

	@Override
	public String getTypeHint() {
		return getProperty(typeHint);
	}

	@Override
	public boolean isValid(final ErrorBuffer errorBuffer) {

		boolean valid = super.isValid(errorBuffer);

		valid &= ValidationHelper.isValidStringMatchingRegex(this, name, schemaPropertyNamePattern, errorBuffer);

		return valid;
	}

	@Override
	public void onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {

		super.onCreation(securityContext, errorBuffer);

		// automatically add new property to the Ui or Custom view
		final AbstractSchemaNode parent = getProperty(SchemaProperty.schemaNode);
		if (parent != null) {

			// register property (so we have a chance to back up an existing builtin property)
			final ConfigurationProvider conf = StructrApp.getConfiguration();
			final Class type = conf.getNodeEntityClass(parent.getName());

			if (type != null) {
				conf.registerProperty(type, conf.getPropertyKeyForJSONName(type, getPropertyName()));
			}

			final String viewToAddTo;
			if (getProperty(isBuiltinProperty)) {
				viewToAddTo = PropertyView.Ui;
			} else {
				viewToAddTo = PropertyView.Custom;
			}

			for (final SchemaView view : parent.getProperty(AbstractSchemaNode.schemaViews)) {

				if (viewToAddTo.equals(view.getName())) {

					final Set<SchemaProperty> properties = Iterables.toSet(view.getProperty(SchemaView.schemaProperties));

					properties.add(this);

					view.setProperty(SchemaView.schemaProperties, new LinkedList<>(properties));

					break;
				}
			}
		}
	}

	@Override
	public void onModification(SecurityContext securityContext, ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		super.onModification(securityContext, errorBuffer, modificationQueue);

		final String uuid = getUuid();
		if (uuid != null) {

			// acknowledge all events for this node when it is modified
			RuntimeEventLog.getEvents(e -> uuid.equals(e.getData().get("id"))).stream().forEach(e -> e.acknowledge());
		}

		if (getProperty(schemaNode) == null) {
			StructrApp.getInstance().delete(this);
		} else {

			// prevent modification of properties using a content hash value
			if (getProperty(isBuiltinProperty) && !getContentHash().equals(getProperty(contentHash))) {
				throw new FrameworkException(403, "Modification of built-in properties not permitted.");
			}
		}
	}

	@Override
	public void onNodeDeletion(SecurityContext securityContext) throws FrameworkException {

		super.onNodeDeletion(securityContext);

		final String thisName = getName();

		// remove property from the sortOrder of views it is used in (directly)
		for (SchemaView view : getProperty(SchemaProperty.schemaViews)) {

			final String sortOrder = view.getProperty(SchemaView.sortOrder);

			if (sortOrder != null) {

				try {
					view.setProperty(SchemaView.sortOrder, StringUtils.join(Arrays.stream(sortOrder.split(",")).filter(propertyName -> !thisName.equals(propertyName)).toArray(), ","));
				} catch (FrameworkException ex) {
					logger.error("Unable to remove property '{}' from view '{}'", thisName, view.getUuid());
				}
			}
		}
	}

	public String getContentHash() {

		int _contentHash = 77;

		_contentHash = addContentHash(defaultValue,      _contentHash);
		_contentHash = addContentHash(propertyType,      _contentHash);
		_contentHash = addContentHash(contentType,       _contentHash);
		_contentHash = addContentHash(dbName,            _contentHash);
		_contentHash = addContentHash(format,            _contentHash);
		_contentHash = addContentHash(typeHint,          _contentHash);
		_contentHash = addContentHash(notNull,           _contentHash);
		_contentHash = addContentHash(unique,            _contentHash);
		_contentHash = addContentHash(indexed,           _contentHash);
		_contentHash = addContentHash(readOnly,          _contentHash);
		_contentHash = addContentHash(isDynamic,         _contentHash);
		_contentHash = addContentHash(isBuiltinProperty, _contentHash);
		_contentHash = addContentHash(isDefaultInUi,     _contentHash);
		_contentHash = addContentHash(isDefaultInPublic, _contentHash);
		_contentHash = addContentHash(isCachingEnabled,  _contentHash);
		_contentHash = addContentHash(readFunction,      _contentHash);
		_contentHash = addContentHash(writeFunction,     _contentHash);
		_contentHash = addContentHash(openAPIReturnType, _contentHash);
		_contentHash = addContentHash(transformers,      _contentHash);
		_contentHash = addContentHash(validators,        _contentHash);

		return Integer.toHexString(_contentHash);
	}

	@Override
	public String getFormat() {

		String _format = getProperty(SchemaProperty.format);
		if (_format != null) {

			_format = _format.trim();
		}

		return _format;
	}

	@Override
	public String getHint() {
		return getProperty(SchemaProperty.hint);
	}

	@Override
	public String getCategory() {
		return getProperty(SchemaProperty.category);
	}

	public boolean isRequired() {
		return getProperty(SchemaProperty.notNull);
	}

	public String getSourceContentType() {

		final String source = getFormat();
		if (source != null) {

			if (source.startsWith("{") && source.endsWith("}")) {

				return "application/x-structr-javascript";
			}
		}

		return null;
	}

	public Set<String> getEnumDefinitions() {

		final String _format    = getProperty(SchemaProperty.format);
		final Set<String> enums = new LinkedHashSet<>();

		if (_format != null) {

			for (final String source : _format.split("[, ]+")) {

				final String trimmed = source.trim();
				if (StringUtils.isNotBlank(trimmed)) {

					enums.add(trimmed);
				}
			}
		}

		return enums;
	}

	public boolean isPropertySetNotion(final Map<String, SchemaNode> schemaNodes) {
		return getNotionPropertyParser(schemaNodes).isPropertySet();
	}

	public String getTypeReferenceForNotionProperty(final Map<String, SchemaNode> schemaNodes) {
		return getNotionPropertyParser(schemaNodes).getValueType();

	}

	public Set<String> getPropertiesForNotionProperty(final Map<String, SchemaNode> schemaNodes) {

		final Set<String> properties = new LinkedHashSet<>();

		for (final String property : getNotionPropertyParser(schemaNodes).getProperties()) {

			if (property.contains(".")) {

				final String[] parts = property.split("[.]+");
				if (parts.length > 1) {

					final String type = parts[0];
					final String name = parts[1];

					properties.add(name);
				}

			} else {

				properties.add(property);
			}
		}

		return properties;
	}

	public String getNotionBaseProperty(final Map<String, SchemaNode> schemaNodes) {
		return getNotionPropertyParser(schemaNodes).getBaseProperty();
	}

	public String getNotionMultiplicity(final Map<String, SchemaNode> schemaNodes) {
		return getNotionPropertyParser(schemaNodes).getMultiplicity();
	}

	@Override
	public String getReadFunction() {

		String _readFunction = getProperty(SchemaProperty.readFunction);
		if (_readFunction != null) {

			_readFunction = _readFunction.trim();
		}

		return _readFunction;
	}

	@Override
	public String getWriteFunction() {

		String _writeFunction = getProperty(SchemaProperty.writeFunction);
		if (_writeFunction != null) {

			_writeFunction = _writeFunction.trim();
		}

		return _writeFunction;
	}

	@Override
	public String getOpenAPIReturnType() {
		String _openAPIReturnType = getProperty(SchemaProperty.openAPIReturnType);
		if (_openAPIReturnType != null) {

			_openAPIReturnType = _openAPIReturnType.trim();
		}

		return _openAPIReturnType;
	}

	@Override
	public String[] getTransformators() {
		return getProperty(SchemaProperty.transformers);
	}

	@Override
	public String[] getValidators() {
		return getProperty(SchemaProperty.validators);
	}

	@Override
	public String getFqcn() {
		return getProperty(fqcn);
	}

	public void setFqcn(final String value) throws FrameworkException {
		setProperty(fqcn, value);
	}

	public String getFullName() {

		final AbstractSchemaNode schemaNode = getProperty(SchemaProperty.schemaNode);
		final StringBuilder buf             = new StringBuilder();

		if (schemaNode != null) {

			buf.append(schemaNode.getProperty(SchemaNode.name));
			buf.append(".");
		}

		buf.append(getProperty(SchemaProperty.name));

		return buf.toString();
	}

	public GraphQLFieldDefinition getGraphQLField() {

		final GraphQLOutputType outputType = SchemaHelper.getGraphQLOutputTypeForProperty(this);
		if (outputType != null) {

			return GraphQLFieldDefinition
				.newFieldDefinition()
				.name(SchemaHelper.cleanPropertyName(getPropertyName()))
				.type(outputType)
				.arguments(SchemaProperty.getGraphQLArgumentsForType(getPropertyType()))
				.build();
		}

		return null;
	}

	public NotionPropertyParser getNotionPropertyParser(final Map<String, SchemaNode> schemaNodes) {

		if (notionPropertyParser == null) {

			try {
				notionPropertyParser = new NotionPropertyParser(new ErrorBuffer(), getName(), this);
				notionPropertyParser.getPropertySource(schemaNodes, new SourceFile(""), getProperty(SchemaProperty.schemaNode));

			} catch (FrameworkException ignore) {
				// ignore this error because we only need the property parser to extract
				// some information, the generated code is not used at all
			}
		}

		return notionPropertyParser;
	}

	public IntPropertyParser getIntPropertyParser(final Map<String, SchemaNode> schemaNodes) {

		if (intPropertyParser == null) {

			try {
				intPropertyParser = new IntPropertyParser(new ErrorBuffer(), getName(), this);
				intPropertyParser.getPropertySource(schemaNodes, new SourceFile(""), getProperty(SchemaProperty.schemaNode));

			} catch (FrameworkException fex) {
				// ignore this error because we only need the property parser to extract
				// some information, the generated code is not used at all
			}
		}

		return intPropertyParser;
	}

	public IntegerArrayPropertyParser getIntArrayPropertyParser(final Map<String, SchemaNode> schemaNodes) {

		if (intArrayPropertyParser == null) {

			try {
				intArrayPropertyParser = new IntegerArrayPropertyParser(new ErrorBuffer(), getName(), this);
				intArrayPropertyParser.getPropertySource(schemaNodes, new SourceFile(""), getProperty(SchemaProperty.schemaNode));

			} catch (FrameworkException fex) {
				// ignore this error because we only need the property parser to extract
				// some information, the generated code is not used at all
			}
		}

		return intArrayPropertyParser;
	}

	public LongPropertyParser getLongPropertyParser(final Map<String, SchemaNode> schemaNodes) {

		if (longPropertyParser == null) {

			try {
				longPropertyParser = new LongPropertyParser(new ErrorBuffer(), getName(), this);
				longPropertyParser.getPropertySource(schemaNodes, new SourceFile(""), getProperty(SchemaProperty.schemaNode));

			} catch (FrameworkException fex) {
				// ignore this error because we only need the property parser to extract
				// some information, the generated code is not used at all
			}
		}

		return longPropertyParser;
	}

	public LongArrayPropertyParser getLongArrayPropertyParser(final Map<String, SchemaNode> schemaNodes) {

		if (longArrayPropertyParser == null) {

			try {
				longArrayPropertyParser = new LongArrayPropertyParser(new ErrorBuffer(), getName(), this);
				longArrayPropertyParser.getPropertySource(schemaNodes, new SourceFile(""), getProperty(SchemaProperty.schemaNode));

			} catch (FrameworkException fex) {
				// ignore this error because we only need the property parser to extract
				// some information, the generated code is not used at all
			}
		}

		return longArrayPropertyParser;
	}

	public DoublePropertyParser getDoublePropertyParser(final Map<String, SchemaNode> schemaNodes) {

		if (doublePropertyParser == null) {

			try {
				doublePropertyParser = new DoublePropertyParser(new ErrorBuffer(), getName(), this);
				doublePropertyParser.getPropertySource(schemaNodes, new SourceFile(""), getProperty(SchemaProperty.schemaNode));

			} catch (FrameworkException fex) {
				// ignore this error because we only need the property parser to extract
				// some information, the generated code is not used at all
			}
		}

		return doublePropertyParser;
	}

	public DoubleArrayPropertyParser getDoubleArrayPropertyParser(final Map<String, SchemaNode> schemaNodes) {

		if (doubleArrayPropertyParser == null) {

			try {
				doubleArrayPropertyParser = new DoubleArrayPropertyParser(new ErrorBuffer(), getName(), this);
				doubleArrayPropertyParser.getPropertySource(schemaNodes, new SourceFile(""), getProperty(SchemaProperty.schemaNode));

			} catch (FrameworkException fex) {
				// ignore this error because we only need the property parser to extract
				// some information, the generated code is not used at all
			}
		}

		return doubleArrayPropertyParser;
	}

	@Override
	public boolean reloadSchemaOnCreate() {
		return true;
	}

	@Override
	public boolean reloadSchemaOnModify(final ModificationQueue modificationQueue) {

		final Set<PropertyKey> modifiedProperties = modificationQueue.getModifiedProperties();
		for (final PropertyKey triggerKey : schemaRebuildTriggerKeys) {

			if (modifiedProperties.contains(triggerKey)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean reloadSchemaOnDelete() {
		return true;
	}

	// ----- public static methods -----
	public static List<GraphQLArgument> getGraphQLArgumentsForType(final Type type) {

		final List<GraphQLArgument> arguments = new LinkedList<>();

		switch (type) {

			case String:
				arguments.add(GraphQLArgument.newArgument().name("_equals").type(Scalars.GraphQLString).build());
				arguments.add(GraphQLArgument.newArgument().name("_contains").type(Scalars.GraphQLString).build());
				arguments.add(GraphQLArgument.newArgument().name("_conj").type(Scalars.GraphQLString).build());
				break;

			case Integer:
				arguments.add(GraphQLArgument.newArgument().name("_equals").type(Scalars.GraphQLInt).build());
				arguments.add(GraphQLArgument.newArgument().name("_conj").type(Scalars.GraphQLString).build());
				break;

			case Long:
				arguments.add(GraphQLArgument.newArgument().name("_equals").type(Scalars.GraphQLLong).build());
				arguments.add(GraphQLArgument.newArgument().name("_conj").type(Scalars.GraphQLString).build());
				break;
		}

		return arguments;
	}

	public static List<GraphQLArgument> getGraphQLArgumentsForUUID() {

		final List<GraphQLArgument> arguments = new LinkedList<>();

		arguments.add(GraphQLArgument.newArgument().name("_equals").type(Scalars.GraphQLString).build());

		return arguments;
	}

	public static List<GraphQLArgument> getGraphQLArgumentsForRelatedType(final String relatedType) {

		// related type parameter is unused right now

		final List<GraphQLArgument> arguments = new LinkedList<>();

		arguments.add(GraphQLArgument.newArgument().name("_equals").type(typeRef(GraphQLNodeReferenceName)).build());
		arguments.add(GraphQLArgument.newArgument().name("_sort").type(Scalars.GraphQLString).build());
		arguments.add(GraphQLArgument.newArgument().name("_desc").type(Scalars.GraphQLString).build());

		return arguments;
	}

	// ----- private methods -----
	private int addContentHash(final PropertyKey key, final int contentHash) {

		final Object value = getProperty(key);
		if (value != null) {

			return contentHash ^ value.hashCode();
		}

		return contentHash;
	}
}
