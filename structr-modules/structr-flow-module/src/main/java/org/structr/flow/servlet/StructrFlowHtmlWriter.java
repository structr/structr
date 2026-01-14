/*
 * Copyright (C) 2010-2026 Structr GmbH
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
import org.structr.api.util.html.attr.Href;
import org.structr.api.util.html.attr.Rel;
import org.structr.api.util.html.attr.Src;
import org.structr.api.util.html.attr.Type;
import org.structr.common.SecurityContext;
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
		head.empty("link").attr(new Rel("stylesheet"), new Type("text/css"), new Href("/structr/css/rest.css"));
		head.inline("script").attr(new Type("text/javascript"), new Src("/structr/js/rest.js"));

		head.inline("title").text(baseUrl);

		Tag body = doc.block("body");
		Tag top  = body.block("div").id("top");

//		final App app  = StructrApp.getInstance(securityContext);
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
