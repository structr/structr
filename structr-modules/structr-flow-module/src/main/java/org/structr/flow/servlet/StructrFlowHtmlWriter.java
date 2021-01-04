/*
 * Copyright (C) 2010-2021 Structr GmbH
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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.flow.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.util.html.Tag;
import org.structr.api.util.html.attr.Onload;
import org.structr.api.util.html.attr.Type;
import org.structr.common.SecurityContext;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.rest.serialization.RestWriter;
import org.structr.rest.serialization.StructrJsonHtmlWriter;

import java.io.IOException;
import java.io.PrintWriter;

public class StructrFlowHtmlWriter extends StructrJsonHtmlWriter {

	final static private Logger logger = LoggerFactory.getLogger(StreamingFlowWriter.class);

	public StructrFlowHtmlWriter(SecurityContext securityContext, PrintWriter rawWriter) {
		super(securityContext, rawWriter);
	}

	@Override
	public RestWriter beginDocument(final String baseUrl, final String propertyView) throws IOException {

		String currentType = baseUrl.replace(restPath + "/", "").replace("/" + propertyView, "");

		if (!propertyView.equals("public")) {
			this.propertyView = "/" + propertyView;
		}

		Tag head = doc.block("head");

		head.inline("style").attr(new Type("text/css")).text("body,html{font-family:\"Liberation Mono\",\"DejaVu Sans Mono\",Consolas,Monaco,\"Vera Sans Mono\",\"Lucida Console\",\"Courier New\",monospace!important;font-size:9pt;padding:0;margin:0}div#top{border-bottom:1px solid #a5a5a5;line-height:2em;background-color:#363636;padding:20px;font-family:Arial,Helvetica,Sans-serif}div#top a{color:#fff;text-decoration:none;font-weight:700;margin-right:1em}div#left{background-color:#eee;padding:10px 20px}div#left a{color:#666;text-decoration:none;font-weight:700;margin-right:1em}div#right{clear:both;padding-left:20px}ul{list-style-type:none}b{font-weight:700;color:#444}a.id,a.id:visited{color:#00a}span.type{font-weight:400;color:#080}span.id{font-weight:400;color:#999}span.name{font-weight:700;color:#666}span.string{color:#c60}span.boolean,span.number{color:#00a}span.null{color:#999}span.comma{color:#000}.collapsibleListOpen{list-style-image:url(data:image/gif;base64,R0lGODlhBwAHAOcYAAAAAAEBAQICAgMDAwQEBAUFBQYGBgcHBwgICAkJCQoKCgsLCwwMDA0NDQ4ODg8PDxAQEBERERISEhMTExQUFBUVFRYWFhcXFxgYGBkZGRoaGhsbGxwcHB0dHR4eHh8fHyAgICEhISIiIiMjIyQkJCUlJSYmJicnJygoKCkpKSoqKisrKywsLC0tLS4uLi8vLzAwMDExMTIyMjMzMzQ0NDU1NTY2Njc3Nzg4ODk5OTo6Ojs7Ozw8PD09PT4+Pj8/P0BAQEFBQUJCQkNDQ0REREVFRUZGRkdHR0hISElJSUpKSktLS0xMTE1NTU5OTk9PT1BQUFFRUVJSUlNTU1RUVFVVVVZWVldXV1hYWFlZWVpaWltbW1xcXF1dXV5eXl9fX2BgYGFhYWJiYmNjY2RkZGVlZWZmZmdnZ2hoaGlpaWpqamtra2xsbG1tbW5ubm9vb3BwcHFxcXJycnNzc3R0dHV1dXZ2dnd3d3h4eHl5eXp6ent7e3x8fH19fX5+fn9/f4CAgIGBgYKCgoODg4SEhIWFhYaGhoeHh4iIiImJiYqKiouLi4yMjI2NjY6Ojo+Pj5CQkJGRkZKSkpOTk5SUlJWVlZaWlpeXl5iYmJmZmZqampubm5ycnJ2dnZ6enp+fn6CgoKGhoaKioqOjo6SkpKWlpaampqenp6ioqKmpqaqqqqurq6ysrK2tra6urq+vr7CwsLGxsbKysrOzs7S0tLW1tba2tre3t7i4uLm5ubq6uru7u7y8vL29vb6+vr+/v8DAwMHBwcLCwsPDw8TExMXFxcbGxsfHx8jIyMnJycrKysvLy8zMzM3Nzc7Ozs/Pz9DQ0NHR0dLS0tPT09TU1NXV1dbW1tfX19jY2NnZ2dra2tvb29zc3N3d3d7e3t/f3+Dg4OHh4eLi4uPj4+Tk5OXl5ebm5ufn5+jo6Onp6erq6uvr6+zs7O3t7e7u7u/v7/Dw8PHx8fLy8vPz8/T09PX19fb29vf39/j4+Pn5+fr6+vv7+/z8/P39/f7+/v///yH5BAEKAP8ALAAAAAAHAAcAAAgRAP8JHEiwoMF/mRImPMjwYEAAOw==);cursor:pointer;}.collapsibleListClosed{list-style-image:url(data:image/gif;base64,R0lGODlhBwAHAOcYAAAAAAEBAQICAgMDAwQEBAUFBQYGBgcHBwgICAkJCQoKCgsLCwwMDA0NDQ4ODg8PDxAQEBERERISEhMTExQUFBUVFRYWFhcXFxgYGBkZGRoaGhsbGxwcHB0dHR4eHh8fHyAgICEhISIiIiMjIyQkJCUlJSYmJicnJygoKCkpKSoqKisrKywsLC0tLS4uLi8vLzAwMDExMTIyMjMzMzQ0NDU1NTY2Njc3Nzg4ODk5OTo6Ojs7Ozw8PD09PT4+Pj8/P0BAQEFBQUJCQkNDQ0REREVFRUZGRkdHR0hISElJSUpKSktLS0xMTE1NTU5OTk9PT1BQUFFRUVJSUlNTU1RUVFVVVVZWVldXV1hYWFlZWVpaWltbW1xcXF1dXV5eXl9fX2BgYGFhYWJiYmNjY2RkZGVlZWZmZmdnZ2hoaGlpaWpqamtra2xsbG1tbW5ubm9vb3BwcHFxcXJycnNzc3R0dHV1dXZ2dnd3d3h4eHl5eXp6ent7e3x8fH19fX5+fn9/f4CAgIGBgYKCgoODg4SEhIWFhYaGhoeHh4iIiImJiYqKiouLi4yMjI2NjY6Ojo+Pj5CQkJGRkZKSkpOTk5SUlJWVlZaWlpeXl5iYmJmZmZqampubm5ycnJ2dnZ6enp+fn6CgoKGhoaKioqOjo6SkpKWlpaampqenp6ioqKmpqaqqqqurq6ysrK2tra6urq+vr7CwsLGxsbKysrOzs7S0tLW1tba2tre3t7i4uLm5ubq6uru7u7y8vL29vb6+vr+/v8DAwMHBwcLCwsPDw8TExMXFxcbGxsfHx8jIyMnJycrKysvLy8zMzM3Nzc7Ozs/Pz9DQ0NHR0dLS0tPT09TU1NXV1dbW1tfX19jY2NnZ2dra2tvb29zc3N3d3d7e3t/f3+Dg4OHh4eLi4uPj4+Tk5OXl5ebm5ufn5+jo6Onp6erq6uvr6+zs7O3t7e7u7u/v7/Dw8PHx8fLy8vPz8/T09PX19fb29vf39/j4+Pn5+fr6+vv7+/z8/P39/f7+/v///yH5BAEKAP8ALAAAAAAHAAcAAAgXAP8JHEgwE0GBBgdmWriw4MF/CR8KDAgAOw==);cursor:pointer}.collapsibleList{cursor:default}button.collapse,button.expand{font-family:\"Liberation Mono\",\"DejaVu Sans Mono\",Consolas,Monaco,\"Vera Sans Mono\",\"Lucida Console\",\"Courier New\",monospace!important;float:right;margin:-.3em 1em 0 0}li.collapsibleListClosed>ul.collapsibleList>li{display:none}li.collapsibleListClosed>ul.collapsibleList::before{content:\"…\"}li.collapsibleListClosed>ul.collapsibleList:empty{display:none}li.collapsibleListClosed>ul.collapsibleList{line-height:11px;display:inline-block;border:1px solid #aaa;border-radius:6px;padding-left:2px;cursor:pointer;background-color:#ddd;padding-right:3px;margin-left:-5px;margin-right:-5px;font-family:initial}");
		head.inline("script").attr(new Type("text/javascript")).text("var CollapsibleLists=new function(){function g(b){return function(a){a||(a=window.event);for(a=a.target?a.target:a.srcElement;\"LI\"!=a.nodeName;)a=a.parentNode;a==b&&f(b)}}function f(b){for(var a=b.className.match(/(^| )collapsibleListClosed( |$)/),c=b.getElementsByTagName(\"ul\"),d=0;d<c.length;d++){for(var e=c[d];\"LI\"!=e.nodeName;)e=e.parentNode}b.className=b.className.replace(/(^| )collapsibleList(Open|Closed)( |$)/,\"\");0<c.length&&(b.className+=\" collapsibleList\"+\n" +
				"(a?\"Open\":\"Closed\"))}this.apply=function(b){for(var a=document.getElementsByTagName(\"ul\"),c=0;c<a.length;c++)if(a[c].className.match(/(^| )collapsibleList( |$)/)&&(this.applyTo(a[c],!0),!b))for(var d=a[c].getElementsByTagName(\"ul\"),e=0;e<d.length;e++)d[e].className+=\" collapsibleList\"};this.applyTo=function(b,a){for(var c=b.getElementsByTagName(\"li\"),d=0;d<c.length;d++)a&&b!=c[d].parentNode||(c[d].addEventListener?c[d].addEventListener(\"click\",g(c[d]),!1):c[d].attachEvent(\"onclick\",g(c[d])),f(c[d]))};\n" +
				"this.openAll=function(){var b = [].slice.call(document.getElementsByClassName(\"collapsibleListClosed\")); [].forEach.call(b, function (el) { f(el); });};this.closeAll=function(){var b = [].slice.call(document.getElementsByClassName(\"collapsibleListOpen\")); [].forEach.call(b, function (el) { f(el); });}};");

		head.inline("title").text(baseUrl);

		Tag body = doc.block("body").attr(new Onload("if (document.querySelectorAll('#right > ul > li > ul > li > ul.collapsibleList > li').length > 5) { CollapsibleLists.apply(true); }"));
		Tag top  = body.block("div").id("top");

		final App app  = StructrApp.getInstance(securityContext);
		final Tag left = body.block("div").id("left");

		// main div
		currentElement = body.block("div").id("right");

		// h1 title
		currentElement.block("h1").text(baseUrl);

		// begin ul
		currentElement = currentElement.block("ul");

		return this;
	}

}
