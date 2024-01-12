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
package org.structr.common;

import org.apache.commons.lang3.text.WordUtils;

//~--- classes ----------------------------------------------------------------

/**
 * A helper class that contains methods to convert strings to and from
 * different cases and styles, i.e. camelCase to underscore_style etc.
 *
 *
 */
public class CaseHelper {

	public static String toUpperCamelCase(final String input) {
		return WordUtils.capitalize(input, new char[] { '_' }).replaceAll("_", "");
	}

	public static String toLowerCamelCase(final String input) {
		return input.substring(0, 1).toLowerCase().concat(WordUtils.capitalize(input, new char[] { '_' }).replaceAll("_", "").substring(1));
	}

	public static String toUnderscore(final String input, final boolean plural) {

		if (input.toUpperCase().equals(input)) {

			return input;

		}

		StringBuilder out = new StringBuilder();

		for (int i = 0; i < input.length(); i++) {

			char c = input.charAt(i);

			if (Character.isUpperCase(c)) {
				char nextCharacter = 0;

				try {

					nextCharacter = input.charAt(i + 1);

				} catch (IndexOutOfBoundsException ex) {
				}

				Boolean nextCharacterIsUpperCase = Character.isUpperCase(nextCharacter);

				if (i > 0 && !nextCharacterIsUpperCase) {

					out.append("_");
					out.append(Character.toLowerCase(c));

				} else if (nextCharacterIsUpperCase) {

					out.append(c);

				} else {

					out.append(Character.toLowerCase(c));
				}

			} else {

				out.append(c);

			}

		}

		String output = out.toString();

		return plural
		       ? plural(output)
		       : output;
	}

	public static String plural(String type) {

		int len = type.length();

		if (type.substring(len - 1, len).equals("y")) {

			return type.substring(0, len - 1) + "ies";

		} else if (!(type.substring(len - 1, len).equals("s"))) {

			return type.concat("s");

		} else {
			return type;
		}
	}
}
