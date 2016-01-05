package org.structr.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class StripHtmlFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_STRIP_HTML    = "Usage: ${strip_html(html)}. Example: ${strip_html(\"<p>foo</p>\")}";
	public static final String ERROR_MESSAGE_STRIP_HTML_JS = "Usage: ${{Structr.strip_html(html)}}. Example: ${{Structr.strip_html(\"<p>foo</p>\")}}";

	@Override
	public String getName() {
		return "strip_html()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		return (arrayHasMinLengthAndAllElementsNotNull(sources, 1))
			? sources[0].toString().replaceAll("\\<.*?>", "")
			: "";

	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_STRIP_HTML_JS : ERROR_MESSAGE_STRIP_HTML);
	}

	@Override
	public String shortDescription() {
		return "";
	}


}
