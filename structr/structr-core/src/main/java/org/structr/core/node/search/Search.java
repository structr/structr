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

import java.util.LinkedList;
import java.util.List;
import org.structr.core.Services;
import org.structr.core.entity.PlainText;
import org.structr.core.entity.AbstractNode;

/**
 *
 * @author axel
 */
public abstract class Search {

    public static SearchAttribute orType(final String searchString) {
        SearchAttribute attr = new TextualSearchAttribute(AbstractNode.TYPE_KEY, searchString, SearchOperator.OR);
        return attr;
    }

    public static SearchAttribute andType(final String searchString) {
        SearchAttribute attr = new TextualSearchAttribute(AbstractNode.TYPE_KEY, searchString, SearchOperator.AND);
        return attr;
    }

    public static SearchAttribute orName(final String searchString) {
        SearchAttribute attr = new TextualSearchAttribute(AbstractNode.NAME_KEY, searchString, SearchOperator.OR);
        return attr;
    }

    public static SearchAttribute andName(final String searchString) {
        SearchAttribute attr = new TextualSearchAttribute(AbstractNode.NAME_KEY, searchString, SearchOperator.AND);
        return attr;
    }

    public static SearchAttribute andTitle(final String searchString) {
        SearchAttribute attr = new TextualSearchAttribute(AbstractNode.TITLE_KEY, searchString, SearchOperator.AND);
        return attr;
    }

    public static SearchAttribute orTitle(final String searchString) {
        SearchAttribute attr = new TextualSearchAttribute(AbstractNode.TITLE_KEY, searchString, SearchOperator.OR);
        return attr;
    }

    public static SearchAttribute andContent(final String searchString) {
        SearchAttribute attr = new TextualSearchAttribute(PlainText.CONTENT_KEY, searchString, SearchOperator.AND);
        return attr;
    }

    public static SearchAttribute orContent(final String searchString) {
        SearchAttribute attr = new TextualSearchAttribute(PlainText.CONTENT_KEY, searchString, SearchOperator.OR);
        return attr;
    }

    public static SearchAttribute orExactType(final String searchString) {
        SearchAttribute attr = new TextualSearchAttribute(AbstractNode.TYPE_KEY, exactMatch(searchString), SearchOperator.OR);
        return attr;
    }

    public static SearchAttribute andExactType(final String searchString) {
        SearchAttribute attr = new TextualSearchAttribute(AbstractNode.TYPE_KEY, exactMatch(searchString), SearchOperator.AND);
        return attr;
    }

    public static SearchAttribute orExactName(final String searchString) {
        SearchAttribute attr = new TextualSearchAttribute(AbstractNode.NAME_KEY, exactMatch(searchString), SearchOperator.OR);
        return attr;
    }

    public static SearchAttribute andExactName(final String searchString) {
        SearchAttribute attr = new TextualSearchAttribute(AbstractNode.NAME_KEY, exactMatch(searchString), SearchOperator.AND);
        return attr;
    }

    public static SearchAttribute orExactTitle(final String searchString) {
        SearchAttribute attr = new TextualSearchAttribute(AbstractNode.TITLE_KEY, exactMatch(searchString), SearchOperator.OR);
        return attr;
    }

    public static SearchAttribute andExactTitle(final String searchString) {
        SearchAttribute attr = new TextualSearchAttribute(AbstractNode.TITLE_KEY, exactMatch(searchString), SearchOperator.AND);
        return attr;
    }

    public static SearchAttribute orExactContent(final String searchString) {
        SearchAttribute attr = new TextualSearchAttribute(PlainText.CONTENT_KEY, exactMatch(searchString), SearchOperator.OR);
        return attr;
    }

    public static SearchAttribute andExactContent(final String searchString) {
        SearchAttribute attr = new TextualSearchAttribute(PlainText.CONTENT_KEY, exactMatch(searchString), SearchOperator.AND);
        return attr;
    }

    public static SearchAttribute andNotHidden() {
        SearchAttribute attr = new BooleanSearchAttribute(AbstractNode.HIDDEN_KEY, true, SearchOperator.NOT);
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

//    public static String normalize(final String input) {
//        String output = Normalizer.normalize(input, Form.NFD);
//        output = StringUtils.trim(output);
////        output = StringUtils.lowerCase(output);
//        output = StringUtils.replace(output, "\"", "");
//        output = StringUtils.replace(output, "´", "");
//        output = StringUtils.replace(output, "`", "");
//        output = StringUtils.replace(output, "^", "");
//        output = StringUtils.replace(output, "'", "");
//        output = StringUtils.replace(output, ".", "");
//        output = StringUtils.replace(output, ",", "");
//        output = StringUtils.replace(output, "-", "");
//        output = StringUtils.replace(output, "+", "");
//        output = StringUtils.replace(output, "(", "");
//        output = StringUtils.replace(output, ")", "");
//        output = StringUtils.replace(output, "=", "");
//        output = StringUtils.replace(output, ":", "");
//        output = StringUtils.replace(output, "~", "");
//        return output;
//    }


    /**
     * Return a list with all nodes matching the given string
     *
     * Internally, the wildcard character '*' will be appended to the string.
     *
     * @param string
     * @return
     */
    public static List<String> getNodeNamesLike(final String string) {
        List<String> names = new LinkedList<String>();

        List<SearchAttribute> searchAttrs = new LinkedList<SearchAttribute>();

        // always add wildcard character '*' for auto completion
        searchAttrs.add(Search.andExactName(string + SearchAttribute.WILDCARD));
        List<AbstractNode> result = (List<AbstractNode>) Services.command(SearchNodeCommand.class).execute(null, null, false, false, searchAttrs);

        if (result != null) {
            for (AbstractNode n : result) {
                names.add(n.getName());
            }
        }

        return names;
    }
}
