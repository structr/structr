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

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.structr.schema.SchemaHelper.Type;

/**
 *
 *
 */
public class StringBasedPropertyDefinition implements PropertyDefinition {

	private String propertyName           = null;
	private Type propertyType             = null;
	private String rawSource              = null;
	private String source                 = null;
	private String dbName                 = null;
	private String format                 = null;
	private String typeHint               = null;
	private String hint                   = null;
	private String category               = null;
	private String defaultValue           = null;
	private String contentType            = null;
	private String fqcn                   = null;
	private boolean notNull               = false;
	private boolean compound              = false;
	private boolean unique                = false;
	private boolean indexed               = true;
	private boolean readOnly              = false;
	private boolean isPartOfBuiltInSchema = false;
	private boolean isCachingEnabled      = false;
	private String readFunction           = null;
	private String writeFunction          = null;
	private String openAPIReturnType      = null;
	private List<String> transformators   = null;
	private List<String> validators       = null;

	public StringBasedPropertyDefinition(final String propertyName, final String rawSource) {

		this.propertyName = propertyName;
 		this.rawSource    = rawSource;
 		this.source       = rawSource;

		if (this.propertyName.startsWith("_")) {
			this.propertyName = this.propertyName.substring(1);
		}

		// detect and remove format: <type>(...)
		if (StringUtils.isNotBlank(source)) {

			format = substringBetween(source, "(", ")");
			source = source.replaceFirst("\\(.*\\)", "");

		}

		// detect optional db name
		if (source.contains("|")) {

			dbName = source.substring(0, source.indexOf("|"));
			source = source.substring(source.indexOf("|")+1);

		}

		// detect and remove not-null constraint
		if (source.startsWith("+")) {
			source = source.substring(1);
			notNull = true;
		}

		// detect and remove content-type: <type>[...]
		if (StringUtils.isNotBlank(source)) {

			contentType = substringBetween(source, "[", "]");
			source = source.replaceFirst("\\[.*\\]", "");

		}

		// detect and remove default value
		if (source.contains(":")) {

			// default value is everything after the first :
			// this is possible because we stripped off the format (...) above
			int firstIndex      = source.indexOf(":");
			defaultValue = source.substring(firstIndex + 1);
			source       = source.substring(0, firstIndex);

		}

		if (source.endsWith("!!")) {

			compound = true;
			source = source.substring(0, source.length() - 2);

		} else if (source.endsWith("!")) {

			unique = true;
			source = source.substring(0, source.length() - 1);
		}

		propertyType = Type.valueOf(source);
	}

	@Override
	public String getPropertyName() {
		return propertyName;
	}

	@Override
	public Type getPropertyType() {
		return propertyType;
	}

	@Override
	public String getRawSource() {
		return rawSource;
	}

	@Override
	public String getSource() {
		return source;
	}

	@Override
	public String getDbName() {
		return dbName;
	}

	@Override
	public String getHint() {
		return hint;
	}

	@Override
	public String getCategory() {
		return category;
	}

	@Override
	public String getFormat() {
		return format;
	}

	@Override
	public String getTypeHint() {
		return typeHint;
	}

	@Override
	public boolean isNotNull() {
		return notNull;
	}

	@Override
	public boolean isCompound() {
		return compound;
	}

	@Override
	public boolean isUnique() {
		return unique;
	}

	@Override
	public boolean isIndexed() {
		return indexed;
	}

	@Override
	public boolean isReadOnly() {
		return readOnly;
	}

	@Override
	public String getDefaultValue() {
		return defaultValue;
	}

	@Override
	public String getContentType() {
		return contentType;
	}

	@Override
	public String getReadFunction() {
		return readFunction;
	}

	@Override
	public String getWriteFunction() {
		return writeFunction;
	}

	@Override
	public String getOpenAPIReturnType() {	return openAPIReturnType; }

	@Override
	public List<String> getTransformators() {
		return transformators;
	}

	@Override
	public List<String> getValidators() {
		return validators;
	}

	@Override
	public String getFqcn() {
		return fqcn;
	}

	@Override
	public boolean isPartOfBuiltInSchema() {
		return isPartOfBuiltInSchema;
	}

	@Override
	public boolean isCachingEnabled() {
		return isCachingEnabled;
	}

	@Override
	public String getUuid() {
		return null;
	}

	public static String substringBetween(final String source, final String prefix, final String suffix) {

		final int pos1 = source.indexOf(prefix);
		final int pos2 = source.lastIndexOf(suffix);

		if (pos1 < pos2 && pos2 > 0) {

			return source.substring(pos1 + 1, pos2);
		}

		return null;
	}
}
