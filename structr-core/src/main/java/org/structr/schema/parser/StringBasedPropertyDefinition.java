package org.structr.schema.parser;

import org.apache.commons.lang3.StringUtils;
import org.structr.schema.SchemaHelper.Type;

/**
 *
 * @author Christian Morgner
 */
public class StringBasedPropertyDefinition implements PropertyDefinition {

	private String propertyName = null;
	private Type propertyType   = null;
	private String rawSource    = null;
	private String source       = null;
	private String dbName       = null;
	private String format       = null;
	private String defaultValue = null;
	private String contentType  = null;
	private boolean notNull     = false;
	private boolean unique      = false;

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

		if (source.endsWith("!")) {
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
	public String getFormat() {
		return format;
	}

	@Override
	public boolean isNotNull() {
		return notNull;
	}

	@Override
	public boolean isUnique() {
		return unique;
	}

	@Override
	public String getDefaultValue() {
		return defaultValue;
	}

	@Override
	public String getContentType() {
		return contentType;
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
