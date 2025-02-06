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

import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.schema.SchemaHelper;
import org.structr.schema.parser.*;

import java.util.LinkedHashSet;
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
		return wrappedObject.getProperty(traits.key("name"));
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
	public SchemaHelper.Type getPropertyType() {

		final String _type = wrappedObject.getProperty(traits.key("propertyType"));
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
		return wrappedObject.getProperty(traits.key("contentType"));
	}

	@Override
	public boolean isNotNull() {

		final Boolean isNotNull = wrappedObject.getProperty(traits.key("notNull"));
		if (isNotNull != null && isNotNull) {

			return true;
		}

		return false;
	}

	@Override
	public boolean isCompound() {

		final Boolean isCompoundUnique = wrappedObject.getProperty(traits.key("compound"));
		if (isCompoundUnique != null && isCompoundUnique) {

			return true;
		}

		return false;
	}

	@Override
	public boolean isUnique() {

		final Boolean isUnique = wrappedObject.getProperty(traits.key("unique"));
		if (isUnique != null && isUnique) {

			return true;
		}

		return false;
	}

	@Override
	public boolean isIndexed() {

		final Boolean isIndexed = wrappedObject.getProperty(traits.key("indexed"));
		if (isIndexed != null && isIndexed) {

			return true;
		}

		return false;
	}

	@Override
	public boolean isReadOnly() {

		final Boolean isReadOnly = wrappedObject.getProperty(traits.key("readOnly"));
		if (isReadOnly != null && isReadOnly) {

			return true;
		}

		return false;
	}

	@Override
	public boolean isCachingEnabled() {

		final Boolean _isCachingEnabled = wrappedObject.getProperty(traits.key("isCachingEnabled"));
		if (_isCachingEnabled != null && _isCachingEnabled) {

			return true;
		}

		return false;
	}

	@Override
	public AbstractSchemaNode getSchemaNode() {

		final NodeInterface node = wrappedObject.getProperty(traits.key("schemaNode"));
		if (node != null) {

			return node.as(AbstractSchemaNode.class);
		}

		return null;
	}

	@Override
	public String getSource() {
		return wrappedObject.getProperty(traits.key("source"));
	}

	@Override
	public String getDbName() {
		return wrappedObject.getProperty(traits.key("dbName"));
	}

	@Override
	public String getDefaultValue() {
		return wrappedObject.getProperty(traits.key("defaultValue"));
	}

	@Override
	public String getTypeHint() {
		return wrappedObject.getProperty(traits.key("typeHint"));
	}

	@Override
	public String getFormat() {

		String _format = wrappedObject.getProperty(traits.key("format"));
		if (_format != null) {

			_format = _format.trim();
		}

		return _format;
	}

	@Override
	public String getHint() {
		return wrappedObject.getProperty(traits.key("hint"));
	}

	@Override
	public String getCategory() {
		return wrappedObject.getProperty(traits.key("category"));
	}

	public boolean isRequired() {
		return wrappedObject.getProperty(traits.key("notNull"));
	}

	@Override
	public String getReadFunction() {

		String _readFunction = wrappedObject.getProperty(traits.key("readFunction"));
		if (_readFunction != null) {

			_readFunction = _readFunction.trim();
		}

		return _readFunction;
	}

	@Override
	public String getWriteFunction() {

		String _writeFunction = wrappedObject.getProperty(traits.key("writeFunction"));
		if (_writeFunction != null) {

			_writeFunction = _writeFunction.trim();
		}

		return _writeFunction;
	}

	@Override
	public String getOpenAPIReturnType() {

		String _openAPIReturnType = wrappedObject.getProperty(traits.key("openAPIReturnType"));
		if (_openAPIReturnType != null) {

			_openAPIReturnType = _openAPIReturnType.trim();
		}

		return _openAPIReturnType;
	}

	@Override
	public String[] getTransformators() {
		return wrappedObject.getProperty(traits.key("transformers"));
	}

	@Override
	public String[] getValidators() {
		return wrappedObject.getProperty(traits.key("validators"));
	}

	@Override
	public String getFqcn() {
		return wrappedObject.getProperty(traits.key("fqcn"));
	}

	@Override
	public Set<String> getEnumDefinitions() {

		final String _format    = getFormat();
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

	public String getContentHash() {

		int _contentHash = 77;

		_contentHash = addContentHash(traits.key("defaultValue"),      _contentHash);
		_contentHash = addContentHash(traits.key("propertyType"),      _contentHash);
		_contentHash = addContentHash(traits.key("contentType"),       _contentHash);
		_contentHash = addContentHash(traits.key("dbName"),            _contentHash);
		_contentHash = addContentHash(traits.key("format"),            _contentHash);
		_contentHash = addContentHash(traits.key("typeHint"),          _contentHash);
		_contentHash = addContentHash(traits.key("notNull"),           _contentHash);
		_contentHash = addContentHash(traits.key("unique"),            _contentHash);
		_contentHash = addContentHash(traits.key("indexed"),           _contentHash);
		_contentHash = addContentHash(traits.key("readOnly"),          _contentHash);
		_contentHash = addContentHash(traits.key("isDynamic"),         _contentHash);
		_contentHash = addContentHash(traits.key("isBuiltinProperty"), _contentHash);
		_contentHash = addContentHash(traits.key("isDefaultInUi"),     _contentHash);
		_contentHash = addContentHash(traits.key("isDefaultInPublic"), _contentHash);
		_contentHash = addContentHash(traits.key("isCachingEnabled"),  _contentHash);
		_contentHash = addContentHash(traits.key("readFunction"),      _contentHash);
		_contentHash = addContentHash(traits.key("writeFunction"),     _contentHash);
		_contentHash = addContentHash(traits.key("openAPIReturnType"), _contentHash);
		_contentHash = addContentHash(traits.key("transformers"),      _contentHash);
		_contentHash = addContentHash(traits.key("validators"),        _contentHash);

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
	public PropertyKey createKey(final AbstractSchemaNode entity) throws FrameworkException {

		final ErrorBuffer errorBuffer = new ErrorBuffer();

		PropertyGenerator generator = SchemaHelper.getPropertyGenerator(errorBuffer, entity, this);

		return generator.createKey();
	}

	@Override
	public List<IsValid> createValidators(final AbstractSchemaNode entity) throws FrameworkException {

		final ErrorBuffer errorBuffer     = new ErrorBuffer();
		final PropertyGenerator generator = SchemaHelper.getPropertyGenerator(errorBuffer, entity, this);

		return generator.getValidators(getPropertyName());
	}

	public void setFqcn(final String value) throws FrameworkException {
		wrappedObject.setProperty(traits.key("fqcn"), value);
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
			notionPropertyParser.setSchemaNode(getSchemaNode().as(SchemaNode.class));
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
