package org.structr.rest.serialization;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;

/**
 *
 * @author Christian Morgner
 */
public class StructrJsonHtmlWriter implements RestWriter {

	private static final String LI = "li";
	private static final String UL = "ul";
	
	private PrintWriter writer = null;
	private boolean hasName    = false;
	private String lastName    = null;
	private String indent      = "";
	private int level          = 0;
	
	public StructrJsonHtmlWriter(final Writer rawWriter) {
		
		this.writer = new PrintWriter(rawWriter, false);
		
		writer.println("<!DOCTYPE html>");
	}
	
	@Override
	public void setIndent(String indent) {
		//writer.setIndent(indent);
		this.indent = indent;
	}

	@Override
	public void flush() throws IOException {
		writer.flush();
	}

	@Override
	public RestWriter beginDocument() throws IOException {
		
		beginTag("html", true);

		beginTag("head", true);
		writer.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"//structr.org/rest.css\" />");
		writer.println("<script type=\"text/javascript\" src=\"//structr.org/CollapsibleLists.js\"></script>");
		endTag("head", true);
		
		writer.println("<body onload=\"CollapsibleLists.apply();\">");
		writer.println("<ul class=\"collapsibleList\">");
		
		level++;
		
		return this;
	}

	@Override
	public RestWriter endDocument() throws IOException {
		
		level--;
		
		endTag("ul", true);
		endTag("body", true);
		endTag("html", true);
		
		return this;
	}

	@Override
	public RestWriter beginArray() throws IOException {

		writer.println();
		
		beginTag(UL, true);

		level++;
		
		hasName = false;
		
		return this;
	}

	@Override
	public RestWriter endArray() throws IOException {
		
		level--;

		endTag(UL, true);
		
		return this;
	}

	@Override
	public RestWriter beginObject() throws IOException {
		return beginObject(null);
	}

	@Override
	public RestWriter beginObject(final GraphObject graphObject) throws IOException {
		
		writer.println();
		
		if (!hasName) {

			beginTag(LI, true);
			beginTag("b", false);
		
			if (graphObject != null) {

				final String name = graphObject.getProperty(AbstractNode.name);
				final String uuid = graphObject.getUuid();
				final String type = graphObject.getType();

				if (name != null) {
					writer.print("<span class=\"name\">" + name + "</span>");
				}

				if (uuid != null) {
					writer.print(" <span class=\"id\">" + uuid + "</span>");
				}

				if (type != null) {
					writer.print(" <span class=\"type\">" + type + "</span>");
				}
			}
			
			endTag("b", false);
		}
		
		writer.println(" {");
		
		beginTag(UL, true);
		
		level++;
		
		hasName = false;
		
		return this;
	}

	@Override
	public RestWriter endObject() throws IOException {
		return endObject(null);
	}

	@Override
	public RestWriter endObject(final GraphObject graphObject) throws IOException {
		
		level--;

		endTag(UL, true);
		writer.println("}");
		endTag(LI, true);
		
		return this;
	}

	@Override
	public RestWriter name(String name) throws IOException {

		beginTag(LI, false);
		
		writer.print("<b>" + name + ":</b> ");

		lastName = name;
		hasName = true;
		
		return this;
	}

	@Override
	public RestWriter value(String value) throws IOException {

		if ("id".equals(lastName)) {
		
			writer.println("<a class=\"id\" href=\"" + value + "\">" + value + "</a>");
			
		} else {
		
			writer.println("<span class=\"string\">\"" + value + "\"</span>");
			
		}
		
		endTag(LI, false);
		
		hasName = false;
		
		return this;
	}

	@Override
	public RestWriter nullValue() throws IOException {
		
		writer.print("<span class=\"null\">null</span>");
		endTag(LI, false);
		
		hasName = false;
		
		return this;
	}

	@Override
	public RestWriter value(boolean value) throws IOException {
		
		writer.print("<span class=\"boolean\">" + value + "</span>");
		endTag(LI, false);
		
		hasName = false;
		
		return this;
	}

	@Override
	public RestWriter value(double value) throws IOException {
		
		writer.print("<span class=\"number\">" + value + "</span>");
		endTag(LI, false);
		
		hasName = false;
		
		return this;
	}

	@Override
	public RestWriter value(long value) throws IOException {
		
		writer.print("<span class=\"number\">" + value + "</span>");
		endTag(LI, false);
		
		hasName = false;
		
		return this;
	}

	@Override
	public RestWriter value(Number value) throws IOException {
		
		writer.print("<span class=\"number\">" + value + "</span>");
		endTag(LI, false);
		
		hasName = false;
		
		return this;
	}
	
	private void beginTag(final String tagName, final boolean newline) throws IOException {

		writer.flush();
		
		for (int i=0; i<level; i++) {
			writer.print(indent);
		}
		
		if (newline) {
			
			writer.println("<" + tagName + ">");
			
		} else {
			
			writer.print("<" + tagName + ">");
		}
	}
	
	private void endTag(final String tagName, final boolean hasNewline) throws IOException {

		writer.flush();
		
		if (!hasNewline) {
			
			for (int i=0; i<level; i++) {
				writer.print(indent);
			}
		}
		
		writer.println("</" + tagName + ">");
	}
}
