/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.api.util.html;

import org.structr.api.util.html.attr.Context;
import org.structr.api.util.html.attr.Css;
import org.structr.api.util.html.attr.Id;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 *
 *
 */
public abstract class Tag {

	private final List<Attr> attrs   = new LinkedList<>();
	private final List<Tag> children = new LinkedList<>();
	private boolean empty            = false;
	private boolean newline          = false;
	private String text              = null;
	private String tag               = null;
	private String indent            = "";
	private Tag parent               = null;

	Tag(final Tag parent, final String tagName) {
		this(parent, tagName, false);
	}

	Tag(final Tag parent, final String tagName, final boolean isEmpty) {
		this(parent, tagName, isEmpty, true);
	}

	Tag(final Tag parent, final String tagName, final boolean isEmpty, final boolean newline) {

		this.parent  = parent;
		this.tag     = tagName;
		this.empty   = isEmpty;
		this.newline = newline;
	}

	public Tag block(String tagName) {

		Tag tag = new Block(this, tagName);
		add(tag);

		return tag;
	}

	public Tag inline(String tagName) {

		Tag tag = new Inline(this, tagName);
		add(tag);

		return tag;
	}

	public Tag empty(String tagName) {

		Tag tag = new Empty(this, tagName);
		add(tag);

		return tag;
	}

	public Tag text(Object... content) {

		final StringBuilder buf = new StringBuilder();
		for (Object p : content) {
			buf.append(p);
		}

		this.text = buf.toString();

		return this;
	}

	public Tag textNode(String text) {

		Tag tag = new TextNode(this, text);
		add(tag);

		return tag;
	}

	public void setIndent(final String indent) {
		this.indent = indent;
	}

	public List<Tag> getChildren() {
		return children;
	}

	public Tag add(final Tag tag) {
		children.add(tag);
		return this;
	}

	public Tag attr(final Attr... attr) {
		attrs.addAll(Arrays.asList(attr));
		return this;
	}

	public Tag id(String id) {
		attr(new Id(id));
		return this;
	}

	public Tag css(String css) {
		attr(new Css(css));
		return this;
	}

	public Tag parent() {
		return parent;
	}

	public void appendComma () {

		if (!this.children.isEmpty()) {

			this.children.get(this.children.size() - 1).appendComma();

		} else if (this.text != null) {

			this.parent.textNode(",");
		}
	}

	// ----- protected methods -----
	protected void render(final PrintWriter writer, final int level) throws IOException {

		boolean newLine = false;

		for (Tag child : children) {
			newLine = newLine || (child instanceof Block);
		}

		beginTag(writer, tag, newLine, attrs, level, indent);

		if (!empty) {

			if (text != null) {
				writer.print(text);
			}

			for (Tag child : children) {
				child.render(writer, level + 1);
			}

			endTag(writer, tag, newline, level, indent);
		}
	}

	// ----- protected static methods -----
	protected static void beginTag(final PrintWriter writer, final String tagName, final boolean newline, final List<Attr> attributes, final int level, final String indent) throws IOException {
		beginTag(writer, tagName, newline, false, attributes, level, indent);
	}

	protected static void beginTag(final PrintWriter writer, final String tagName, final boolean newline, final boolean empty, final List<Attr> attributes, final int level, final String indent) throws IOException {

		final Context context = new Context(level);

		writer.flush();

		for (int i=0; i<level; i++) {
			writer.print(indent);
		}

		writer.print("<" + tagName);

		// print attributes
		for (Attr attr : attributes) {

			String output = attr.format(context);
			if (output.length() > 0) {

				writer.print(" " + output);
			}
		}

		if (newline) {

			if (empty) {

				writer.println("/>");

			} else {

				writer.println(">");
			}

		} else {

			if (empty) {

				writer.print("/>");

			} else {

				writer.print(">");
			}
		}
	}

	protected static void endTag(final PrintWriter writer, final String tagName, final boolean hasNewline, final int level, final String indent) throws IOException {

		writer.flush();

		if (hasNewline) {

			for (int i=0; i<level; i++) {
				writer.print(indent);
			}
		}

		if (hasNewline) {

			writer.println("</" + tagName + ">");

		} else {

			writer.print("</" + tagName + ">");
		}
	}
}
