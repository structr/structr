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
package org.structr.rest.servlet;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.profile.pegdown.Extensions;
import com.vladsch.flexmark.profile.pegdown.PegdownOptionsAdapter;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.data.MutableDataSet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.structr.docs.ontology.Details;
import org.structr.docs.ontology.Ontology;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;

public class DocumentationServlet extends HttpServlet {

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		final List<String> lines     = Ontology.getInstance().createMarkdownDocumentation(EnumSet.allOf(Details.class));
		final MutableDataSet options = new MutableDataSet();

		options.setAll(PegdownOptionsAdapter.flexmarkOptions(false, Extensions.ALL));
		//options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");

		final Parser parser         = Parser.builder(options).build();
		final HtmlRenderer renderer = HtmlRenderer.builder(options).build();


		final Document doc = parser.parse(StringUtils.join(lines, "\n"));

		renderer.render(doc, response.getWriter());
	}
}
