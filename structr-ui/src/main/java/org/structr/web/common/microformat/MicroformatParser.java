package org.structr.web.common.microformat;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Tag;
import org.jsoup.select.NodeVisitor;

/**
 *
 * @author Christian Morgner
 */
public class MicroformatParser {

	public List<Map<String, Object>> parse(final String source, final String selector) {

		final List<Map<String, Object>> objects = new LinkedList<>();

		for (final Element element : Jsoup.parse(source).select(selector)) {

			// remove semantically empty markup elements
			unwrap(element);

			final Map<String, Object> values = new LinkedHashMap<>();
			recurse(element, values, 0);

			objects.add(values);

		}

		return objects;
	}

	private void recurse(final Element element, final Map<String, Object> values, final int depth) {

		final Tag tag                    = element.tag();
		final Set<String> classes        = element.classNames();
		final String link                = element.attr("href");
		final Object content             = extractChildContent(element);

		if (!classes.isEmpty()) {

			removeEmpty(classes);

			// toplevel classes define type
			if (tag.isBlock()) {

				if (depth == 0) {

					// store type attribute
					values.put("type", classes);

					for (final Element child : element.children()) {
						recurse(child, values, depth+1);
					}

				} else {

					final Map<String, Object> childMap = new LinkedHashMap<>();
					values.put(classes.iterator().next(), childMap);

					if (content != null) {
						childMap.put("name", content);
					}

					for (final Element child : element.children()) {
						recurse(child, childMap, depth+1);
					}
				}

			} else if (tag.isInline()) {

				// extract href and store as URL
				if (classes.contains("url") && StringUtils.isNotBlank(link)) {

					values.put("url", link);
					classes.remove("url");
				}

				if (content != null) {

					for (final String type : classes) {
						values.put(type, content);
					}
				}

			}
		}
	}

	private void removeEmpty(final Set<String> source) {

		for (Iterator<String> it = source.iterator(); it.hasNext();) {

			if (StringUtils.isBlank(it.next())) {
				it.remove();
			}
		}
	}

	private void unwrap(final Element element) {

		final Set<Element> elementsToUnwrap = new LinkedHashSet<>();

		element.traverse(new NodeVisitor() {

			@Override
			public void head(Node node, int depth) {

				if (node instanceof Element) {

					final Element element = (Element)node;

					if (element.isBlock()) {
						final Set<String> classes = element.classNames();

						removeEmpty(classes);

						if (classes.isEmpty()) {
							elementsToUnwrap.add(element);
						}
					}
				}
			}

			@Override
			public void tail(Node node, int depth) {
			}
		});

		for (final Element unwrap : elementsToUnwrap) {
			unwrap.unwrap();
		}
	}

	private Object extractChildContent(final Element element) {

		final List<String> parts = new LinkedList<>();

		element.traverse(new NodeVisitor() {

			@Override
			public void head(Node node, int depth) {

				if (node instanceof Element) {

					final Element element     = (Element)node;
					final Set<String> classes = element.classNames();

					removeEmpty(classes);

					if (classes.isEmpty()) {

						parts.add(element.ownText());
					}
				}
			}

			@Override
			public void tail(Node node, int depth) {
			}
		});

		if (parts.isEmpty()) {

			final String ownText = element.ownText();
			if (StringUtils.isNotBlank(ownText)) {

				parts.add(element.ownText());
			}
		}

		if (parts.isEmpty()) {
			return null;
		}

		if (parts.size() == 1) {
			return parts.get(0);
		}

		return parts;
	}
}
