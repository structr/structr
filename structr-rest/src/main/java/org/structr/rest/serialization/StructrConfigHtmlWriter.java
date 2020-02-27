/**
 * Copyright (C) 2010-2020 Structr GmbH
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
package org.structr.rest.serialization;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.util.html.Attr;
import org.structr.api.util.html.Document;
import org.structr.api.util.html.Tag;
import org.structr.api.util.html.attr.AtDepth;
import org.structr.api.util.html.attr.Css;
import org.structr.api.util.html.attr.Href;
import org.structr.api.util.html.attr.If;
import org.structr.api.util.html.attr.Onload;
import org.structr.api.util.html.attr.Type;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.SchemaNode;
import org.structr.core.graph.Tx;

/**
 *
 *
 */
public class StructrConfigHtmlWriter implements RestWriter {

	private static final Logger logger = LoggerFactory.getLogger(StructrConfigHtmlWriter.class.getName());

	private static final Set<String> hiddenViews = new LinkedHashSet<>();
	private static final int CLOSE_LEVEL         = 5;
	private static final String LI               = "li";
	private static final String UL               = "ul";

	private SecurityContext securityContext = null;
	private Document doc                    = null;
	private Tag currentElement              = null;
	private GraphObject currentObject       = null;
	private Tag previousElement             = null;
	private boolean hasName                 = false;
	private String lastName                 = null;
	private final String restPath           = StringUtils.removeEnd(Settings.RestServletPath.getValue(), "/*");
	private String propertyView	        = "";
	private int pageSize                    = -1;

	static {

		hiddenViews.add(PropertyView.All);
		hiddenViews.add(PropertyView.Html);
		hiddenViews.add(PropertyView.Ui);
	}

	public StructrConfigHtmlWriter(final SecurityContext securityContext, final PrintWriter rawWriter) {

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

		String currentType = baseUrl.replace(restPath + "/", "").replace("/" + propertyView, "");

		if (!propertyView.equals("public")) {
			this.propertyView = "/" + propertyView;
		}

		Tag head = doc.block("head");
//		head.empty("link").attr(new Rel("stylesheet"), new Type("text/css"), new Href("/structr/css/rest.css"));
//		head.inline("script").attr(new Type("text/javascript"), new Src("/structr/js/CollapsibleLists.js"));

		head.inline("style").attr(new Type("text/css")).text("body,html{font-family:\"Liberation Mono\",\"DejaVu Sans Mono\",Consolas,Monaco,\"Vera Sans Mono\",\"Lucida Console\",\"Courier New\",monospace!important;font-size:9pt;padding:0;margin:0}div#top{border-bottom:1px solid #a5a5a5;line-height:2em;background:#363636;background:-webkit-gradient(linear,left bottom,left top,from(#363636),to(#666)) no-repeat;background:-moz-linear-gradient(90deg,#363636,#666) no-repeat;filter:progid:DXImageTransform.Microsoft.Gradient(StartColorStr='#666666', EndColorStr='#363636', GradientType=0);padding:20px;font-family:Arial,Helvetica,Sans-serif}div#top a{color:#fff;text-decoration:none;font-weight:700;margin-right:1em}div#left{background-color:#eee;padding:10px 20px}div#left a{color:#666;text-decoration:none;font-weight:700;margin-right:1em}div#right{clear:both;padding-left:20px}ul{list-style-type:none}b{font-weight:700;color:#444}a.id,a.id:visited{color:#00a}span.type{font-weight:400;color:#080}span.id{font-weight:400;color:#999}span.name{font-weight:700;color:#666}span.string{color:#c60}span.boolean,span.number{color:#00a}span.null{color:#999}.collapsibleListOpen{list-style-image:url(data:image/gif;base64,R0lGODlhBwAHAOcYAAAAAAEBAQICAgMDAwQEBAUFBQYGBgcHBwgICAkJCQoKCgsLCwwMDA0NDQ4ODg8PDxAQEBERERISEhMTExQUFBUVFRYWFhcXFxgYGBkZGRoaGhsbGxwcHB0dHR4eHh8fHyAgICEhISIiIiMjIyQkJCUlJSYmJicnJygoKCkpKSoqKisrKywsLC0tLS4uLi8vLzAwMDExMTIyMjMzMzQ0NDU1NTY2Njc3Nzg4ODk5OTo6Ojs7Ozw8PD09PT4+Pj8/P0BAQEFBQUJCQkNDQ0REREVFRUZGRkdHR0hISElJSUpKSktLS0xMTE1NTU5OTk9PT1BQUFFRUVJSUlNTU1RUVFVVVVZWVldXV1hYWFlZWVpaWltbW1xcXF1dXV5eXl9fX2BgYGFhYWJiYmNjY2RkZGVlZWZmZmdnZ2hoaGlpaWpqamtra2xsbG1tbW5ubm9vb3BwcHFxcXJycnNzc3R0dHV1dXZ2dnd3d3h4eHl5eXp6ent7e3x8fH19fX5+fn9/f4CAgIGBgYKCgoODg4SEhIWFhYaGhoeHh4iIiImJiYqKiouLi4yMjI2NjY6Ojo+Pj5CQkJGRkZKSkpOTk5SUlJWVlZaWlpeXl5iYmJmZmZqampubm5ycnJ2dnZ6enp+fn6CgoKGhoaKioqOjo6SkpKWlpaampqenp6ioqKmpqaqqqqurq6ysrK2tra6urq+vr7CwsLGxsbKysrOzs7S0tLW1tba2tre3t7i4uLm5ubq6uru7u7y8vL29vb6+vr+/v8DAwMHBwcLCwsPDw8TExMXFxcbGxsfHx8jIyMnJycrKysvLy8zMzM3Nzc7Ozs/Pz9DQ0NHR0dLS0tPT09TU1NXV1dbW1tfX19jY2NnZ2dra2tvb29zc3N3d3d7e3t/f3+Dg4OHh4eLi4uPj4+Tk5OXl5ebm5ufn5+jo6Onp6erq6uvr6+zs7O3t7e7u7u/v7/Dw8PHx8fLy8vPz8/T09PX19fb29vf39/j4+Pn5+fr6+vv7+/z8/P39/f7+/v///yH5BAEKAP8ALAAAAAAHAAcAAAgRAP8JHEiwoMF/mRImPMjwYEAAOw==);cursor:pointer}.collapsibleListClosed{list-style-image:url(data:image/gif;base64,R0lGODlhBwAHAOcYAAAAAAEBAQICAgMDAwQEBAUFBQYGBgcHBwgICAkJCQoKCgsLCwwMDA0NDQ4ODg8PDxAQEBERERISEhMTExQUFBUVFRYWFhcXFxgYGBkZGRoaGhsbGxwcHB0dHR4eHh8fHyAgICEhISIiIiMjIyQkJCUlJSYmJicnJygoKCkpKSoqKisrKywsLC0tLS4uLi8vLzAwMDExMTIyMjMzMzQ0NDU1NTY2Njc3Nzg4ODk5OTo6Ojs7Ozw8PD09PT4+Pj8/P0BAQEFBQUJCQkNDQ0REREVFRUZGRkdHR0hISElJSUpKSktLS0xMTE1NTU5OTk9PT1BQUFFRUVJSUlNTU1RUVFVVVVZWVldXV1hYWFlZWVpaWltbW1xcXF1dXV5eXl9fX2BgYGFhYWJiYmNjY2RkZGVlZWZmZmdnZ2hoaGlpaWpqamtra2xsbG1tbW5ubm9vb3BwcHFxcXJycnNzc3R0dHV1dXZ2dnd3d3h4eHl5eXp6ent7e3x8fH19fX5+fn9/f4CAgIGBgYKCgoODg4SEhIWFhYaGhoeHh4iIiImJiYqKiouLi4yMjI2NjY6Ojo+Pj5CQkJGRkZKSkpOTk5SUlJWVlZaWlpeXl5iYmJmZmZqampubm5ycnJ2dnZ6enp+fn6CgoKGhoaKioqOjo6SkpKWlpaampqenp6ioqKmpqaqqqqurq6ysrK2tra6urq+vr7CwsLGxsbKysrOzs7S0tLW1tba2tre3t7i4uLm5ubq6uru7u7y8vL29vb6+vr+/v8DAwMHBwcLCwsPDw8TExMXFxcbGxsfHx8jIyMnJycrKysvLy8zMzM3Nzc7Ozs/Pz9DQ0NHR0dLS0tPT09TU1NXV1dbW1tfX19jY2NnZ2dra2tvb29zc3N3d3d7e3t/f3+Dg4OHh4eLi4uPj4+Tk5OXl5ebm5ufn5+jo6Onp6erq6uvr6+zs7O3t7e7u7u/v7/Dw8PHx8fLy8vPz8/T09PX19fb29vf39/j4+Pn5+fr6+vv7+/z8/P39/f7+/v///yH5BAEKAP8ALAAAAAAHAAcAAAgXAP8JHEgwE0GBBgdmWriw4MF/CR8KDAgAOw==);cursor:pointer}.collapsibleList{cursor:default}button.collapse,button.expand{font-family:\"Liberation Mono\",\"DejaVu Sans Mono\",Consolas,Monaco,\"Vera Sans Mono\",\"Lucida Console\",\"Courier New\",monospace!important;float:right;margin:-.3em 1em 0 0}");
		head.inline("script").attr(new Type("text/javascript")).text("var CollapsibleLists=new function(){function g(b){return function(a){a||(a=window.event);for(a=a.target?a.target:a.srcElement;\"LI\"!=a.nodeName;)a=a.parentNode;a==b&&f(b)}}function f(b){for(var a=b.className.match(/(^| )collapsibleListClosed( |$)/),c=b.getElementsByTagName(\"ul\"),d=0;d<c.length;d++){for(var e=c[d];\"LI\"!=e.nodeName;)e=e.parentNode;e==b&&(c[d].style.display=a?\"block\":\"none\")}b.className=b.className.replace(/(^| )collapsibleList(Open|Closed)( |$)/,\"\");0<c.length&&(b.className+=\" collapsibleList\"+\n" +
			"(a?\"Open\":\"Closed\"))}this.apply=function(b){for(var a=document.getElementsByTagName(\"ul\"),c=0;c<a.length;c++)if(a[c].className.match(/(^| )collapsibleList( |$)/)&&(this.applyTo(a[c],!0),!b))for(var d=a[c].getElementsByTagName(\"ul\"),e=0;e<d.length;e++)d[e].className+=\" collapsibleList\"};this.applyTo=function(b,a){for(var c=b.getElementsByTagName(\"li\"),d=0;d<c.length;d++)a&&b!=c[d].parentNode||(c[d].addEventListener?c[d].addEventListener(\"click\",g(c[d]),!1):c[d].attachEvent(\"onclick\",g(c[d])),f(c[d]))};\n" +
			"this.openAll=function(){var b = [].slice.call(document.getElementsByClassName(\"collapsibleListClosed\")); [].forEach.call(b, function (el) { f(el); });};this.closeAll=function(){var b = [].slice.call(document.getElementsByClassName(\"collapsibleListOpen\")); [].forEach.call(b, function (el) { f(el); });}};");

		head.inline("title").text(baseUrl);

		Tag body = doc.block("body").attr(new Onload("if (document.querySelectorAll('#right > ul > li > ul > li > ul.collapsibleList > li').length > 5) { CollapsibleLists.apply(true); }"));
		Tag top  = body.block("div").id("top");

		final App app  = StructrApp.getInstance(securityContext);
		final Tag left = body.block("div").id("left");

		left.inline("button").attr(new Css("collapse right")).attr(new Attr("onclick", "CollapsibleLists.closeAll()")).text(" - ");
		left.inline("button").attr(new Css("expand right")).attr(new Attr("onclick", "CollapsibleLists.openAll()")).text(" + ");

		try (final Tx tx = app.tx()) {

			final List<SchemaNode> schemaNodes = app.nodeQuery(SchemaNode.class).getAsList();
			Collections.sort(schemaNodes);

			for (SchemaNode node : schemaNodes) {

				final String rawType = node.getName();
				top.inline("a").attr(new Href(restPath + "/" + rawType), new If(rawType.equals(currentType), new Css("active"))).text(rawType);
			}

		} catch (Throwable t) {
			logger.warn("", t);
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

		currentElement.inline("span").text("[");	// print [
		currentElement = currentElement.block(UL).attr(new AtDepth(CLOSE_LEVEL, new Css("collapsibleList")));

		hasName = false;

		return this;
	}

	@Override
	public RestWriter endArray() throws IOException {

		currentElement = currentElement.parent();	// end LI
		currentElement.inline("span").text("]");	// print ]
		previousElement = currentElement;
		currentElement = currentElement.parent();	// end UL

		return this;
	}

	@Override
	public RestWriter beginObject() throws IOException {
		return beginObject(null);
	}

	@Override
	public RestWriter beginObject(final GraphObject graphObject) throws IOException {

		increaseSerializationDepth();

		currentObject = graphObject;

		if (!hasName) {
			currentElement = currentElement.block(LI);
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

		decreaseSerializationDepth();

		currentElement = currentElement.parent();	// end UL
		currentElement.inline("span").text("}");	// print }
		previousElement = currentElement;
		currentElement = currentElement.parent();	// end LI

		return this;
	}

	@Override
	public RestWriter name(final String name) throws IOException {

		if (previousElement != null) {
			previousElement.appendComma();
		}
		previousElement = currentElement;

		currentElement = currentElement.block(LI);

		currentElement.inline("b").text("\"", name, "\":");

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

			if (currentObject == null) {

				currentElement.inline("a").css("id").attr(new Href(restPath + "/" + value + propertyView)).text("\"", value, "\"");

			} else if (currentObject instanceof AbstractRelationship) {

				currentElement.inline("a").css("id").attr(new Href(restPath + "/" + currentObject.getProperty(AbstractRelationship.type) + "/" + value + propertyView)).text("\"", value, "\"");

			} else {

				currentElement.inline("a").css("id").attr(new Href(restPath + "/" + currentObject.getType() + "/" + value + propertyView)).text("\"", value, "\"");

			}

		} else {

			value = value.replaceAll("\\\\", "\\\\\\\\");           // escape backslashes in strings
			value = value.replaceAll("\"", "\\\\\\\"");             // escape quotation marks inside strings

			// Escape for HTML output
			value = StringUtils.replaceEach(value, new String[]{"&", "<", ">"}, new String[]{"&amp;", "&lt;", "&gt;"});

			currentElement.inline("span").css("string").text("\"", value, "\"");

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

	@Override
	public void raw(final String data) throws IOException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void flush() throws IOException {
	}

	@Override
	public void setPageSize(final int pageSize) {
		this.pageSize = pageSize;
	}

	@Override
	public int getPageSize() {
		return pageSize;
	}
}
