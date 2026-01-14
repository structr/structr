/*
 * Copyright (C) 2010-2026 Structr GmbH
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
import org.structr.core.property.Property;
import org.structr.schema.SchemaHelper;
import org.structr.schema.SchemaHelper.Type;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;

/**
 *
 *
 */
public class JoinPropertyGenerator extends PropertyGenerator {

	private static final Logger logger = LoggerFactory.getLogger(JoinPropertyGenerator.class.getName());

	private String parameters   = "";

	public JoinPropertyGenerator(final ErrorBuffer errorBuffer, final String className, final PropertyDefinition params) {
		super(errorBuffer, className, params);
	}

	@Override
	public SchemaHelper.Type getPropertyType() {
		return Type.Join;
	}

	@Override
	public String getValueType() {
		return String.class.getSimpleName();
	}

	@Override
	protected Object getDefaultValue() {
		return null;
	}

	@Override
	public Property newInstance() throws FrameworkException {

		final String expression         = source.getFormat();
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


		return null;
	}
}
