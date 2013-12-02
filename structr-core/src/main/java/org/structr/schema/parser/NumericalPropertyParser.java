package org.structr.schema.parser;

import org.apache.commons.lang.StringUtils;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.InvalidPropertySchemaToken;
import org.structr.core.entity.Schema;

/**
 *
 * @author Christian Morgner
 */
public abstract class NumericalPropertyParser extends PropertyParser {
	
	public NumericalPropertyParser(final ErrorBuffer errorBuffer, final String className, final String propertyName, final String rawSource) {
		super(errorBuffer, className, propertyName, rawSource);
	}

	public abstract Number parseNumber(final ErrorBuffer errorBuffer, final String source, final String which);
	
	@Override
	public String getEnumType() {
		return "";
	}

	@Override
	public void extractTypeValidation(final String expression) throws FrameworkException {

		if (StringUtils.isNotBlank(expression)) {

			if (expression.contains(",")) {

				if ((expression.startsWith("[") || expression.startsWith("]")) && (expression.endsWith("[") || expression.endsWith("]"))) {

					final String range      = expression.substring(1, expression.length()-1);
					final String[] parts    = range.split("[, ]+");
					boolean error           = false;

					if (parts.length == 2) {
						
						Number lowerBound = parseNumber(errorBuffer, parts[0], "lower");
						Number upperBound = parseNumber(errorBuffer, parts[1], "upper");
						
						if (lowerBound == null) {
							error = true;
						}
						
						if (upperBound == null) {
							error = true;
						}
						
					} else {

						errorBuffer.add(Schema.class.getSimpleName(), new InvalidPropertySchemaToken(expression, "invalid_range_expression", "Range must have exactly two bounds."));
						error = true;
					}

					if (error) {
						return;
					}

					final StringBuilder buf = new StringBuilder();

					buf.append("ValidationHelper.check").append(getValueType()).append("InRangeError(this, ");
					buf.append(className).append(".").append(propertyName).append("Property");
					buf.append(", \"").append(expression);
					buf.append("\", errorBuffer)");

					globalValidators.add(buf.toString());

				} else {

					errorBuffer.add(Schema.class.getSimpleName(), new InvalidPropertySchemaToken(expression, "invalid_range_expression", "Range expression must start and end with [ or ], e.g. [" + expression + "]."));
				}

			} else {

				errorBuffer.add(Schema.class.getSimpleName(), new InvalidPropertySchemaToken(expression, "invalid_range_expression", "Range expression must contain two values separated by a comma."));
			}
		}
	}
}
