/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.web.appmap;

import org.apache.commons.lang3.StringUtils;
import org.structr.appmap.ApplicationMap;
import org.structr.appmap.ApplicationMapContributor;
import org.structr.appmap.ScriptScanner;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.Site;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.event.ActionMapping;
import org.structr.web.entity.event.ParameterMapping;
import org.structr.web.entity.path.PagePath;
import org.structr.web.entity.path.PagePathParameter;
import org.structr.web.traits.definitions.dom.DOMElementTraitDefinition;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The user interface: every page and shared component with the outline of its HTML
 * structure. The scripts in its elements connect it to the types, methods and external
 * services they name, an include() or an embedded shared component to that component, and
 * links and the success and failure URLs of its action mappings to the pages they lead to.
 * Its repeaters and action mappings become nodes of their own, see DataBindings, and the
 * outline names them at the element they live on, so that a client can show their relations
 * inside the page; the outlines are therefore written after DataBindings has run.
 */
public class PagesMapContributor implements ApplicationMapContributor {

	private static final Set<String> SKIPPED_TAGS = Set.of("head", "script", "style", "meta", "link", "title");
	private static final Pattern INCLUDE          = Pattern.compile("\\binclude(?:_child)?\\s*\\(\\s*['\"]([^'\"]+)['\"]");
	private static final Pattern PLACING          = Pattern.compile("^\\$\\{\\s*(?:render|include|include_child)\\s*\\(");
	private static final int OUTLINE_DEPTH        = 9;
	private static final int EXPRESSIONS_LENGTH   = 120;

	@Override
	public void contribute(final ApplicationMap map, final App app) throws FrameworkException {

		final List<HtmlNode> all                  = new LinkedList<>();
		final Set<String> assignedActions         = new HashSet<>();
		final Map<String, String> componentIds    = new LinkedHashMap<>();
		final Map<String, String> pagesByPath     = new LinkedHashMap<>();
		final Map<String, String> pagesByName     = new LinkedHashMap<>();
		final Map<ApplicationMap.MapNode, List<HtmlNode>> outlines = new LinkedHashMap<>();

		for (final NodeInterface node : app.nodeQuery(StructrTraits.PAGE).getAsList()) {

			final Page page = node.as(Page.class);

			if (page.is(StructrTraits.SHADOW_DOCUMENT)) {

				for (final DOMNode element : page.getElements()) {

					if (element.getParent() == null) {

						final HtmlNode root = collect(element, null, element.getUuid(), all, assignedActions);

						outlines.put(map.addNode(element.getUuid(), ApplicationMap.COMPONENT, nameOf(element)).setAccess(element), List.of(root));
						componentIds.put(nameOf(element), element.getUuid());
					}
				}

			} else {

				final List<HtmlNode> roots = new LinkedList<>();

				for (final DOMNode child : page.getChildren()) {
					roots.add(collect(child, null, page.getUuid(), all, assignedActions));
				}

				// a page visible to public users is open to anyone who knows its address
				outlines.put(map.addNode(page.getUuid(), ApplicationMap.PAGE, page.getName()).setPublic(node.isVisibleToPublicUsers()).setAccess(node).setCategory(page.getCategory()).setRouting(routesOf(map, page), page.getPosition(), page.getShowOnErrorCodes(), page.getContentType()), roots);
				pagesByName.put(page.getName(), page.getUuid());

				if (page.getPath() != null) {
					pagesByPath.put(StringUtils.strip(page.getPath(), "/").split("/")[0], page.getUuid());
				}
			}
		}

		for (final HtmlNode html : all) {

			ScriptScanner.scan(map, (html.content != null ? html.content : "") + " " + (html.functionQuery != null ? html.functionQuery : ""), html.home);

			if (html.content != null) {

				final Matcher matcher = INCLUDE.matcher(html.content);

				while (matcher.find()) {
					map.addReference(html.home, componentIds.get(matcher.group(1)), "include", 0.3);
				}
			}

			final DOMNode sharedComponent = html.node.getSharedComponent();

			if (sharedComponent != null) {
				map.addReference(html.home, sharedComponent.getUuid(), "uses", 0.3);
			}

			if ("a".equals(html.tag)) {
				map.addReference(html.home, pageOf(html.attribute("_html_href"), pagesByPath, pagesByName), "navigates", 0.8);
			}

			for (final ActionMapping action : html.actions) {

				// the action mapping connects its page to the method it calls and the type it acts on
				map.connect(html.home, action.getMethodNode() != null ? action.getMethodNode().getUuid() : null);
				map.connect(html.home, action.getDataTypeNode() != null ? action.getDataTypeNode().getUuid() : null);

				map.addReference(html.home, pageOf(action.getSuccessURL(), pagesByPath, pagesByName), "navigates", 0.8);
				map.addReference(html.home, pageOf(action.getFailureURL(), pagesByPath, pagesByName), "navigates", 0.8);
			}
		}

		new DataBindings(map, all, url -> pageOf(url, pagesByPath, pagesByName)).contribute();

		for (final NodeInterface node : app.nodeQuery(StructrTraits.SITE).getAsList()) {

			final Site site          = node.as(Site.class);
			final String name        = StringUtils.defaultIfBlank(site.getHostname(), "(no host name)") + (site.getPort() != null ? ":" + site.getPort() : "");
			final List<String> pages = new LinkedList<>();

			site.getPages().forEach(page -> pages.add(page.getUuid()));

			map.addSite(site.getUuid(), name, pages);
		}

		outlines.forEach((node, roots) -> node.setOutline(outlineOf(roots)).setTemplate(onlyTemplateOf(roots)).setSize(sizeOf(roots)));
	}

	/**
	 * The URL routes of a page: per route its path, the type of its current object and the number of its warnings. The
	 * current object comes from the parameter marked as such, else from the only parameter of type Node; its type is the
	 * one the parameter is restricted to, and the page is connected to it.
	 */
	private List<Object> routesOf(final ApplicationMap map, final Page page) {

		final List<Object> routes = new LinkedList<>();

		for (final PagePath path : page.getPaths()) {

			final Map<String, Object> route     = new LinkedHashMap<>();
			final List<PagePathParameter> nodes = new LinkedList<>();
			PagePathParameter current           = null;

			for (final PagePathParameter parameter : path.getParameters()) {

				if ("Node".equals(parameter.getValueType())) {
					nodes.add(parameter);
				}

				if (parameter.getIsCurrentObject()) {
					current = parameter;
				}
			}

			if (current == null && nodes.size() == 1) {
				current = nodes.get(0);
			}

			final String type   = current != null && "Node".equals(current.getValueType()) ? StringUtils.trimToNull(current.getFormat()) : null;
			final String typeId = type != null ? map.getSchema().getTypeIds().get(type) : null;

			route.put("p", path.getName());

			if (type != null) {
				route.put("cur", type);
			}

			if (path.getWarnings() != null && path.getWarnings().length > 0) {
				route.put("w", path.getWarnings().length);
			}

			if (typeId != null) {

				map.addEdge(page.getUuid(), typeId, "current", 0.8, "route " + path.getName());
				map.connect(page.getUuid(), typeId);
			}

			routes.add(route);
		}

		return routes;
	}

	/**
	 * An HTML element lies in the elements above it, up to the root of its shared component or its page; an action mapping
	 * lies at its trigger element, a parameter mapping at its action mapping.
	 */
	@Override
	public List<String> locate(final NodeInterface node) {

		final List<String> chain = new LinkedList<>();

		if (node.is(StructrTraits.PARAMETER_MAPPING)) {

			final ActionMapping action = node.as(ParameterMapping.class).getActionMapping();

			if (action != null) {

				chain.add(action.getUuid());
				chain.addAll(locate(action));
			}

		} else if (node.is(StructrTraits.ACTION_MAPPING)) {

			for (final DOMElement trigger : node.as(ActionMapping.class).getTriggerElements()) {

				chain.add(trigger.getUuid());
				chain.addAll(locate(trigger));

				break;
			}

		} else if (node.is(StructrTraits.DOM_NODE) && !node.is(StructrTraits.PAGE)) {

			final DOMNode element = node.as(DOMNode.class);
			final Page page       = element.getOwnerDocument();

			for (DOMNode parent = element.getParent(); parent != null; parent = parent.getParent()) {
				chain.add(parent.getUuid());
			}

			// the root of a shared component is the component itself
			if (page != null && !page.is(StructrTraits.SHADOW_DOCUMENT)) {
				chain.add(page.getUuid());
			}
		}

		return chain;
	}

	// ----- private methods -----
	/**
	 * Takes a node and everything below it; every action mapping belongs to the first of its
	 * trigger elements that is met.
	 */
	private HtmlNode collect(final DOMNode node, final HtmlNode parent, final String home, final List<HtmlNode> all, final Set<String> assignedActions) {

		final HtmlNode html = new HtmlNode(node, parent, home);

		all.add(html);

		if (html.isElement) {

			for (final ActionMapping action : node.as(DOMElement.class).getTriggeredActions()) {

				if (assignedActions.add(action.getUuid())) {
					html.actions.add(action);
				}
			}
		}

		for (final DOMNode child : node.getChildren()) {
			html.children.add(collect(child, html, home, all, assignedActions));
		}

		return html;
	}

	/**
	 * The id of the template that a page or shared component consists of alone, with no element
	 * or other template below it, or null.
	 */
	private String onlyTemplateOf(final List<HtmlNode> roots) {

		if (roots.size() != 1 || !roots.get(0).node.is(StructrTraits.TEMPLATE)) {
			return null;
		}

		final HtmlNode template = roots.get(0);

		for (final HtmlNode child : template.children) {

			if (child.isElement || child.node.is(StructrTraits.TEMPLATE)) {
				return null;
			}
		}

		return template.id;
	}

	// the number of HTML elements below the given ones, themselves included: the size of a page or shared component
	private int sizeOf(final List<HtmlNode> nodes) {

		int size = 0;

		for (final HtmlNode html : nodes) {
			size += 1 + sizeOf(html.children);
		}

		return size;
	}

	private List<Object> outlineOf(final List<HtmlNode> roots) {

		final List<Object> outline = new LinkedList<>();

		for (final HtmlNode root : roots) {

			final List<Object> item = outlineOf(root, 0);

			if (item != null) {
				outline.add(item);
			}
		}

		return outline;
	}

	/**
	 * A compact outline: [tag, flags, children, ids, expressions], children only when there are
	 * any or something follows, ids only when there are any or expressions follow, expressions
	 * only when there are any; the flags are 1 (the element does something), 2 (it triggers an
	 * action mapping) and 4 (it repeats), the ids are those of the repeater and action mapping
	 * nodes that live on the element, the expressions those that its text shows, see
	 * expressionsOf. Text nodes and the head are left out.
	 */
	private List<Object> outlineOf(final HtmlNode html, final int depth) {

		if (!html.isElement && !html.node.is(StructrTraits.TEMPLATE)) {
			return null;
		}

		final String tag = html.isElement ? html.tag : "template";

		if (tag == null || SKIPPED_TAGS.contains(tag)) {
			return null;
		}

		final int flags             = (html.isActive ? 1 : 0) | (html.actions.isEmpty() ? 0 : 2) | (StringUtils.isNotEmpty(html.functionQuery) || StringUtils.isNotEmpty(html.dataKey) ? 4 : 0);
		final List<Object> item     = new LinkedList<>(List.of(tag, flags));
		final List<Object> children = new LinkedList<>();
		final List<String> ids      = new LinkedList<>(html.mapIds);

		for (final HtmlNode child : html.children) {

			final List<Object> childItem = depth < OUTLINE_DEPTH ? outlineOf(child, depth + 1) : null;

			if (childItem != null) {

				children.add(childItem);

			} else {

				// what lives below the depth of the outline, or inside an element it leaves out, is named at the deepest element it shows
				collectMapIds(child, ids);
			}
		}

		final String expressions = expressionsOf(html);
		final String dataKey     = StringUtils.trimToNull(html.dataKey);

		if (!children.isEmpty() || !ids.isEmpty() || expressions != null || dataKey != null) {
			item.add(children);
		}

		if (!ids.isEmpty() || expressions != null || dataKey != null) {
			item.add(ids);
		}

		if (expressions != null || dataKey != null) {
			item.add(StringUtils.defaultString(expressions));
		}

		if (dataKey != null) {
			item.add(dataKey);
		}

		return item;
	}

	/**
	 * The template expressions an element shows: those in its text nodes, or in its own content
	 * for a template, each once, shortened to EXPRESSIONS_LENGTH together; null when there are
	 * none. render(), include() and include_child() are left out, as they only place other
	 * elements, which the outline shows itself.
	 */
	private String expressionsOf(final HtmlNode html) {

		final Set<String> expressions = new LinkedHashSet<>();

		if (!html.isElement) {

			collectExpressions(html.content, expressions);

		} else {

			for (final HtmlNode child : html.children) {

				if (!child.isElement && !child.node.is(StructrTraits.TEMPLATE) && !child.node.is(StructrTraits.COMMENT)) {
					collectExpressions(child.content, expressions);
				}
			}
		}

		if (expressions.isEmpty()) {
			return null;
		}

		return StringUtils.abbreviate(String.join(" ", expressions), EXPRESSIONS_LENGTH);
	}

	/**
	 * Every ${...} in the text, up to the brace that closes it, with its whitespace collapsed.
	 */
	private void collectExpressions(final String text, final Set<String> expressions) {

		if (text == null) {
			return;
		}

		int start = text.indexOf("${");

		while (start >= 0) {

			int depth = 0;
			int end   = -1;

			for (int i = start + 1; i < text.length() && end < 0; i++) {

				final char c = text.charAt(i);

				if (c == '{') {

					depth++;

				} else if (c == '}' && --depth == 0) {

					end = i;
				}
			}

			if (end < 0) {
				return;
			}

			final String expression = text.substring(start, end + 1).replaceAll("\\s+", " ");

			if (!PLACING.matcher(expression).find()) {
				expressions.add(expression);
			}

			start = text.indexOf("${", end + 1);
		}
	}

	private void collectMapIds(final HtmlNode html, final List<String> ids) {

		ids.addAll(html.mapIds);

		for (final HtmlNode child : html.children) {
			collectMapIds(child, ids);
		}
	}

	/**
	 * The page a URL leads to, by the first part of its path or by the name of the page.
	 */
	private String pageOf(final String url, final Map<String, String> pagesByPath, final Map<String, String> pagesByName) {

		if (url == null || url.startsWith("http") || url.startsWith("javascript") || url.startsWith("#") || url.startsWith("mailto")) {
			return null;
		}

		final String first = StringUtils.stripStart(url, "/").split("\\?")[0].split("/")[0];

		if (first.isEmpty() || first.contains("$")) {
			return null;
		}

		return pagesByPath.containsKey(first) ? pagesByPath.get(first) : pagesByName.get(first);
	}

	/**
	 * The name, or a selector-like label such as "div#main.container" for unnamed elements.
	 */
	static String nameOf(final DOMNode node) {

		if (node.getName() != null) {
			return node.getName();
		}

		if (node.is(StructrTraits.DOM_ELEMENT)) {

			final DOMElement element = node.as(DOMElement.class);
			final String htmlId      = element.getHtmlId();
			final String cssClass    = element.getProperty(element.getTraits().key(DOMElementTraitDefinition._HTML_CLASS_PROPERTY));
			final StringBuilder name = new StringBuilder(element.getTag());

			if (StringUtils.isNotBlank(htmlId)) {
				name.append("#").append(htmlId);
			}

			// only the first class, and not when it is a script expression
			if (StringUtils.isNotBlank(cssClass) && !cssClass.trim().startsWith("${")) {
				name.append(".").append(cssClass.trim().split("\\s+")[0]);
			}

			return name.toString();
		}

		return node.getType().toLowerCase();
	}
}
