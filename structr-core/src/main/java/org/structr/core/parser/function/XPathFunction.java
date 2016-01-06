package org.structr.core.parser.function;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
import org.w3c.dom.Document;

/**
 *
 */
public class XPathFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_XPATH = "Usage: ${xpath(xmlDocument, expression)}. Example: ${xpath(xml(this.xmlSource), \"/test/testValue\")}";

	@Override
	public String getName() {
		return "xpath()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasMinLengthAndAllElementsNotNull(sources, 2) && sources[0] instanceof Document) {

			try {

				XPath xpath = XPathFactory.newInstance().newXPath();
				return xpath.evaluate(sources[1].toString(), sources[0], XPathConstants.STRING);

			} catch (XPathExpressionException ioex) {
				ioex.printStackTrace();
			}
		}

		return "";
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_XPATH;
	}

	@Override
	public String shortDescription() {
		return "Returns the value of the given XPath expression from the given XML DOM";
	}

}
