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
package org.structr.core.traits.wrappers;

import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.SchemaPropertyTraitDefinition;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.schema.SchemaHelper;
import org.structr.schema.parser.*;

import java.util.List;
import java.util.Set;

public class SchemaPropertyTraitWrapper extends AbstractNodeTraitWrapper implements SchemaProperty {

	private NotionPropertyGenerator notionPropertyParser           = null;
	private DoublePropertyGenerator doublePropertyParser           = null;
	private LongPropertyGenerator longPropertyParser               = null;
	private IntegerPropertyGenerator intPropertyParser             = null;
	private DoubleArrayPropertyGenerator doubleArrayPropertyParser = null;
	private LongArrayPropertyGenerator longArrayPropertyParser     = null;
	private IntegerArrayPropertyGenerator intArrayPropertyParser   = null;

	public SchemaPropertyTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public String getName() {
		return wrappedObject.getProperty(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY));
	}

	@Override
	public String getPropertyName() {
		return getName();
	}

	@Override
	public String getClassName() {
		return getSchemaNode().getClassName();
	}

	@Override
	public String getStaticSchemaNodeName() {
		return wrappedObject.getProperty(traits.key(SchemaPropertyTraitDefinition.STATIC_SCHEMA_NODE_NAME_PROPERTY));
	}

	@Override
	public SchemaHelper.Type getPropertyType() {

		final String _type = wrappedObject.getProperty(traits.key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY));
		if (_type != null) {

			try {

				return SchemaHelper.Type.valueOf(_type);

			} catch (IllegalArgumentException ex) {

				throw new IllegalStateException("Invalid property type " + _type + " for property " + getPropertyName() + ".");

			}

		} else {

			throw new IllegalStateException("Invalid property type null for property " + getPropertyName() + ".");
		}
	}

	@Override
	public String getContentType() {
		return wrappedObject.getProperty(traits.key(SchemaPropertyTraitDefinition.CONTENT_TYPE_PROPERTY));
	}

	@Override
	public boolean isNotNull() {

		final Boolean isNotNull = wrappedObject.getProperty(traits.key(SchemaPropertyTraitDefinition.NOT_NULL_PROPERTY));
		if (isNotNull != null && isNotNull) {

			return true;
		}

		return false;
	}

	@Override
	public boolean isCompound() {

		final Boolean isCompoundUnique = wrappedObject.getProperty(traits.key(SchemaPropertyTraitDefinition.COMPOUND_PROPERTY));

		return Boolean.TRUE.equals(isCompoundUnique);
	}

	@Override
	public boolean isUnique() {

		final Boolean isUnique = wrappedObject.getProperty(traits.key(SchemaPropertyTraitDefinition.UNIQUE_PROPERTY));

		return Boolean.TRUE.equals(isUnique);
	}

	@Override
	public boolean isIndexed() {

		final Boolean isIndexed = wrappedObject.getProperty(traits.key(SchemaPropertyTraitDefinition.INDEXED_PROPERTY));

		return Boolean.TRUE.equals(isIndexed);
	}

	@Override
	public boolean isReadOnly() {

		final Boolean isReadOnly = wrappedObject.getProperty(traits.key(SchemaPropertyTraitDefinition.READ_ONLY_PROPERTY));

		return Boolean.TRUE.equals(isReadOnly);
	}

	@Override
	public boolean isCachingEnabled() {

		final Boolean _isCachingEnabled = wrappedObject.getProperty(traits.key(SchemaPropertyTraitDefinition.IS_CACHING_ENABLED_PROPERTY));

		return Boolean.TRUE.equals(_isCachingEnabled);
	}

	@Override
	public AbstractSchemaNode getSchemaNode() {

		final NodeInterface node = wrappedObject.getProperty(traits.key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY));
		if (node != null) {

			return node.as(AbstractSchemaNode.class);
		}

		return null;
	}

	@Override
	public String getDbName() {
		return wrappedObject.getProperty(traits.key(SchemaPropertyTraitDefinition.DB_NAME_PROPERTY));
	}

	@Override
	public String getDefaultValue() {
		return wrappedObject.getProperty(traits.key(SchemaPropertyTraitDefinition.DEFAULT_VALUE_PROPERTY));
	}

	@Override
	public String getTypeHint() {
		return wrappedObject.getProperty(traits.key(SchemaPropertyTraitDefinition.TYPE_HINT_PROPERTY));
	}

	@Override
	public String getFormat() {

		String _format = wrappedObject.getProperty(traits.key(SchemaPropertyTraitDefinition.FORMAT_PROPERTY));
		if (_format != null) {

			_format = _format.trim();
		}

		return _format;
	}

	@Override
	public String getHint() {
		return wrappedObject.getProperty(traits.key(SchemaPropertyTraitDefinition.HINT_PROPERTY));
	}

	@Override
	public String getCategory() {
		return wrappedObject.getProperty(traits.key(SchemaPropertyTraitDefinition.CATEGORY_PROPERTY));
	}

	public boolean isRequired() {
		return wrappedObject.getProperty(traits.key(SchemaPropertyTraitDefinition.NOT_NULL_PROPERTY));
	}

	@Override
	public String getReadFunction() {

		String _readFunction = wrappedObject.getProperty(traits.key(SchemaPropertyTraitDefinition.READ_FUNCTION_PROPERTY));
		if (_readFunction != null) {

			_readFunction = _readFunction.trim();
		}

		return _readFunction;
	}

	@Override
	public String getWriteFunction() {

		String _writeFunction = wrappedObject.getProperty(traits.key(SchemaPropertyTraitDefinition.WRITE_FUNCTION_PROPERTY));
		if (_writeFunction != null) {

			_writeFunction = _writeFunction.trim();
		}

		return _writeFunction;
	}

	@Override
	public String getOpenAPIReturnType() {

		String _openAPIReturnType = wrappedObject.getProperty(traits.key(SchemaPropertyTraitDefinition.OPEN_API_RETURN_TYPE_PROPERTY));
		if (_openAPIReturnType != null) {

			_openAPIReturnType = _openAPIReturnType.trim();
		}

		return _openAPIReturnType;
	}

	@Override
	public String[] getTransformators() {
		return wrappedObject.getProperty(traits.key(SchemaPropertyTraitDefinition.TRANSFORMERS_PROPERTY));
	}

	@Override
	public String[] getValidators() {
		return wrappedObject.getProperty(traits.key(SchemaPropertyTraitDefinition.VALIDATORS_PROPERTY));
	}

	@Override
	public String getMultiplicity(final String baseProperty) {

		final AbstractSchemaNode abstractSchemaNode = getSchemaNode();
		if (abstractSchemaNode != null && abstractSchemaNode.is(StructrTraits.SCHEMA_NODE)) {

			final SchemaNode schemaNode = abstractSchemaNode.as(SchemaNode.class);

			return schemaNode.getMultiplicity(baseProperty);
		}

		return null;
	}

	@Override
	public String getRelatedType(final String baseProperty) {

		final AbstractSchemaNode abstractSchemaNode = getSchemaNode();
		if (abstractSchemaNode != null && abstractSchemaNode.is(StructrTraits.SCHEMA_NODE)) {

			final SchemaNode schemaNode = abstractSchemaNode.as(SchemaNode.class);

			return schemaNode.getRelatedType(baseProperty);
		}

		return null;
	}

	@Override
	public String getFqcn() {
		return wrappedObject.getProperty(traits.key(SchemaPropertyTraitDefinition.FQCN_PROPERTY));
	}

	public String getContentHash() {

		int _contentHash = 77;

		_contentHash = addContentHash(traits.key(SchemaPropertyTraitDefinition.DEFAULT_VALUE_PROPERTY),        _contentHash);
		_contentHash = addContentHash(traits.key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY),        _contentHash);
		_contentHash = addContentHash(traits.key(SchemaPropertyTraitDefinition.CONTENT_TYPE_PROPERTY),         _contentHash);
		_contentHash = addContentHash(traits.key(SchemaPropertyTraitDefinition.DB_NAME_PROPERTY),              _contentHash);
		_contentHash = addContentHash(traits.key(SchemaPropertyTraitDefinition.FORMAT_PROPERTY),               _contentHash);
		_contentHash = addContentHash(traits.key(SchemaPropertyTraitDefinition.TYPE_HINT_PROPERTY),            _contentHash);
		_contentHash = addContentHash(traits.key(SchemaPropertyTraitDefinition.NOT_NULL_PROPERTY),             _contentHash);
		_contentHash = addContentHash(traits.key(SchemaPropertyTraitDefinition.UNIQUE_PROPERTY),               _contentHash);
		_contentHash = addContentHash(traits.key(SchemaPropertyTraitDefinition.INDEXED_PROPERTY),              _contentHash);
		_contentHash = addContentHash(traits.key(SchemaPropertyTraitDefinition.READ_ONLY_PROPERTY),            _contentHash);
		_contentHash = addContentHash(traits.key(SchemaPropertyTraitDefinition.IS_DYNAMIC_PROPERTY),           _contentHash);
		_contentHash = addContentHash(traits.key(SchemaPropertyTraitDefinition.IS_BUILTIN_PROPERTY_PROPERTY),  _contentHash);
		_contentHash = addContentHash(traits.key(SchemaPropertyTraitDefinition.IS_CACHING_ENABLED_PROPERTY),   _contentHash);
		_contentHash = addContentHash(traits.key(SchemaPropertyTraitDefinition.READ_FUNCTION_PROPERTY),        _contentHash);
		_contentHash = addContentHash(traits.key(SchemaPropertyTraitDefinition.WRITE_FUNCTION_PROPERTY),       _contentHash);
		_contentHash = addContentHash(traits.key(SchemaPropertyTraitDefinition.OPEN_API_RETURN_TYPE_PROPERTY), _contentHash);
		_contentHash = addContentHash(traits.key(SchemaPropertyTraitDefinition.TRANSFORMERS_PROPERTY),         _contentHash);
		_contentHash = addContentHash(traits.key(SchemaPropertyTraitDefinition.VALIDATORS_PROPERTY),           _contentHash);

		return Integer.toHexString(_contentHash);
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

	public boolean isPropertySetNotion() {
		return getNotionPropertyParser().isPropertySet();
	}

	public String getTypeReferenceForNotionProperty() {
		return getNotionPropertyParser().getValueType();
	}

	@Override
	public Set<String> getPropertiesForNotionProperty() {
		return getNotionPropertyParser().getProperties();
	}

	@Override
	public String getNotionBaseProperty() {
		return getNotionPropertyParser().getBaseProperty();
	}

	@Override
	public String getNotionMultiplicity() {
		return getNotionPropertyParser().getMultiplicity();
	}

	@Override
	public PropertyKey createKey(final String className) throws FrameworkException {

		final ErrorBuffer errorBuffer = new ErrorBuffer();

		PropertyGenerator generator = SchemaHelper.getPropertyGenerator(errorBuffer, className, this);

		return generator.createKey();
	}

	@Override
	public List<IsValid> createValidators(final AbstractSchemaNode entity) throws FrameworkException {

		final ErrorBuffer errorBuffer     = new ErrorBuffer();
		final PropertyGenerator generator = SchemaHelper.getPropertyGenerator(errorBuffer, entity.getClassName(), this);

		return generator.getValidators(getPropertyName());
	}

	public void setFqcn(final String value) throws FrameworkException {
		wrappedObject.setProperty(traits.key(SchemaPropertyTraitDefinition.FQCN_PROPERTY), value);
	}

	public String getFullName() {

		final AbstractSchemaNode schemaNode = getSchemaNode();
		final StringBuilder buf             = new StringBuilder();

		if (schemaNode != null) {

			buf.append(schemaNode.getName());
			buf.append(".");
		}

		buf.append(getPropertyName());

		return buf.toString();
	}

	@Override
	public NotionPropertyGenerator getNotionPropertyParser() {

		if (notionPropertyParser == null) {

			notionPropertyParser = new NotionPropertyGenerator(new ErrorBuffer(), getPropertyName(), this);

			// we need to initialize the parser
			notionPropertyParser.createKey();
		}

		return notionPropertyParser;
	}

	@Override
	public IntegerPropertyGenerator getIntPropertyParser() {

		if (intPropertyParser == null) {

			intPropertyParser = new IntegerPropertyGenerator(new ErrorBuffer(), getPropertyName(), this);
		}

		return intPropertyParser;
	}

	@Override
	public LongPropertyGenerator getLongPropertyParser() {

		if (longPropertyParser == null) {

			longPropertyParser = new LongPropertyGenerator(new ErrorBuffer(), getPropertyName(), this);
		}

		return longPropertyParser;
	}

	@Override
	public DoublePropertyGenerator getDoublePropertyParser() {

		if (doublePropertyParser == null) {

			doublePropertyParser = new DoublePropertyGenerator(new ErrorBuffer(), getPropertyName(), this);
		}

		return doublePropertyParser;
	}

	// ----- private methods -----
	private int addContentHash(final PropertyKey key, final int contentHash) {

		final Object value = wrappedObject.getProperty(key);
		if (value != null) {

			return contentHash ^ value.hashCode();
		}

		return contentHash;
	}
}
