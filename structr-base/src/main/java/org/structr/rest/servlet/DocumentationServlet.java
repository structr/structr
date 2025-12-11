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
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.structr.core.Services;
import org.structr.docs.OutputSettings;
import org.structr.docs.formatter.*;
import org.structr.docs.ontology.Concept;
import org.structr.docs.ontology.Details;
import org.structr.docs.ontology.Ontology;
import org.structr.rest.service.HttpService;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

public class DocumentationServlet extends HttpServlet {

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		try {

			final HttpService service             = Services.getInstance().getServiceImplementation(HttpService.class);
			final ResourceHandler resourceHandler = service.getExportedResourceHandler();
			final Resource baseResource           = resourceHandler.getBaseResource();
			final Resource facts                  = baseResource.resolve("facts");
			final Ontology ontology               = new Ontology(facts.getPath());
			final OutputSettings settings         = setupOutputSettings(baseResource);
			final List<Concept> concepts          = new LinkedList<>();

			handleRequestParameters(request, ontology, concepts, settings);

			final List<String> lines = ontology.createDocumentation(concepts, settings);

			if ("markdown".equals(settings.getOutputFormat())) {
				renderMarkdown(response, lines, settings);
			}

			if ("text".equals(settings.getOutputFormat())) {
				renderPlaintext(response, lines);
			}


		} catch (Throwable t) {

			t.printStackTrace();

			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().print(t.getMessage());
		}
	}

	// ----- private methods -----
	private void renderMarkdown(final HttpServletResponse response, final List<String> lines, final OutputSettings settings) throws IOException {

		final MutableDataSet options = new MutableDataSet();

		options.setAll(PegdownOptionsAdapter.flexmarkOptions(false, Extensions.ALL));
		//options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");

		final Parser parser         = Parser.builder(options).build();
		final HtmlRenderer renderer = HtmlRenderer.builder(options).build();
		final Document doc          = parser.parse(StringUtils.join(lines, "\n"));
		final Writer writer         = response.getWriter();

		writer.write("<!DOCTYPE html>\n");
		writer.write("<html>\n");
		writer.write("<head>\n");
		writer.write("    <link rel=\"stylesheet\" href=\"/structr/css/main.css\" />\n");
		writer.write("    <link rel=\"stylesheet\" href=\"/structr/css/docs.css\" />\n");

		if (settings.getBaseUrl() != null) {
			writer.write("<base href='" + settings.getBaseUrl() + "'/>\n");
		}

		writer.write("</head>\n");
		writer.write("<body>\n");
		writer.write("<article class=\"article\">\n");

		renderer.render(doc, writer);

		writer.write("</article>\n");
		writer.write("</body>\n");
		writer.write("</html>\n");
	}

	private void renderPlaintext(final HttpServletResponse response, final List<String> lines) throws IOException {

		final Writer writer = response.getWriter();

		for (final String line : lines) {

			writer.write(line);
			writer.write("\n");
		}
	}



	private void handleRequestParameters(final HttpServletRequest request, final Ontology ontology, final List<Concept> concepts, final OutputSettings settings) {

		boolean hasFilter = false;

		// format?
		final String format = request.getParameter("format");
		if (StringUtils.isNotBlank(format)) {

			settings.setOutputFormat(format);
		}

		// types filter?
		final String types = request.getParameter("types");
		if (StringUtils.isNotBlank(types)) {

			for (final String type : types.split("[,]+")) {

				concepts.addAll(ontology.getConcepts(c -> c.getType().equals(type)));
			}

			// clear link types (only one level)
			settings.setLinkTypes(Map.of());

			// set start level to 1, 0 is ignored
			settings.setStartLevel(1);

			// do not add default set of concepts
			hasFilter = true;
		}

		// type filter?
		final String type = request.getParameter("type");
		if (StringUtils.isNotBlank(type)) {

			settings.setTypeToRender(type);

			// do not add default set of concepts
			hasFilter = true;
		}

		// root?
		final String root = request.getParameter("root");
		if (StringUtils.isNotBlank(root)) {

			concepts.add(ontology.getConcept("unknown", root));

			// do not add default set of concepts
			hasFilter = true;
		}

		final String startLevel = request.getParameter("startLevel");
		if (StringUtils.isNotBlank(startLevel) && StringUtils.isNumeric(startLevel)) {

			settings.setStartLevel(Integer.parseInt(startLevel));
		}

		final String levels = request.getParameter("levels");
		if (StringUtils.isNotBlank(levels) && StringUtils.isNumeric(levels)) {

			settings.setMaxLevels(Integer.parseInt(levels));
		}

		// default behaviour: add all root concepts
		if (concepts.isEmpty() && !hasFilter) {

			concepts.addAll(ontology.getRootConcepts());
		}

		// details?
		final String details = request.getParameter("details");
		if (StringUtils.isNotBlank(details)) {

			final String[] parts = details.split(",");
			for (final String part : parts) {

				settings.getDetails().add(Details.valueOf(part.trim()));
			}
		}
	}

	private OutputSettings setupOutputSettings(final Resource baseResource) {

		final OutputSettings settings = new OutputSettings(1, Integer.MAX_VALUE);

		settings.setFormatterForOutputFormatAndType("markdown", "topic",           new MarkdownTopicFormatter());
		settings.setFormatterForOutputFormatAndType("markdown", "service",         new MarkdownServiceFormatter());
		settings.setFormatterForOutputFormatAndType("markdown", "markdown-source", new MarkdownMarkdownSourceFormatter(baseResource));
		settings.setFormatterForOutputFormatAndType("markdown", "code-source",     new MarkdownCodeSourceFormatter());

		settings.setFormatterForOutputFormatAndType("text", "topic",               new PlaintextTopicFormatter());
		settings.setFormatterForOutputFormatAndType("text", "markdown-source",     new PlaintextMarkdownSourceFormatter(baseResource));

		return settings;
	}
}
