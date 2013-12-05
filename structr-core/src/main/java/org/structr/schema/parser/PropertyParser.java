package org.structr.schema.parser;

import java.util.Set;
import java.util.LinkedHashSet;
import org.apache.commons.lang.StringUtils;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.InvalidPropertySchemaToken;
import org.structr.core.entity.SchemaNode;
import org.structr.schema.SchemaHelper.Type;

/**
 *
 * @author Christian Morgner
 */
public abstract class PropertyParser {

	protected Set<String> globalValidators = new LinkedHashSet<>();
	protected Set<String> enumDefinitions  = new LinkedHashSet<>();
	protected ErrorBuffer errorBuffer      = null;
	protected String propertyName          = "";;
	protected String localValidator        = "";
	protected String className             = "";
	protected String rawSource             = "";
	
	public abstract Type getKey();
	public abstract String getPropertyType();
	public abstract String getValueType();
	public abstract String getAuxiliaryType();
	public abstract void extractTypeValidation(final String expression) throws FrameworkException;

	public PropertyParser(final ErrorBuffer errorBuffer, final String className, final String propertyName, final String rawSource) {
		
		this.errorBuffer  = errorBuffer;
		this.className    = className;
		this.propertyName = propertyName;
		this.rawSource    = rawSource;
		
		if (this.propertyName.startsWith("_")) {
			this.propertyName = this.propertyName.substring(1);
		}
	}
	
	public String getPropertySource(final ErrorBuffer errorBuffer) throws FrameworkException {
		
		final String keyName   = getKey().name();
		String parserSource    = rawSource.substring(keyName.length());

		// second: uniqueness and/or non-null, check until the two methods to not change the length of the string any more
		parserSource = extractUniqueness(parserSource);

		extractComplexValidation(parserSource);
		
		return getPropertySource();
	}
	
	public Set<String> getGlobalValidators() {
		return globalValidators;
	}
	
	public Set<String> getEnumDefinitions() {
		return enumDefinitions;
	}

	// ----- protected methods -----
	protected String getPropertySource() {
		
		final StringBuilder buf = new StringBuilder();
		
		buf.append("\tpublic static final Property<").append(getValueType()).append("> ").append(propertyName).append("Property");
		buf.append(" = new ").append(getPropertyType()).append("(\"").append(propertyName).append("\"");
		buf.append(getAuxiliaryType());
		buf.append(localValidator);
		buf.append(").indexed();\n");
		
		return buf.toString();
	}

	private String extractUniqueness(final String source) {

		if (source.startsWith("!")) {

			localValidator = ", new GlobalPropertyUniquenessValidator()";

			return source.substring(1);
		}

		return source;
	}

	public void setNotNull(final boolean notNull) {

		if (notNull) {

			StringBuilder buf = new StringBuilder();
			buf.append("ValidationHelper.checkPropertyNotNull(this, ");
			buf.append(className).append(".").append(propertyName).append("Property");
			buf.append(", errorBuffer)");

			globalValidators.add(buf.toString());
		}
	}

	private void extractComplexValidation(final String source) throws FrameworkException {

		if (StringUtils.isNotBlank(source)) {

			if (source.startsWith("(") && source.endsWith(")")) {

				extractTypeValidation(source.substring(1, source.length() - 1));
				
			} else {
	
				errorBuffer.add(SchemaNode.class.getSimpleName(), new InvalidPropertySchemaToken(source, "invalid_validation_expression", "Valdation expression must be enclosed in (), e.g. (" + source + ")"));
			}
		}
	}
}
