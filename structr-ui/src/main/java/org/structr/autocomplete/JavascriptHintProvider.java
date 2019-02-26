/**
 * Copyright (C) 2010-2019 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.autocomplete;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.structr.common.CaseHelper;
import org.structr.core.GraphObject;
import org.structr.core.function.Functions;
import org.structr.schema.action.Function;
import org.structr.schema.action.Hint;

/**
 *
 *
 */
public class JavascriptHintProvider extends AbstractHintProvider {

	@Override
	protected List<Hint> getAllHints(final GraphObject currentNode, final String currentToken, final String previousToken, final String thirdToken) {

		final boolean isStructrHandle = "Structr".equals(previousToken);
		final List<Hint> hints        = new LinkedList<>();

		if (isStructrHandle) {
			
			// add functions
			for (final Function<Object, Object> func : Functions.getFunctions()) {
				hints.add(func);
			}

			// sort hints
			Collections.sort(hints, comparator);

			// add keywords
			if ("(".equals(currentToken) && "Structr".equals(previousToken)) {
				
				hints.add(0, createHint("this",     "", "The current object",         "this"));
				hints.add(0, createHint("response", "", "The current response",       "response"));
				hints.add(0, createHint("request",  "", "The current request",        "request"));
				hints.add(0, createHint("page",     "", "The current page",           "page"));
				hints.add(0, createHint("me",       "", "The current user",           "me"));
				hints.add(0, createHint("locale",   "", "The current locale",         "locale"));
				hints.add(0, createHint("current",  "", "The current details object", "current"));
				
			}

		} else {

			hints.add(createHint("Structr", "", "Structr context handle", "Structr"));
		}

		return hints;
	}

	@Override
	protected String getFunctionName(final String source) {

		if (source.contains("_")) {
			return CaseHelper.toLowerCamelCase(source);
		}

		return source;
	}

	@Override
	protected String visitReplacement(final String replacement) {
		return replacement;
	}
}
