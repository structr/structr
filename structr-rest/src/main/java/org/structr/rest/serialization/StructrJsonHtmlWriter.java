package org.structr.rest.serialization;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.module.ModuleService;

/**
 *
 * @author Christian Morgner
 */
public class StructrJsonHtmlWriter implements RestWriter {

	private static final int CLOSE_LEVEL = 5;
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
		this.indent = indent;
	}

	@Override
	public void flush() throws IOException {
		writer.flush();
	}

	@Override
	public RestWriter beginDocument(final String baseUrl, final String propertyView) throws IOException {
		
		String restPath    = Services.getConfigurationValue(Services.REST_PATH);
		String currentType = baseUrl.replace(restPath + "/", "").replace("/" + propertyView, "");
		
		beginTag("html", true);

		beginTag("head", true);
		beginTag("link", true, true, new Rel("stylesheet"), new Type("text/css"), new Href("//structr.org/rest.css"));
		beginTag("script", false, new Type("text/javascript"), new Src("//structr.org/CollapsibleLists.js"));
		endTag("script", false);
		beginTag("title", false);
		writer.print(baseUrl);
		endTag("title", false);
		endTag("head", true);
		
		beginTag("body", true, new Onload("CollapsibleLists.apply(true);"));
		beginTag("div", true, new Id("top"));
		beginTag("ul", true);

		for (String view : EntityContext.getPropertyViews()) {
			
			beginTag("li", true);
			beginTag("a", false, new Href(restPath + "/" + currentType + "/" + view), new Conditional(view.equals(propertyView), new Css("active")));
			writer.print(view);
			endTag("a", false);
			endTag("li", true);
		}
		
		endTag("ul", true);
		endTag("div", true);
		
		beginTag("div", true, new Id("left"));

		ModuleService moduleService = (ModuleService)Services.getService(ModuleService.class);
		for (String rawType : moduleService.getCachedNodeEntityTypes()) {
			
			beginTag("p", true);
			beginTag("a", false, new Href(restPath + "/" + rawType), new Conditional(rawType.equals(currentType), new Css("active")));
			writer.print(rawType);
			endTag("a", false);
			endTag("p", true);
		}

		endTag("div", true);
		
		beginTag("div", true, new Id("right"));
		
		// display url
		beginTag("h1", false);
		writer.print(baseUrl);
		endTag("h1", false);
		
		beginTag("ul", true);
		
		return this;
	}

	@Override
	public RestWriter endDocument() throws IOException {
		
		endTag("ul", true);
		endTag("div", true);
		endTag("body", true);
		endTag("html", true);
		
		return this;
	}

	@Override
	public RestWriter beginArray() throws IOException {

		beginTag(UL, true, new Conditional(level > CLOSE_LEVEL, new Css("collapsibleList")));

		hasName = false;
		
		return this;
	}

	@Override
	public RestWriter endArray() throws IOException {
		
		endTag(LI, true);
		endTag(UL, true);
		
		return this;
	}

	@Override
	public RestWriter beginObject() throws IOException {
		return beginObject(null);
	}

	@Override
	public RestWriter beginObject(final GraphObject graphObject) throws IOException {
		
		if (!hasName) {

			beginTag(LI, true);
			beginTag("b", true);
			
			if (graphObject != null) {

				final String name = graphObject.getProperty(AbstractNode.name);
				final String uuid = graphObject.getUuid();
				final String type = graphObject.getType();

				if (name != null) {
					
					beginTag("span", false, new Css("name"));
					writer.print(name);
					endTag("span", false);
				}

				if (uuid != null) {
					
					beginTag("span", false, new Css("id"));
					writer.print(uuid);
					endTag("span", false);
				}

				if (type != null) {
					
					beginTag("span", false, new Css("type"));
					writer.print(type);
					endTag("span", false);
				}
			}
			
			endTag("b", true);
		}
		
		writer.println(" {");
		
		beginTag(UL, true, new Conditional(level > CLOSE_LEVEL, new Css("collapsibleList")));

		hasName = false;
		
		return this;
	}

	@Override
	public RestWriter endObject() throws IOException {
		return endObject(null);
	}

	@Override
	public RestWriter endObject(final GraphObject graphObject) throws IOException {
		
		endTag(UL, true);
		writer.println("}");
		endTag(LI, true);
		
		return this;
	}

	@Override
	public RestWriter name(String name) throws IOException {

		beginTag(LI, true);
		
		beginTag("b", false);
		writer.print(name + ":");
		endTag("b", false);

		lastName = name;
		hasName = true;
		
		return this;
	}

	@Override
	public RestWriter value(String value) throws IOException {

		if ("id".equals(lastName)) {
		
			beginTag("a", false, new Css("id"), new Href(value));
			writer.print(value);
			endTag("a", false);
			
		} else {
		
			beginTag("span", false, new Css("string"));
			writer.print(value);
			endTag("span", false);
			
		}

		endTag(LI, true);
		
		hasName = false;
		
		return this;
	}

	@Override
	public RestWriter nullValue() throws IOException {
		
		beginTag("span", false, new Css("null"));
		writer.print("null");
		endTag("span", false);
		endTag(LI, true);
		
		hasName = false;
		
		return this;
	}

	@Override
	public RestWriter value(boolean value) throws IOException {
		
		beginTag("span", false, new Css("boolean"));
		writer.print(value);
		endTag("span", false);
		endTag(LI, true);

		hasName = false;
		
		return this;
	}

	@Override
	public RestWriter value(double value) throws IOException {
		
		beginTag("span", false, new Css("number"));
		writer.print(value);
		endTag("span", false);
		endTag(LI, true);

		hasName = false;
		
		return this;
	}

	@Override
	public RestWriter value(long value) throws IOException {
		
		beginTag("span", false, new Css("number"));
		writer.print(value);
		endTag("span", false);
		endTag(LI, true);
		
		hasName = false;
		
		return this;
	}

	@Override
	public RestWriter value(Number value) throws IOException {
		
		beginTag("span", false, new Css("number"));
		writer.print(value);
		endTag("span", false);
		endTag(LI, true);

		hasName = false;
		
		return this;
	}
	
	private void beginTag(final String tagName, final boolean newline, final Attr... attributes) throws IOException {
		beginTag(tagName, newline, false, attributes);
	}
	
	private void beginTag(final String tagName, final boolean newline, final boolean empty, final Attr... attributes) throws IOException {
		
		writer.flush();
		
		for (int i=0; i<level; i++) {
			writer.print(indent);
		}
			
		writer.print("<" + tagName);

		// print attributes
		for (Attr attr : attributes) {
			
			String value = attr.toString();
			if (value.length() > 0) {
				
				writer.print(" " + attr.toString());
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

		if (!empty) {
			level++;
		}
	}
	
	private void endTag(final String tagName, final boolean hasNewline) throws IOException {
		
		level--;

		writer.flush();
		
		if (hasNewline) {
			
			for (int i=0; i<level; i++) {
				writer.print(indent);
			}
		}
		
		writer.println("</" + tagName + ">");
	}
	
	private static class Attr {
		
		private String key = null;
		private String value = null;
		
		public Attr(final String key, final String value) {
			this.key = key;
			this.value = value;
		}
		
		@Override
		public String toString() {
			return key + "=\"" + value + "\"";
		}
		
		public String getKey() {
			return key;
		}
		
		public String getValue() {
			return value;
		}
	}
	
	private static class Id extends Attr {
		
		public Id(final String id) {
			super("id", id);
		}
	}
	
	private static class Css extends Attr {
		
		public Css(final String css) {
			super("class", css);
		}
	}
	
	private static class Type extends Attr {
		
		public Type(final String type) {
			super("type", type);
		}
	}
	
	private static class Rel extends Attr {
		
		public Rel(final String rel) {
			super("rel", rel);
		}
	}
	
	private static class Href extends Attr {
		
		public Href(final String href) {
			super("href", href);
		}
	}
	
	private static class Src extends Attr {
		
		public Src(final String src) {
			super("src", src);
		}
	}
	
	private static class Onload extends Attr {
		
		public Onload(final String onload) {
			super("onload", onload);
		}
	}
	
	private static class Conditional extends Attr {
	
		private boolean condition = false;
		
		public Conditional(boolean condition, Attr attr) {
			
			super(attr.key, attr.value);
			
			this.condition = condition;
		}
		
		@Override
		public String toString() {
			
			if (condition) {
				return super.toString();
			}
			
			return "";
		}
	}
}
