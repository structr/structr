package org.structr.rest.serialization;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
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

	private static final int CLOSE_LEVEL = 5;
	private static final String LI = "li";
	private static final String UL = "ul";
	
	private Document doc       = null;
	private Tag currentElement = null;
	private boolean hasName    = false;
	private String lastName    = null;
	
	public StructrJsonHtmlWriter(final Writer rawWriter) {
		
		this.doc = new Document(new PrintWriter(rawWriter, false));
	}
	
	@Override
	public void setIndent(String indent) {
		doc.setIndent(indent);
	}

	@Override
	public void flush() throws IOException {
		doc.flush();
	}

	@Override
	public RestWriter beginDocument(final String baseUrl, final String propertyView) throws IOException {
		
		String restPath    = StructrApp.getConfigurationValue(Services.REST_PATH);
		String currentType = baseUrl.replace(restPath + "/", "").replace("/" + propertyView, "");

		Tag head = doc.block("head");
		head.empty("link").attr(new Rel("stylesheet"), new Type("text/css"), new Href("//structr.org/rest.css"));
		head.inline("script").attr(new Type("text/javascript"), new Src("//structr.org/CollapsibleLists.js"));
		head.inline("title").text(baseUrl);
		
		Tag body = doc.block("body").attr(new Onload("CollapsibleLists.apply(true);"));
		Tag top  = body.block("div").id("top");
		Tag ul1  = top.block("ul");

		for (String view : StructrApp.getConfiguration().getPropertyViews()) {
			
			ul1.block("li").inline("a").attr(new Href(restPath + "/" + currentType + "/" + view), new If(view.equals(propertyView), new Css("active"))).text(view);
		}
		
		Tag left = body.block("div").id("left");

		for (String rawType : StructrApp.getConfiguration().getNodeEntities().keySet()) {
			
			left.block("p").inline("a").attr(new Href(restPath + "/" + rawType), new If(rawType.equals(currentType), new Css("active"))).text(rawType);
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
		
		currentElement.inline("span").css("null").text("null");
		currentElement = currentElement.parent();
		
		hasName = false;
		
		return this;
	}

	@Override
	public RestWriter value(boolean value) throws IOException {
		
		currentElement.inline("span").css("boolean").text(value);
		currentElement = currentElement.parent();

		hasName = false;
		
		return this;
	}

	@Override
	public RestWriter value(double value) throws IOException {
		
		currentElement.inline("span").css("number").text(value);
		currentElement = currentElement.parent();

		hasName = false;
		
		return this;
	}

	@Override
	public RestWriter value(long value) throws IOException {
		
		currentElement.inline("span").css("number").text(value);
		currentElement = currentElement.parent();
		
		hasName = false;
		
		return this;
	}

	@Override
	public RestWriter value(Number value) throws IOException {
		
		currentElement.inline("span").css("number").text(value);
		currentElement = currentElement.parent();

		hasName = false;
		
		return this;
	}
}
