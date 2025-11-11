/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.autocomplete;

import org.apache.commons.lang3.StringUtils;
import org.structr.docs.DocumentableType;
import org.structr.docs.Example;
import org.structr.docs.Language;

import java.util.LinkedList;
import java.util.List;

public abstract class BuiltinFunctionHint extends AbstractHint {

	@Override
	public String getDisplayName() {

		// show method with signature right away
		return getReplacement();
	}

	@Override
	public String getDocumentation() {

		// this is the place where we assemble the hint text
		final List<String> buf = new LinkedList<>();

		if (isJavascriptOnly()) { buf.add("**Javascript only**"); }
		if (isStructrScriptOnly()) { buf.add("**StructrScript only**"); }

		// newline
		buf.add("");

		buf.add(getShortDescription());

		if (hasExamples()) {

			final List<Example> examples = getExamples();

			if (examples.size() == 1) {

				buf.add("**Example:**");

			} else {

				buf.add("**Examples:**");
			}

			for (final Example example : examples) {

				// newline
				buf.add("");
				buf.add("```");
				buf.add(example.getText());
				buf.add("```");

				// example description
				if (example.getDescription() != null) {

					buf.add("");
					buf.add(example.getDescription());
				}
			}
		}

		return StringUtils.join(buf, "\n");
	}

	@Override
	public String getReplacement() {

		final StringBuilder buf = new StringBuilder();

		buf.append(getName());
		buf.append("(");

		final String signature = getFirstSignature();
		if (signature != null) {

			buf.append(signature);
		}

		buf.append(")");

		return buf.toString();
	}

	@Override
	public DocumentableType getType() {
		return DocumentableType.BuiltInFunction;
	}

	@Override
	public List<String> getNotes() {
		return null;
	}

	// ----- private methods -----
	private boolean isJavascriptOnly() {
		return getLanguages().containsAll(List.of(Language.Javascript));
	}

	private boolean isStructrScriptOnly() {
		return getLanguages().containsAll(List.of(Language.StructrScript));
	}

	private boolean hasExamples() {
		return getExamples() != null;
	}
}
