/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.common;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.PlainText;
import org.structr.core.entity.AbstractNode;
import org.structr.core.search.SearchAttribute;
import org.structr.core.search.SingleSearchAttribute;
import org.structr.core.search.SearchNodeCommand;

/**
 *
 * @author axel
 */
public abstract class Search {

    public static SearchAttribute orType(final String searchString) {
        SearchAttribute attr = new SingleSearchAttribute(AbstractNode.TYPE_KEY, searchString, SearchOperator.OR);
        return attr;
    }

    public static SearchAttribute andType(final String searchString) {
        SearchAttribute attr = new SingleSearchAttribute(AbstractNode.TYPE_KEY, searchString, SearchOperator.AND);
        return attr;
    }

    public static SearchAttribute orName(final String searchString) {
        SearchAttribute attr = new SingleSearchAttribute(AbstractNode.NAME_KEY, searchString, SearchOperator.OR);
        return attr;
    }

    public static SearchAttribute andName(final String searchString) {
        SearchAttribute attr = new SingleSearchAttribute(AbstractNode.NAME_KEY, searchString, SearchOperator.AND);
        return attr;
    }

    public static SearchAttribute andTitle(final String searchString) {
        SearchAttribute attr = new SingleSearchAttribute(AbstractNode.TITLE_KEY, searchString, SearchOperator.AND);
        return attr;
    }

    public static SearchAttribute orTitle(final String searchString) {
        SearchAttribute attr = new SingleSearchAttribute(AbstractNode.TITLE_KEY, searchString, SearchOperator.OR);
        return attr;
    }

    public static SearchAttribute andContent(final String searchString) {
        SearchAttribute attr = new SingleSearchAttribute(PlainText.CONTENT_KEY, searchString, SearchOperator.AND);
        return attr;
    }

    public static SearchAttribute orContent(final String searchString) {
        SearchAttribute attr = new SingleSearchAttribute(PlainText.CONTENT_KEY, searchString, SearchOperator.OR);
        return attr;
    }

    public static SearchAttribute orExactType(final String searchString) {
        SearchAttribute attr = new SingleSearchAttribute(AbstractNode.TYPE_KEY, exactMatch(searchString), SearchOperator.OR);
        return attr;
    }

    public static SearchAttribute andExactType(final String searchString) {
        SearchAttribute attr = new SingleSearchAttribute(AbstractNode.TYPE_KEY, exactMatch(searchString), SearchOperator.AND);
        return attr;
    }

    public static SearchAttribute orExactName(final String searchString) {
        SearchAttribute attr = new SingleSearchAttribute(AbstractNode.NAME_KEY, exactMatch(searchString), SearchOperator.OR);
        return attr;
    }

    public static SearchAttribute andExactName(final String searchString) {
        SearchAttribute attr = new SingleSearchAttribute(AbstractNode.NAME_KEY, exactMatch(searchString), SearchOperator.AND);
        return attr;
    }

    public static SearchAttribute orExactTitle(final String searchString) {
        SearchAttribute attr = new SingleSearchAttribute(AbstractNode.TITLE_KEY, exactMatch(searchString), SearchOperator.OR);
        return attr;
    }

    public static SearchAttribute andExactTitle(final String searchString) {
        SearchAttribute attr = new SingleSearchAttribute(AbstractNode.TITLE_KEY, exactMatch(searchString), SearchOperator.AND);
        return attr;
    }

    public static SearchAttribute orExactContent(final String searchString) {
        SearchAttribute attr = new SingleSearchAttribute(PlainText.CONTENT_KEY, exactMatch(searchString), SearchOperator.OR);
        return attr;
    }

    public static SearchAttribute andExactContent(final String searchString) {
        SearchAttribute attr = new SingleSearchAttribute(PlainText.CONTENT_KEY, exactMatch(searchString), SearchOperator.AND);
        return attr;
    }

    public static SearchAttribute andNotHidden() {
        SearchAttribute attr = new SingleSearchAttribute(AbstractNode.HIDDEN_KEY, true, SearchOperator.NOT);
        return attr;
    }

    private static String exactMatch(final String searchString) {
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

    private static String normalize(final String input) {
        String output = Normalizer.normalize(input, Form.NFD);
        output = StringUtils.trim(output);
        output = StringUtils.lowerCase(output);
        output = StringUtils.replace(output, "´", "");
        output = StringUtils.replace(output, "`", "");
        output = StringUtils.replace(output, "^", "");
        output = StringUtils.replace(output, "'", "");
        output = StringUtils.replace(output, ".", "");
        output = StringUtils.replace(output, ",", "");
        output = StringUtils.replace(output, " ", "");
        output = StringUtils.replace(output, "-", "");
        output = StringUtils.replace(output, "+", "");
        output = StringUtils.replace(output, "(", "");
        output = StringUtils.replace(output, ")", "");
        return output;
    }


    /**
     * Return a list with all nodes matching the given string
     *
     * Internally, the wildcard character '*' will be appended to the string.
     *
     * @param string
     * @return
     */
    public static List<String> getNodeNamesLike(final String string) {
        List<String> names = new ArrayList<String>();

        Command search = Services.command(SearchNodeCommand.class);
        List<SearchAttribute> searchAttrs = new ArrayList<SearchAttribute>();

        // always add wildcard character '*' for auto completion
        searchAttrs.add(Search.andExactName(string + SearchAttribute.WILDCARD));
        List<AbstractNode> result = (List<AbstractNode>) search.execute(null, null, true, false, searchAttrs);

        if (result != null) {
            for (AbstractNode n : result) {
                names.add(n.getName());
            }
        }

        return names;
    }
}
