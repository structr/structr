/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.rest.serialization;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashSet;
import java.util.Set;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.SchemaNode;
import org.structr.core.graph.Tx;
import org.structr.rest.serialization.html.Document;
import org.structr.rest.serialization.html.Tag;
import org.structr.rest.serialization.html.attr.Css;
import org.structr.rest.serialization.html.attr.AtDepth;
import org.structr.rest.serialization.html.attr.Href;
import org.structr.rest.serialization.html.attr.If;
import org.structr.rest.serialization.html.attr.Onload;
import org.structr.rest.serialization.html.attr.Rel;
import org.structr.rest.serialization.html.attr.Src;
import org.structr.rest.serialization.html.attr.Type;

/**
 *
 * @author Christian Morgner
 */
public class StructrJsonHtmlWriter implements RestWriter {

	private static final Set<String> hiddenViews = new LinkedHashSet<>();
	private static final int CLOSE_LEVEL         = 5;
	private static final String LI               = "li";
	private static final String UL               = "ul";

	private SecurityContext securityContext = null;
	private Document doc                    = null;
	private Tag currentElement              = null;
	private boolean hasName                 = false;
	private String lastName                 = null;

	static {

		hiddenViews.add(PropertyView.All);
		hiddenViews.add(PropertyView.Html);
		hiddenViews.add(PropertyView.Ui);
	}

	public StructrJsonHtmlWriter(final SecurityContext securityContext, final PrintWriter rawWriter) {

		this.securityContext = securityContext;
		this.doc = new Document(rawWriter);
	}

	@Override
	public void setIndent(String indent) {
		doc.setIndent(indent);
	}

	@Override
	public SecurityContext getSecurityContext() {
		return securityContext;
	}

	@Override
	public RestWriter beginDocument(final String baseUrl, final String propertyView) throws IOException {

		String restPath    = "/structr/rest";
		String currentType = baseUrl.replace(restPath + "/", "").replace("/" + propertyView, "");

		Tag head = doc.block("head");
		head.empty("link").attr(new Rel("stylesheet"), new Type("text/css"), new Href("//structr.org/rest.css"));
		head.inline("script").attr(new Type("text/javascript"), new Src("//structr.org/CollapsibleLists.js"));
		head.inline("title").text(baseUrl);

		Tag body = doc.block("body").attr(new Onload("CollapsibleLists.apply(true);"));
		Tag top  = body.block("div").id("top");

		final App app  = StructrApp.getInstance(securityContext);
		final Tag left = body.block("div").id("left");

		try (final Tx tx = app.tx()) {

			for (SchemaNode node : app.nodeQuery(SchemaNode.class).getAsList()) {

				final String rawType = node.getName();
				top.inline("a").attr(new Href(restPath + "/" + rawType), new If(rawType.equals(currentType), new Css("active"))).text(rawType);
			}

		} catch (Throwable t) {
			t.printStackTrace();
		}

		for (String view : StructrApp.getConfiguration().getPropertyViews()) {

			if (!hiddenViews.contains(view)) {
				left.inline("a").attr(new Href(restPath + "/" + currentType + "/" + view), new If(view.equals(propertyView), new Css("active"))).text(view);
			}
		}

		// main div
		currentElement = body.block("div").id("right");

		// h1 title
		currentElement.block("h1").text(baseUrl);

		// begin ul
		currentElement = currentElement.block("ul");

		return this;
	}

	@Override
	public RestWriter endDocument() throws IOException {

		// finally render document
		doc.render();

		return this;
	}

	@Override
	public RestWriter beginArray() throws IOException {

		currentElement = currentElement.block(UL).attr(new AtDepth(CLOSE_LEVEL, new Css("collapsibleList")));

		hasName = false;

		return this;
	}

	@Override
	public RestWriter endArray() throws IOException {

		currentElement = currentElement.parent();	// end LI
		currentElement = currentElement.parent();	// end UL

		return this;
	}

	@Override
	public RestWriter beginObject() throws IOException {
		return beginObject(null);
	}

	@Override
	public RestWriter beginObject(final GraphObject graphObject) throws IOException {

		if (!hasName) {

			currentElement = currentElement.block(LI);

			Tag b = currentElement.block("b");

			if (graphObject != null) {

				final String name = graphObject.getProperty(AbstractNode.name);
				final String uuid = graphObject.getUuid();
				final String type = graphObject.getType();

				if (name != null) {

					b.inline("span").css("name").text(name);
				}

				if (uuid != null) {

					b.inline("span").css("id").text(uuid);
				}

				if (type != null) {

					b.inline("span").css("type").text(type);
				}
			}
		}

		currentElement.inline("span").text("{");

		currentElement = currentElement.block(UL).attr(new AtDepth(CLOSE_LEVEL, new Css("collapsibleList")));

		hasName = false;

		return this;
	}

	@Override
	public RestWriter endObject() throws IOException {
		return endObject(null);
	}

	@Override
	public RestWriter endObject(final GraphObject graphObject) throws IOException {

		currentElement = currentElement.parent();	// end UL
		currentElement.inline("span").text("}");	// print }
		currentElement = currentElement.parent();	// end LI

		return this;
	}

	@Override
	public RestWriter name(String name) throws IOException {

		currentElement = currentElement.block(LI);

		currentElement.inline("b").text(name, ":");

		lastName = name;
		hasName = true;

		return this;
	}

	@Override
	public RestWriter value(String value) throws IOException {

		if (!hasName) {

			currentElement = currentElement.block("li");
		}

		if ("id".equals(lastName)) {

			currentElement.inline("a").css("id").attr(new Href(value)).text(value);

		} else {

			currentElement.inline("span").css("string").text(value);

		}

		currentElement = currentElement.parent();	// end LI

		hasName = false;

		return this;
	}

	@Override
	public RestWriter nullValue() throws IOException {

		if (!hasName) {

			currentElement = currentElement.block("li");
		}

		currentElement.inline("span").css("null").text("null");
		currentElement = currentElement.parent();

		hasName = false;

		return this;
	}

	@Override
	public RestWriter value(boolean value) throws IOException {

		if (!hasName) {

			currentElement = currentElement.block("li");
		}

		currentElement.inline("span").css("boolean").text(value);
		currentElement = currentElement.parent();

		hasName = false;

		return this;
	}

	@Override
	public RestWriter value(double value) throws IOException {

		if (!hasName) {

			currentElement = currentElement.block("li");
		}

		currentElement.inline("span").css("number").text(value);
		currentElement = currentElement.parent();

		hasName = false;

		return this;
	}

	@Override
	public RestWriter value(long value) throws IOException {

		if (!hasName) {

			currentElement = currentElement.block("li");
		}

		currentElement.inline("span").css("number").text(value);
		currentElement = currentElement.parent();

		hasName = false;

		return this;
	}

	@Override
	public RestWriter value(Number value) throws IOException {

		if (!hasName) {

			currentElement = currentElement.block("li");
		}

		currentElement.inline("span").css("number").text(value);
		currentElement = currentElement.parent();

		hasName = false;

		return this;
	}
}
