package org.structr.function;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.parser.function.XmlFunction;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.schema.action.ActionContext;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 */
public class FromXmlFunction extends UiFunction {

	private static final Logger logger                     = Logger.getLogger(FromXmlFunction.class.getName());
	private static final Property<List> attributesProperty = new GenericProperty<>("attributes");
	private static final Property<List> childrenProperty   = new GenericProperty<>("children");
	private static final Property<String> valueProperty    = new StringProperty("value");
	private static final Property<String> nameProperty     = new StringProperty("name");
	private static final Property<String> typeProperty     = new StringProperty("type");

	public static final String ERROR_MESSAGE_FROM_XML    = "Usage: ${from_xml(source)}. Example: ${from_xml('<entry>0</entry>')}";
	public static final String ERROR_MESSAGE_FROM_XML_JS = "Usage: ${{Structr.from_xml(src)}}. Example: ${{Structr.from_xml('<entry>0</entry>')}}";

	@Override
	public Object apply(ActionContext ctx, GraphObject entity, Object[] sources) throws FrameworkException {

		if (sources != null && sources.length > 0) {

			if (sources[0] != null) {

				try {

					final GraphObjectMap result = new GraphObjectMap();
					final XmlFunction xml       = new XmlFunction();
					final Document document     = (Document)xml.apply(ctx, entity, sources);

					if (document != null) {

						convertNode(result, document);
						return result;

					} else {

						logger.log(Level.WARNING, "Unable to parse XML document: {0}", sources[0].toString());
					}

				} catch (Throwable t) {
					t.printStackTrace();
				}
			}

			return "";
		}

		return usage(ctx.isJavaScriptContext());
	}

	@Override
	public String usage(boolean inJavaScriptContext) {

		if (inJavaScriptContext) {
			return ERROR_MESSAGE_FROM_XML_JS;
		}

		return ERROR_MESSAGE_FROM_XML;
	}

	@Override
	public String shortDescription() {
		return "Parses the given XML and returns a list of objects.";
	}

	@Override
	public String getName() {
		return "from_xml";
	}

	// ----- private methods -----
	private void convertNode(final GraphObjectMap map, final Node node) {

		final NodeList nodeList = node.getChildNodes();
		if (nodeList != null) {

			final List<GraphObjectMap> children = new LinkedList<>();
			final int length                    = nodeList.getLength();

			for (int i=0; i<length; i++) {

				final GraphObjectMap childMap = new GraphObjectMap();
				final Node childNode          = nodeList.item(i);

				convertNode(childMap, childNode);
				children.add(childMap);
			}

			map.put(childrenProperty, children);
		}

		final NamedNodeMap attributeList = node.getAttributes();
		if (attributeList != null) {

			final List<GraphObjectMap> attributes = new LinkedList<>();
			final int length                      = attributeList.getLength();

			for (int i=0; i<length; i++) {

				final GraphObjectMap attributeMap = new GraphObjectMap();
				final Node attributeNode          = attributeList.item(i);

				convertNode(attributeMap, attributeNode);
				attributes.add(attributeMap);
			}

			map.put(attributesProperty, attributes);
		}

		map.put(typeProperty, node.getClass().getSimpleName());

		final String nodeName = node.getNodeName();
		if (nodeName != null) {

			map.put(nameProperty, nodeName);
		}

		final String nodeValue = node.getNodeValue();
		if (nodeValue != null) {

			map.put(valueProperty, nodeValue);
		}
	}
}
