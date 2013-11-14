package org.structr.rest.serialization.html;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.structr.rest.serialization.html.attr.Context;
import org.structr.rest.serialization.html.attr.Css;
import org.structr.rest.serialization.html.attr.Id;

/**
 *
 * @author Christian Morgner
 */
public abstract class Tag {

	private List<Attr> attrs   = new LinkedList<Attr>();
	private List<Tag> children = new LinkedList<Tag>();
	private boolean empty      = false;
	private boolean newline    = false;
	private String text        = null;
	private String tag         = null;
	private String indent      = "  ";
	private Tag parent         = null;
	
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
	
		StringBuilder buf = new StringBuilder();
		for (Object p : content) {
			buf.append(p);
		}

		this.text = buf.toString();
		
		return this;
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

	// ----- protected methods -----
	protected void render(final PrintWriter writer, final int level) throws IOException {
		
		beginTag(writer, tag, empty, attrs, level, indent);
		
		if (!empty) {
			
			if (text != null) {
				writer.print(text);
			}
			
			if (newline) {
				writer.println();
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
		
		writer.println("</" + tagName + ">");
	}
}
