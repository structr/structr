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
import org.structr.docs.Documentable;
import org.structr.docs.Documentation;
import org.structr.docs.OutputSettings;
import org.structr.docs.analyzer.ExistingDocs;
import org.structr.docs.formatter.json.JsonConceptFormatter;
import org.structr.docs.formatter.json.SearchResultsConceptFormatter;
import org.structr.docs.formatter.json.TableOfContentsConceptFormatter;
import org.structr.docs.formatter.markdown.*;
import org.structr.docs.formatter.text.PlaintextMarkdownFileFormatter;
import org.structr.docs.formatter.text.PlaintextTopicFormatter;
import org.structr.docs.ontology.Concept;
import org.structr.docs.ontology.ConceptType;
import org.structr.docs.ontology.Details;
import org.structr.docs.ontology.Ontology;
import org.structr.rest.service.HttpService;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

@Documentation(name="DocumentationServlet", parent="Servlets", children={ "DocumentationServlet Settings" })
public class DocumentationServlet extends HttpServlet {

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		try {

			final HttpService service                = Services.getInstance().getServiceImplementation(HttpService.class);
			final ResourceHandler resourceHandler    = service.getExportedResourceHandler();
			final Resource baseResource              = resourceHandler.getBaseResource();
			final Resource facts                     = baseResource.resolve("facts");
			final Ontology ontology                  = new Ontology(facts.getPath());
			final OutputSettings settings            = setupOutputSettings(ontology, baseResource);
			final Map<Concept, Double> searchResults = new LinkedHashMap<>();
			final List<Concept> concepts             = new LinkedList<>();

			handleRequestParameters(request, ontology, concepts, settings, searchResults);

			if (isSearch(request)) {

				final SearchResultsConceptFormatter searchResultsFormatter = new SearchResultsConceptFormatter();
				final List<Map.Entry<Concept, Double>> sortedResults       = new LinkedList<>();
				final List<String> lines                                   = new LinkedList<>();

				for (final Map.Entry<Concept, Double> entry : searchResults.entrySet()) {

					sortedResults.add(entry);
				}

				// sort by score
				final Comparator<Map.Entry<Concept, Double>> comparator = Comparator.comparingDouble(Map.Entry::getValue);

				Collections.sort(sortedResults, comparator.reversed());

				for (final Map.Entry<Concept, Double> entry : sortedResults) {

					final Concept concept = entry.getKey();
					final Double score    = entry.getValue();

					searchResultsFormatter.format(lines, concept, score);
				}

				renderJson(response, lines);

			} else {

				// compare ontology to existing docs
				if (request.getParameter("includeMentions") != null) {

					ontology.countConcepts(new ExistingDocs("structr/docs"));
				}

				final List<String> lines = ontology.createDocumentation(concepts, settings);

				if ("markdown".equals(settings.getOutputFormat())) {
					renderMarkdown(response, lines, settings);
				}

				if ("text".equals(settings.getOutputFormat())) {
					renderPlaintext(response, lines);
				}

				if ("json".equals(settings.getOutputFormat()) || "toc".equals(settings.getOutputFormat())) {
					renderJson(response, lines);
				}
			}

		} catch (Throwable t) {

			t.printStackTrace();

			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().print(t.getMessage());
		}
	}

	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		// simply write out the markdown documentation on POST for now..
		Documentable.createMarkdownDocumentation();
	}

	// ----- private methods -----
	private void renderMarkdown(final HttpServletResponse response, final List<String> lines, final OutputSettings settings) throws IOException {

		response.setContentType("text/html; charset=utf-8");

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
		writer.write("    <link rel=\"stylesheet\" href=\"/structr/css/docs-new.css\" />\n");

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

		response.setContentType("text/plain; charset=utf-8");

		final Writer writer = response.getWriter();

		for (final String line : lines) {

			writer.write(line);
			writer.write("\n");
		}
	}

	private void renderJson(final HttpServletResponse response, final List<String> lines) throws IOException {

		response.setContentType("application/json; charset=utf-8");

		final Writer writer = response.getWriter();

		writer.write("{ \"data\": [\n");
		writer.write(StringUtils.join(lines, ",\n"));
		writer.write("]}\n");
	}

	private boolean handleRequestParameters(final HttpServletRequest request, final Ontology ontology, final List<Concept> concepts, final OutputSettings settings, final Map<Concept, Double> searchResults) {

		boolean hasFilter = false;

		// format?
		final String format = request.getParameter("format");
		if (StringUtils.isNotBlank(format)) {

			settings.setOutputFormat(format);
		}

		// types filter?
		final String types = request.getParameter("types");
		if (StringUtils.isNotBlank(types)) {

			final Set<ConceptType> typeSet = new LinkedHashSet<>();
			for (final String type : types.split(",")) {

				final String trimmed = type.trim();
				if (StringUtils.isNotBlank(trimmed)) {

					typeSet.add(ConceptType.valueOf(trimmed));
				}
			}

			settings.setTypesToRender(typeSet);

			// clear link types (only one level)
			settings.setLinkTypes(Map.of());

			// do not add default set of concepts
			hasFilter = true;
		}

		// type filter?
		final String type = request.getParameter("type");
		if (StringUtils.isNotBlank(type)) {

			if ("*".equals(type)) {

				concepts.addAll(ontology.getAllConcepts());

			} else {

				concepts.addAll(ontology.getConcepts(c -> c.getType().equals(type)));
			}

			// do not add default set of concepts
			hasFilter = true;
		}

		// search
		final String search = request.getParameter("search");
		if (StringUtils.isNotBlank(search)) {

			ontology.searchConcepts(searchResults, search.trim().toLowerCase());

			for (final String part : search.split("[, ]+")) {

				ontology.searchConcepts(searchResults, part.trim().toLowerCase());
			}

			// do not add default set of concepts
			hasFilter = true;
		}

		// root?
		final String root = request.getParameter("root");
		if (StringUtils.isNotBlank(root)) {

			if (root.contains(":")) {

				final String[] parts    = root.split(":");
				final String typeString = parts[0];
				final String name       = parts[1];
				final ConceptType t     = ConceptType.valueOf(typeString);

				concepts.add(ontology.getConcept(t, name));

			} else {

				concepts.addAll(ontology.getConceptsByName(root));
			}

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

		return hasFilter;
	}

	private OutputSettings setupOutputSettings(final Ontology ontology, final Resource baseResource) {

		final OutputSettings settings = new OutputSettings(ontology, 0, 5);

		// sensible default
		settings.getDetails().add(Details.name);

		settings.setFormatterForOutputFormatModeAndType("markdown", "overview", ConceptType.Topic,          new ToplevelTopicsMarkdownFormatter());
		settings.setFormatterForOutputFormatModeAndType("markdown", "overview", ConceptType.MarkdownFolder, new ToplevelTopicsMarkdownFormatter());
		settings.setFormatterForOutputFormatModeAndType("markdown", "overview", ConceptType.MarkdownFile,   new MarkdownMarkdownFileFormatter(baseResource));
		settings.setFormatterForOutputFormatModeAndType("markdown", "overview", ConceptType.CodeSource,     new MarkdownCodeSourceFormatter());
		settings.setFormatterForOutputFormatModeAndType("markdown", "overview", ConceptType.Table,          new MarkdownTableFormatter());
		settings.setFormatterForOutputFormatModeAndType("markdown", "overview", ConceptType.Glossary,       new MarkdownGlossaryFormatter());

		settings.setFormatterForOutputFormatModeAndType("text", "overview", ConceptType.Unknown,            new PlaintextTopicFormatter());
		settings.setFormatterForOutputFormatModeAndType("text", "overview", ConceptType.Topic,              new PlaintextTopicFormatter());
		settings.setFormatterForOutputFormatModeAndType("text", "overview", ConceptType.MarkdownFolder,     new PlaintextTopicFormatter());
		settings.setFormatterForOutputFormatModeAndType("text", "overview", ConceptType.MarkdownFile,       new PlaintextMarkdownFileFormatter(baseResource));

		settings.setFormatterForOutputFormatModeAndType("json", "overview", ConceptType.Unknown,            new JsonConceptFormatter());

		// table of contents for inline documentation
		settings.setFormatterForOutputFormatModeAndType("toc", "overview", ConceptType.Unknown,            new TableOfContentsConceptFormatter());

		return settings;
	}

	private boolean isSearch(final HttpServletRequest request) {
		return request.getParameter("search") != null;
	}
}

