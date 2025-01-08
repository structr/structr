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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.property.JoinProperty;
import org.structr.schema.SchemaHelper;
import org.structr.schema.SchemaHelper.Type;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;

/**
 *
 *
 */
public class JoinPropertyParser extends PropertySourceGenerator {

	private static final Logger logger = LoggerFactory.getLogger(JoinPropertyParser.class.getName());

	private String parameters   = "";

	public JoinPropertyParser(final ErrorBuffer errorBuffer, final String className, final PropertyDefinition params) {
		super(errorBuffer, className, params);
	}

	@Override
	public SchemaHelper.Type getKey() {
		return Type.Join;
	}

	@Override
	public String getPropertyType() {
		return JoinProperty.class.getSimpleName();
	}

	@Override
	public String getValueType() {
		return String.class.getSimpleName();
	}

	@Override
	public String getUnqualifiedValueType() {
		return getValueType();
	}

	@Override
	public String getPropertyParameters() {
		return parameters;
	}

	@Override
	public void parseFormatString(final AbstractSchemaNode entity, String expression) throws FrameworkException {

		final StreamTokenizer tokenizer = new StreamTokenizer(new StringReader(expression));
		final StringBuilder buf         = new StringBuilder();

		tokenizer.wordChars('_', '_');

		String token = null;
		int type = 0;

		try {
			do {

				token = null;
				type = tokenizer.nextToken();

				switch (type) {

					case StreamTokenizer.TT_NUMBER:
						token = String.valueOf(tokenizer.nval);
						break;

					case StreamTokenizer.TT_WORD:
						token = tokenizer.sval;
						break;

					case StreamTokenizer.TT_EOF:
					case StreamTokenizer.TT_EOL:
						break;

					case '\'':
					case '\"':
						token = "\"" + tokenizer.sval + "\"";
						break;
				}

				if (token != null) {

					if (token.startsWith("_")) {

						token = token.substring(1) + "Property";
					}

					buf.append(", ");
					buf.append(token);
				}

			} while (type != StreamTokenizer.TT_EOF);

		} catch (IOException ex) {
			logger.warn("", ex);
		}

		parameters = buf.toString();
	}
}
