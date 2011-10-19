/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.core.node.search;

import org.apache.commons.lang.StringUtils;

import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.PlainText;

//~--- JDK imports ------------------------------------------------------------

import java.text.Normalizer;

import java.util.LinkedList;
import java.util.List;
import org.structr.common.SecurityContext;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author axel
 */
public abstract class Search {

	public static SearchAttribute orType(final String searchString) {

		SearchAttribute attr = new TextualSearchAttribute(AbstractNode.Key.type.name(),
			searchString,
			SearchOperator.OR);

		return attr;
	}

	public static SearchAttribute andType(final String searchString) {

		SearchAttribute attr = new TextualSearchAttribute(AbstractNode.Key.type.name(),
			searchString,
			SearchOperator.AND);

		return attr;
	}

	public static SearchAttribute orName(final String searchString) {

		SearchAttribute attr = new TextualSearchAttribute(AbstractNode.Key.name.name(),
			searchString,
			SearchOperator.OR);

		return attr;
	}

	public static SearchAttribute andName(final String searchString) {

		SearchAttribute attr = new TextualSearchAttribute(AbstractNode.Key.name.name(),
			searchString,
			SearchOperator.AND);

		return attr;
	}

	public static SearchAttribute andTitle(final String searchString) {

		SearchAttribute attr = new TextualSearchAttribute(AbstractNode.Key.title.name(),
			searchString,
			SearchOperator.AND);

		return attr;
	}

	public static SearchAttribute orTitle(final String searchString) {

		SearchAttribute attr = new TextualSearchAttribute(AbstractNode.Key.title.name(),
			searchString,
			SearchOperator.OR);

		return attr;
	}

	public static SearchAttribute andContent(final String searchString) {

		SearchAttribute attr = new TextualSearchAttribute(PlainText.Key.content.name(),
			searchString,
			SearchOperator.AND);

		return attr;
	}

	public static SearchAttribute orContent(final String searchString) {

		SearchAttribute attr = new TextualSearchAttribute(PlainText.Key.content.name(),
			searchString,
			SearchOperator.OR);

		return attr;
	}

	public static SearchAttribute orExactType(final String searchString) {

		SearchAttribute attr = new TextualSearchAttribute(AbstractNode.Key.type.name(),
			exactMatch(searchString),
			SearchOperator.OR);

		return attr;
	}

	public static SearchAttribute andExactType(final String searchString) {

		SearchAttribute attr = new TextualSearchAttribute(AbstractNode.Key.type.name(),
			exactMatch(searchString),
			SearchOperator.AND);

		return attr;
	}

	public static SearchAttribute orExactName(final String searchString) {

		SearchAttribute attr = new TextualSearchAttribute(AbstractNode.Key.name.name(),
			exactMatch(searchString),
			SearchOperator.OR);

		return attr;
	}

	public static SearchAttribute andExactName(final String searchString) {

		SearchAttribute attr = new TextualSearchAttribute(AbstractNode.Key.name.name(),
			exactMatch(searchString),
			SearchOperator.AND);

		return attr;
	}

	public static SearchAttribute orExactTitle(final String searchString) {

		SearchAttribute attr = new TextualSearchAttribute(AbstractNode.Key.title.name(),
			exactMatch(searchString),
			SearchOperator.OR);

		return attr;
	}

	public static SearchAttribute andExactTitle(final String searchString) {

		SearchAttribute attr = new TextualSearchAttribute(AbstractNode.Key.title.name(),
			exactMatch(searchString),
			SearchOperator.AND);

		return attr;
	}

	public static SearchAttribute orExactContent(final String searchString) {

		SearchAttribute attr = new TextualSearchAttribute(PlainText.Key.content.name(),
			exactMatch(searchString),
			SearchOperator.OR);

		return attr;
	}

	public static SearchAttribute andExactContent(final String searchString) {

		SearchAttribute attr = new TextualSearchAttribute(PlainText.Key.content.name(),
			exactMatch(searchString),
			SearchOperator.AND);

		return attr;
	}

	public static SearchAttribute andNotHidden() {

		SearchAttribute attr = new BooleanSearchAttribute(AbstractNode.Key.hidden.name(),
			true,
			SearchOperator.NOT);

		return attr;
	}

	public static String exactMatch(final String searchString) {
		return ("\"" + searchString + "\"");
	}

	public static String unquoteExactMatch(final String searchString) {

		String result = searchString;

		if (searchString.startsWith("\"")) {
			result = result.substring(1);
		}

		if (searchString.endsWith("\"")) {
			result = result.substring(0, result.length() - 1);
		}

		return result;
	}

	/**
	 * Normalize special characters to ASCII
	 *
	 * @param input
	 * @return
	 */
	public static String normalize(final String input) {

		String normalized = Normalizer.normalize(input,
			Normalizer.Form.NFD);

		return normalized.replaceAll("[^\\p{ASCII}]",
					     "");
	}

	/**
	 * Remove dangerous characters from a search string
	 *
	 * @param input
	 * @return
	 */
	public static String clean(final String input) {

//              String output = Normalizer.clean(input, Form.NFD);
		String output = StringUtils.trim(input);

//              String output = input;
		// Remove all kinds of quotation marks
		output = StringUtils.replace(output, "´", "");
		output = StringUtils.replace(output, "`", "");
		output = StringUtils.replace(output, "'", "");

		// output = StringUtils.replace(output, ".", "");
		output = StringUtils.replace(output, ",", "");
		output = StringUtils.replace(output, " - ", "");
		output = StringUtils.replace(output, "- ", "");
		output = StringUtils.replace(output, " -", "");
		output = StringUtils.replace(output, "=", "");
		output = StringUtils.replace(output, "<", "");
		output = StringUtils.replace(output, ">", "");

		// Remove Lucene special characters
		//
		// + - && || ! ( ) { } [ ] ^ " ~ * ? : \
		output = StringUtils.replace(output, "+", "");

		// output = StringUtils.replace(output, "-", "");
		output = StringUtils.replace(output, "&&", "");
		output = StringUtils.replace(output, "||", "");
		output = StringUtils.replace(output, "!", "");
		output = StringUtils.replace(output, "(", "");
		output = StringUtils.replace(output, ")", "");
		output = StringUtils.replace(output, "{", "");
		output = StringUtils.replace(output, "}", "");
		output = StringUtils.replace(output, "[", "");
		output = StringUtils.replace(output, "]", "");
		output = StringUtils.replace(output, "^", "");
		output = StringUtils.replace(output, "\"", "");
		output = StringUtils.replace(output, "~", "");
		output = StringUtils.replace(output, "*", "");
		output = StringUtils.replace(output, "?", "");
		output = StringUtils.replace(output, ":", "");
		output = StringUtils.replace(output, "\\", "");

		return output;
	}

	//~--- get methods ----------------------------------------------------

	/**
	 * Return a list with all nodes matching the given string
	 *
	 * Internally, the wildcard character '*' will be appended to the string.
	 *
	 * @param string
	 * @return
	 */
	public static List<String> getNodeNamesLike(SecurityContext securityContext, final String string) {

		List<String> names                = new LinkedList<String>();
		List<SearchAttribute> searchAttrs = new LinkedList<SearchAttribute>();

		// always add wildcard character '*' for auto completion
		searchAttrs.add(Search.andName(string + SearchAttribute.WILDCARD));

		List<AbstractNode> result = (List<AbstractNode>) Services.command(securityContext, SearchNodeCommand.class).execute(
			null,
			false,
			false,
			searchAttrs);

		if (result != null) {

			for (AbstractNode n : result) {
				names.add(n.getName());
			}
		}

		return names;
	}
}
