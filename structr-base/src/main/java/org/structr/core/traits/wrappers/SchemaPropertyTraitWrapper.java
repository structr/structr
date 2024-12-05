package org.structr.core.traits.wrappers;

import org.structr.core.entity.SchemaProperty;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.schema.SchemaHelper;

public class SchemaPropertyTraitWrapper extends AbstractTraitWrapper<NodeInterface> implements SchemaProperty {

	public SchemaPropertyTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public String getPropertyName() {
		return getProperty(name);
	}

	@Override
	public SchemaHelper.Type getPropertyType() {

		final String _type = getProperty(propertyType);
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
}
