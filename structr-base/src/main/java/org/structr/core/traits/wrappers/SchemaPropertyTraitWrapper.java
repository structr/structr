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
import org.structr.schema.SchemaHelper;
import org.structr.schema.SourceFile;
import org.structr.schema.parser.*;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class SchemaPropertyTraitWrapper extends AbstractTraitWrapper<NodeInterface> implements SchemaProperty {

	private NotionPropertyParser notionPropertyParser           = null;
	private DoublePropertyParser doublePropertyParser           = null;
	private LongPropertyParser longPropertyParser               = null;
	private IntPropertyParser intPropertyParser                 = null;
	private DoubleArrayPropertyParser doubleArrayPropertyParser = null;
	private LongArrayPropertyParser longArrayPropertyParser     = null;
	private IntegerArrayPropertyParser intArrayPropertyParser   = null;

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
	public boolean isPartOfBuiltInSchema() {

		final Boolean _isPartOfBuiltInSchema = wrappedObject.getProperty(traits.key("isPartOfBuiltInSchema"));
		if (_isPartOfBuiltInSchema != null && _isPartOfBuiltInSchema) {

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
	public String getRawSource() {
		return "";
	}

	@Override
	public String getSource() {
		return "";
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

	public boolean isPropertySetNotion(final Map<String, SchemaNode> schemaNodes) {
		return getNotionPropertyParser(schemaNodes).isPropertySet();
	}

	public String getTypeReferenceForNotionProperty(final Map<String, SchemaNode> schemaNodes) {
		return getNotionPropertyParser(schemaNodes).getValueType();

	}

	@Override
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

	@Override
	public String getNotionBaseProperty(final Map<String, SchemaNode> schemaNodes) {
		return getNotionPropertyParser(schemaNodes).getBaseProperty();
	}

	@Override
	public String getNotionMultiplicity(final Map<String, SchemaNode> schemaNodes) {
		return getNotionPropertyParser(schemaNodes).getMultiplicity();
	}

	public void setFqcn(final String value) throws FrameworkException {
		wrappedObject.setProperty(traits.key("fqcn"), value);
	}

	public String getFullName() {

		final NodeInterface schemaNode = getSchemaNode();
		final StringBuilder buf        = new StringBuilder();

		if (schemaNode != null) {

			buf.append(schemaNode.getName());
			buf.append(".");
		}

		buf.append(getPropertyName());

		return buf.toString();
	}

	public NotionPropertyParser getNotionPropertyParser(final Map<String, SchemaNode> schemaNodes) {

		if (notionPropertyParser == null) {

			try {
				notionPropertyParser = new NotionPropertyParser(new ErrorBuffer(), getPropertyName(), this);
				notionPropertyParser.getPropertySource(schemaNodes, new SourceFile(""), getSchemaNode());

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
				intPropertyParser = new IntPropertyParser(new ErrorBuffer(), getPropertyName(), this);
				intPropertyParser.getPropertySource(schemaNodes, new SourceFile(""), getSchemaNode());

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
				intArrayPropertyParser = new IntegerArrayPropertyParser(new ErrorBuffer(), getPropertyName(), this);
				intArrayPropertyParser.getPropertySource(schemaNodes, new SourceFile(""), getSchemaNode());

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
				longPropertyParser = new LongPropertyParser(new ErrorBuffer(), getPropertyName(), this);
				longPropertyParser.getPropertySource(schemaNodes, new SourceFile(""), getSchemaNode());

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
				longArrayPropertyParser = new LongArrayPropertyParser(new ErrorBuffer(), getPropertyName(), this);
				longArrayPropertyParser.getPropertySource(schemaNodes, new SourceFile(""), getSchemaNode());

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
				doublePropertyParser = new DoublePropertyParser(new ErrorBuffer(), getPropertyName(), this);
				doublePropertyParser.getPropertySource(schemaNodes, new SourceFile(""), getSchemaNode());

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
				doubleArrayPropertyParser = new DoubleArrayPropertyParser(new ErrorBuffer(), getPropertyName(), this);
				doubleArrayPropertyParser.getPropertySource(schemaNodes, new SourceFile(""), getSchemaNode());

			} catch (FrameworkException fex) {
				// ignore this error because we only need the property parser to extract
				// some information, the generated code is not used at all
			}
		}

		return doubleArrayPropertyParser;
	}

	// ----- private methods -----
	private int addContentHash(final PropertyKey key, final int contentHash) {

		final Object value = wrappedObject.getProperty(key);
		if (value != null) {

			return contentHash ^ value.hashCode();
		}

		return contentHash;
	}

	@Override
	public NodeInterface getSchemaNode() {
		return wrappedObject.getProperty(traits.key("schemaNode"));
	}
}
