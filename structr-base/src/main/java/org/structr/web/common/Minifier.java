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
package org.structr.web.common;

import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Parser;
import org.structr.common.error.FrameworkException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Minifies web resources: JavaScript with terser and CSS with csso, both run in a sandboxed GraalJS
 * context of their own, HTML, SVG and XML with jsoup, and JSON with Gson.
 */
public final class Minifier {

	public enum Kind {

		JavaScript("application/javascript"),
		Css("text/css"),
		Html("text/html"),
		Svg("image/svg+xml"),
		Xml("application/xml"),
		Json("application/json");

		private final String contentType;

		Kind(final String contentType) {
			this.contentType = contentType;
		}

		public String getContentType() {
			return contentType;
		}
	}

	private static final Pattern WHITESPACE            = Pattern.compile("[ \\t\\n\\r\\f]+");
	private static final Set<String> RAW_HTML_ELEMENTS    = Set.of("pre", "textarea", "script", "style");
	private static final Set<String> SVG_TEXT_ELEMENTS    = Set.of("text", "tspan", "textPath");
	private static final Set<String> JS_SCRIPT_TYPES      = Set.of("", "text/javascript", "application/javascript", "module");
	private static final Object lock                      = new Object();
	private static Context context                        = null;
	private static Value terser                           = null;
	private static Value csso                             = null;

	private Minifier() {}

	/**
	 * Detects the kind of resource from its content type, or from the extension of its name when the content type
	 * is missing or not specific. Returns null for anything that cannot be minified.
	 */
	public static Kind detect(final String contentType, final String name) {

		final Kind byContentType = detectByContentType(contentType);
		if (byContentType != null) {

			return byContentType;
		}

		return detectByExtension(name);
	}

	public static String minify(final Kind kind, final String source, final String name) throws FrameworkException {

		return switch (kind) {

			case JavaScript -> minifyJavaScript(source, isModule(name));
			case Css        -> minifyCss(source);
			case Html       -> minifyHtml(source);
			case Svg        -> minifyXml(source, true);
			case Xml        -> minifyXml(source, false);
			case Json       -> minifyJson(source);
		};
	}

	public static String minifyJavaScript(final String source, final boolean module) throws FrameworkException {

		synchronized (lock) {

			try {

				init();

				return terser.execute(source, module).asString();

			} catch (PolyglotException ex) {

				throw new FrameworkException(422, "Unable to minify JavaScript: " + ex.getMessage());
			}
		}
	}

	public static String minifyCss(final String source) throws FrameworkException {

		synchronized (lock) {

			try {

				init();

				return csso.execute(source).asString();

			} catch (PolyglotException ex) {

				throw new FrameworkException(422, "Unable to minify CSS: " + ex.getMessage());
			}
		}
	}

	public static String minifyJson(final String source) throws FrameworkException {

		try {

			return JsonParser.parseString(source).toString();

		} catch (RuntimeException ex) {

			throw new FrameworkException(422, "Unable to minify JSON: " + ex.getMessage());
		}
	}

	public static String minifyHtml(final String source) throws FrameworkException {

		final String head       = StringUtils.left(source.stripLeading(), 1024).toLowerCase(Locale.ROOT);
		final boolean document  = head.startsWith("<!doctype") || head.contains("<html");
		final Document doc      = document ? Jsoup.parse(source) : Jsoup.parseBodyFragment(source);

		doc.outputSettings().prettyPrint(false);

		minifyHtmlNode(doc);

		return document ? doc.outerHtml() : doc.body().html();
	}

	public static String minifyXml(final String source, final boolean svg) {

		final Document doc = Jsoup.parse(source, "", Parser.xmlParser());

		doc.outputSettings().prettyPrint(false).syntax(Document.OutputSettings.Syntax.xml);

		minifyXmlNode(doc, svg, false);

		return doc.outerHtml();
	}

	// ----- private methods -----
	private static void minifyHtmlNode(final Node node) throws FrameworkException {

		for (final Node child : new ArrayList<>(node.childNodes())) {

			if (child instanceof Comment comment) {

				// conditional comments carry markup for old browsers
				if (!comment.getData().startsWith("[if")) {

					comment.remove();
				}

			} else if (child instanceof TextNode text) {

				if (!hasRawHtmlAncestor(text)) {

					String collapsed = WHITESPACE.matcher(text.getWholeText()).replaceAll(" ");

					// a removed comment can leave two text nodes side by side
					if (collapsed.startsWith(" ") && text.previousSibling() instanceof TextNode previous && previous.getWholeText().endsWith(" ")) {

						collapsed = collapsed.substring(1);
					}

					if (collapsed.isEmpty()) {

						text.remove();

					} else {

						text.text(collapsed);
					}
				}

			} else if (child instanceof Element element) {

				final String tag = element.normalName();

				if ("script".equals(tag) && !element.hasAttr("src")) {

					minifyInlineScript(element);

				} else if ("style".equals(tag)) {

					final String css = element.data();
					if (!css.isBlank()) {

						element.empty().appendChild(new DataNode(minifyCss(css)));
					}

				} else {

					minifyHtmlNode(element);
				}
			}
		}
	}

	private static void minifyInlineScript(final Element script) throws FrameworkException {

		final String type = script.attr("type").trim().toLowerCase(Locale.ROOT);
		final String code = script.data();

		if (code.isBlank()) {

			return;
		}

		if (JS_SCRIPT_TYPES.contains(type)) {

			script.empty().appendChild(new DataNode(minifyJavaScript(code, "module".equals(type))));

		} else if (type.endsWith("json")) {

			script.empty().appendChild(new DataNode(minifyJson(code)));
		}
	}

	private static boolean hasRawHtmlAncestor(final Node node) {

		for (Element parent = (Element)node.parent(); parent != null; parent = parent.parent()) {

			if (RAW_HTML_ELEMENTS.contains(parent.normalName())) {

				return true;
			}
		}

		return false;
	}

	private static void minifyXmlNode(final Node node, final boolean svg, final boolean preserve) {

		for (final Node child : new ArrayList<>(node.childNodes())) {

			if (child instanceof Comment) {

				child.remove();

			} else if (child instanceof TextNode text && !(child instanceof org.jsoup.nodes.CDataNode)) {

				if (!preserve && text.isBlank()) {

					text.remove();
				}

			} else if (child instanceof Element element) {

				final String space         = element.attr("xml:space");
				final boolean keepSpace    = "preserve".equals(space) || (preserve && !"default".equals(space)) || (svg && SVG_TEXT_ELEMENTS.contains(element.tagName()));

				minifyXmlNode(element, svg, keepSpace);
			}
		}
	}

	private static boolean isModule(final String name) {

		return name != null && name.toLowerCase(Locale.ROOT).endsWith(".mjs");
	}

	private static Kind detectByContentType(final String contentType) {

		if (contentType == null) {

			return null;
		}

		final String type = StringUtils.substringBefore(contentType, ";").trim().toLowerCase(Locale.ROOT);

		return switch (type) {

			case "application/javascript", "text/javascript", "application/x-javascript", "application/ecmascript", "text/ecmascript" -> Kind.JavaScript;
			case "text/css"                                                                                                           -> Kind.Css;
			case "text/html"                                                                                                          -> Kind.Html;
			case "image/svg+xml"                                                                                                      -> Kind.Svg;
			case "application/xml", "text/xml"                                                                                        -> Kind.Xml;
			case "application/json", "text/json"                                                                                      -> Kind.Json;
			default -> type.endsWith("+json") ? Kind.Json : type.endsWith("+xml") ? Kind.Xml : null;
		};
	}

	private static Kind detectByExtension(final String name) {

		if (name == null || !name.contains(".")) {

			return null;
		}

		return switch (StringUtils.substringAfterLast(name, ".").toLowerCase(Locale.ROOT)) {

			case "js", "mjs", "cjs"  -> Kind.JavaScript;
			case "css"               -> Kind.Css;
			case "html", "htm"       -> Kind.Html;
			case "svg"               -> Kind.Svg;
			case "xml"               -> Kind.Xml;
			case "json", "webmanifest" -> Kind.Json;
			default                  -> null;
		};
	}

	private static void init() throws FrameworkException {

		if (context != null) {

			return;
		}

		try {

			// no host access, no IO: the bundles only ever see the strings they minify
			final Context ctx = Context.newBuilder("js")
				.engine(Engine.newBuilder().option("engine.WarnInterpreterOnly", "false").build())
				.option("js.ecmascript-version", "latest")
				.build();

			ctx.eval(load("terser.js"));
			ctx.eval(load("csso.js"));

			terser  = ctx.eval("js", "(source, module) => Terser.minify_sync(source, { module: module }).code");
			csso    = ctx.eval("js", "(source) => csso.minify(source).css");
			context = ctx;

		} catch (IOException | PolyglotException ex) {

			throw new FrameworkException(500, "Unable to initialize the minifier: " + ex.getMessage());
		}
	}

	private static Source load(final String name) throws IOException, FrameworkException {

		final InputStream stream = Minifier.class.getResourceAsStream("/minify/" + name);
		if (stream == null) {

			throw new FrameworkException(500, "Unable to initialize the minifier: resource /minify/" + name + " not found");
		}

		try (final InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {

			return Source.newBuilder("js", reader, name).build();
		}
	}
}
