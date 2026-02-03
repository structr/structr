/*
 * Copyright (C) 2010-2026 Structr GmbH
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
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.structr.core.Services;
import org.structr.core.function.tokenizer.FactsTokenizer;
import org.structr.core.function.tokenizer.Token;
import org.structr.docs.Documentable;
import org.structr.docs.Documentation;
import org.structr.docs.Formatter;
import org.structr.docs.OutputSettings;
import org.structr.docs.analyzer.ExistingDocs;
import org.structr.docs.formatter.json.JsonConceptFormatter;
import org.structr.docs.formatter.json.SearchResultsConceptFormatter;
import org.structr.docs.formatter.json.TableOfContentsConceptFormatter;
import org.structr.docs.formatter.json.TableOfContentsMarkdownFileFormatter;
import org.structr.docs.formatter.markdown.*;
import org.structr.docs.formatter.text.PlaintextMarkdownFileFormatter;
import org.structr.docs.formatter.text.PlaintextTopicFormatter;
import org.structr.docs.formatter.text.RawConceptFormatter;
import org.structr.docs.formatter.text.RawMarkdownFileFormatter;
import org.structr.docs.ontology.*;
import org.structr.rest.service.HttpService;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Documentation(name="DocumentationServlet", parent="Servlets", children={ "DocumentationServlet Settings" })
public class DocumentationServlet extends HttpServlet {

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		try {

			final HttpService service                = Services.getInstance().getServiceImplementation(HttpService.class);
			final ResourceHandler resourceHandler    = service.getExportedResourceHandler();
			final Resource baseResource              = resourceHandler.getBaseResource();
			final Resource facts                     = baseResource.resolve("facts");
			final Ontology ontology                  = new Ontology(baseResource, facts.getPath());
			final OutputSettings settings            = setupOutputSettings(ontology, baseResource);
			final Map<Concept, Double> searchResults = new LinkedHashMap<>();
			final List<Link> links                   = new LinkedList<>();

			handleRequest(request, ontology, links, settings, searchResults);

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

				final List<String> lines = ontology.createDocumentation(links, settings);

				if ("markdown".equals(settings.getOutputFormat())) {

					replaceIncludes(ontology, lines, settings);

					renderMarkdown(response, lines, settings);
				}

				if ("text".equals(settings.getOutputFormat())) {
					renderPlaintext(response, lines);
				}

				if ("raw".equals(settings.getOutputFormat())) {
					renderRawContent(response, lines);
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

	@Override
	protected void doPut(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		// update markdown file here
		final String conceptId = request.getParameter("id");
		if (StringUtils.isNotBlank(conceptId)) {

			final HttpService service                = Services.getInstance().getServiceImplementation(HttpService.class);
			final ResourceHandler resourceHandler    = service.getExportedResourceHandler();
			final Resource baseResource              = resourceHandler.getBaseResource();
			final Resource facts                     = baseResource.resolve("facts");
			final Ontology ontology                  = new Ontology(baseResource, facts.getPath());
			final Concept concept                    = ontology.getConceptById(conceptId);

			if (concept != null) {

				try (final InputStream inputStream = request.getInputStream()) {

					final String content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
					final String key     = request.getParameter("key");

					// update content
					concept.updateContent(key, content);

					// write facts files
					ontology.updateFactsContainers();

					// send new HTML content to client
					response.setContentType("text/html; charset=utf-8");

					final MutableDataSet options = new MutableDataSet();

					options.setAll(PegdownOptionsAdapter.flexmarkOptions(false, Extensions.ALL));
					//options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");

					final Parser parser         = Parser.builder(options).build();
					final HtmlRenderer renderer = HtmlRenderer.builder(options).build();
					final Document doc          = parser.parse(content);
					final Writer writer         = response.getWriter();

					renderer.render(doc, writer);
				}

			} else {


				response.setStatus(404);
				response.getWriter().print("Concept " + conceptId + " does not exist.");
			}

		} else {

			response.setStatus(422);
			response.getWriter().print("No fileName provided for update.");
		}
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

	private void renderRawContent(final HttpServletResponse response, final List<String> lines) throws IOException {

		response.setContentType("text/plain; charset=utf-8");

		final Writer writer = response.getWriter();

		writer.write(StringUtils.join(lines, "\n"));
	}

	private void handleRequest(final HttpServletRequest request, final Ontology ontology, final List<Link> links, final OutputSettings settings, final Map<Concept, Double> searchResults) {

		final String path = request.getPathInfo();
		if (StringUtils.isNotBlank(path)) {

			final String[] parts = StringUtils.split(path, '/');
			if (parts.length == 2) {

				// parent exists
				final List<Concept> parents = ontology.getConceptsByName(parts[0]);
				if (!parents.isEmpty()) {

					for (final Concept parent : parents) {

						for (final Link link : parent.getChildLinks(Verb.Has)) {

							if (parts[1].equals(link.getTarget().getName())) {

								links.add(link);
							}
						}
					}
				}

			} else {

				final List<Concept> parents = ontology.getConceptsByName(parts[0]);
				for (final Concept parent : parents) {

					links.add(new Link(null, null, parent));
				}
			}

		} else {

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

					links.addAll(ontology.getAllConcepts().stream().map(c -> new Link(null, null, c)).toList());

				} else {

					links.addAll(ontology.getConcepts(c -> c.getType().equals(type)).stream().map(c -> new Link(null, null, c)).toList());
				}

				// do not add default set of concepts
				hasFilter = true;
			}

			// search
			final String search = request.getParameter("search");
			if (StringUtils.isNotBlank(search)) {

				if (search.contains(" ")) {

					for (final String part : search.split("[ ]+")) {

						ontology.searchConcepts(searchResults, part.trim().toLowerCase(), 1.0);
					}
				}

				// matching the whole search string gets 100x the score
				ontology.searchConcepts(searchResults, search.trim().toLowerCase(), 100.0);

				// do not add default set of concepts
				hasFilter = true;
			}

			// root
			final String root = request.getParameter("root");
			if (StringUtils.isNotBlank(root)) {

				if (root.contains(":")) {

					final String[] parts = root.split(":");
					final String typeString = parts[0];
					final String name = parts[1];
					final ConceptType t = ConceptType.valueOf(typeString);

					links.addAll(ontology.getConcept(t, name).stream().map(c -> new Link(null, null, c)).toList());

				} else {

					links.addAll(ontology.getConceptsByName(root).stream().map(c -> new Link(null, null, c)).toList());
				}

				// do not add default set of concepts
				hasFilter = true;
			}

			final String id = request.getParameter("id");
			if (StringUtils.isNotBlank(id)) {

				final Concept concept = ontology.getConceptById(id);
				if (concept != null) {

					final String key = request.getParameter("key");
					if (StringUtils.isNotBlank(key)) {

						settings.setKey(key);
					}

					Link link = new Link(null, null, concept);

					// parent set?
					if (request.getParameter("parent") != null) {

						final Concept parent = ontology.getConceptById(request.getParameter("parent"));
						if (parent != null) {

							final Link parentLink = parent.getLinkTo(Verb.Has, concept);
							if (parentLink != null) {

								// use existing link
								link = parentLink;
							}
						}
					}

					links.add(link);
				}

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
			if (links.isEmpty() && !hasFilter) {

				links.addAll(ontology.getRootConcepts().stream().map(c -> new Link(null, null, c)).toList());
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
	}

	private OutputSettings setupOutputSettings(final Ontology ontology, final Resource baseResource) {

		final OutputSettings settings = new OutputSettings(ontology, 0, 5);

		// sensible default
		settings.getDetails().add(Details.name);

		settings.setFormatterForOutputFormatModeAndType("markdown", "overview", ConceptType.Topic,            new ToplevelTopicsMarkdownFormatter());
		settings.setFormatterForOutputFormatModeAndType("markdown", "overview", ConceptType.SystemType,       new SystemTypeMarkdownFormatter());
		settings.setFormatterForOutputFormatModeAndType("markdown", "overview", ConceptType.Property,         new ToplevelTopicsMarkdownFormatter());
		settings.setFormatterForOutputFormatModeAndType("markdown", "overview", ConceptType.MarkdownFolder,   new ToplevelTopicsMarkdownFormatter());
		settings.setFormatterForOutputFormatModeAndType("markdown", "overview", ConceptType.RestEndpoint,     new ToplevelTopicsMarkdownFormatter());
		settings.setFormatterForOutputFormatModeAndType("markdown", "overview", ConceptType.RequestParameter, new ToplevelTopicsMarkdownFormatter());
		settings.setFormatterForOutputFormatModeAndType("markdown", "overview", ConceptType.RequestHeader,    new ToplevelTopicsMarkdownFormatter());
		settings.setFormatterForOutputFormatModeAndType("markdown", "overview", ConceptType.MarkdownFile,     new MarkdownMarkdownFileFormatter(baseResource));
		settings.setFormatterForOutputFormatModeAndType("markdown", "overview", ConceptType.MarkdownTopic,    new ToplevelTopicsMarkdownFormatter());
		settings.setFormatterForOutputFormatModeAndType("markdown", "overview", ConceptType.CodeSource,       new MarkdownCodeSourceFormatter());
		settings.setFormatterForOutputFormatModeAndType("markdown", "overview", ConceptType.Table,            new MarkdownTableFormatter());
		settings.setFormatterForOutputFormatModeAndType("markdown", "overview", ConceptType.Glossary,         new MarkdownGlossaryFormatter());

		// wildcard
		//settings.setFormatterForOutputFormatModeAndType("markdown", "overview", ConceptType.Unknown,        new ToplevelTopicsMarkdownFormatter());

		settings.setFormatterForOutputFormatModeAndType("text", "overview", ConceptType.Unknown,            new PlaintextTopicFormatter());
		settings.setFormatterForOutputFormatModeAndType("text", "overview", ConceptType.Topic,              new PlaintextTopicFormatter());
		settings.setFormatterForOutputFormatModeAndType("text", "overview", ConceptType.MarkdownFolder,     new PlaintextTopicFormatter());
		settings.setFormatterForOutputFormatModeAndType("text", "overview", ConceptType.MarkdownFile,       new PlaintextMarkdownFileFormatter(baseResource));

		settings.setFormatterForOutputFormatModeAndType("json", "overview", ConceptType.Unknown,            new JsonConceptFormatter());

		// table of contents for inline documentation
		settings.setFormatterForOutputFormatModeAndType("toc", "overview", ConceptType.MarkdownFile,       new TableOfContentsMarkdownFileFormatter());
		settings.setFormatterForOutputFormatModeAndType("toc", "overview", ConceptType.Unknown,            new TableOfContentsConceptFormatter());

		// raw output of single attributes or content
		settings.setFormatterForOutputFormatModeAndType("raw", "overview", ConceptType.MarkdownFile,       new RawMarkdownFileFormatter(baseResource));
		settings.setFormatterForOutputFormatModeAndType("raw", "overview", ConceptType.Unknown,            new RawConceptFormatter());

		return settings;
	}

	private boolean isSearch(final HttpServletRequest request) {
		return request.getParameter("search") != null;
	}

	private void replaceIncludes(final Ontology ontology, final List<String> lines, final OutputSettings settings) {

		final Pattern pattern = Pattern.compile("\\{\\{(.*?)\\}\\}");
		int row = 0;


		String content        = StringUtils.join(lines, "\n");
		final Matcher matcher = pattern.matcher(content);
		int replacements      = 0;

		while (matcher.find() && replacements++ < 100) {

			final Map<String, String> data = parseIncludeLink("markdown output", row, matcher.group(1));
			final List<Concept> concepts   = new LinkedList<>();
			final String conceptName       = data.get("concept");

			if (data.containsKey("type")) {

				final ConceptType type = Concept.forName(data.get("type"));
				concepts.addAll(ontology.getConcept(type, conceptName));

			} else {

				concepts.addAll(ontology.getConcept(ConceptType.Topic, conceptName));
			}

			if (!concepts.isEmpty()) {

				final List<String> list = new LinkedList<>();
				final Concept concept   = concepts.get(0);
				int levelOffset         = 1;

				if (data.containsKey("h1")) { list.add("# " + concept.getName()); }
				if (data.containsKey("h2")) { list.add("## " + concept.getName()); }
				if (data.containsKey("h3")) { list.add("### " + concept.getName()); }
				if (data.containsKey("h4")) { list.add("#### " + concept.getName()); }
				if (data.containsKey("h5")) { list.add("##### " + concept.getName()); }
				if (data.containsKey("h6")) { list.add("###### " + concept.getName()); }

				if (data.containsKey("+1")) { levelOffset = 1; }
				if (data.containsKey("+2")) { levelOffset = 2; }
				if (data.containsKey("+3")) { levelOffset = 3; }
				if (data.containsKey("+4")) { levelOffset = 4; }
				if (data.containsKey("+5")) { levelOffset = 5; }
				if (data.containsKey("+6")) { levelOffset = 6; }

				if (data.containsKey("shortDescription")) {
					list.addAll(split(concept.getShortDescription()));
				}

				if (data.containsKey("table")) {

					list.add("");

					final OutputSettings tableSettings = OutputSettings.withDetails(ontology, Details.children);
					new MarkdownTableFormatter().format(list, new Link(null, null, concept), tableSettings, 0, new LinkedHashSet<>());
				}

				if (data.containsKey("list")) {

					list.add("");

					final OutputSettings listSettings = OutputSettings.withDetails(ontology, Details.children);
					new MarkdownListFormatter().format(list, new Link(null, null, concept), listSettings, 0, new LinkedHashSet<>());
				}

				if (data.containsKey("children")) {

					final OutputSettings topicSettings = OutputSettings.withDetails(ontology, Details.all);

					topicSettings.setRenderComments(false);
					topicSettings.setLevelOffset(levelOffset);
					topicSettings.setFormatterForOutputFormatModeAndType("markdown", "overview", ConceptType.MarkdownTopic, new MarkdownIncludeFormatter());

					// walk ontology
					Formatter.walkOntology(list, new Link(null, null, concept), topicSettings, 0, new LinkedHashSet<>());

					adaptHeadings(list, levelOffset);
				}

				final String insertText = StringUtils.join(list, "\n");

				content = matcher.replaceFirst(insertText);
				matcher.reset(content);
			}
		}

		lines.clear();
		lines.addAll(split(content));
	}

	/**
	 * A include link is a string of text that contains the name of the concept
	 * and a comma-separated list of keywords that control the rendering, like
	 * list, table, h1, h2, shortDescription etc.
	 * @param sourceFile
	 * @param row
	 * @param source
	 * @return
	 */
	private Map<String, String> parseIncludeLink(final String sourceFile, final int row, final String source) {

		final List<Token> tokens         = new FactsTokenizer().tokenize(null, source);
		final Map<String, String> result = new LinkedHashMap<>();

		while (!tokens.isEmpty()) {

			final Token token         = getNextToken(tokens);
			final String tokenContent = token.getContent();

			// remove empty tokens and commas
			if (StringUtils.isBlank(tokenContent) || tokenContent.trim().equals(","	)) {
				continue;
			}

			// first token is the concept to be included
			if (!result.containsKey("concept")) {

				final String content = token.getContent();

				if (content.contains(":")) {

					final String[] parts = content.split(":");
					final String type = parts[0];
					final String name = parts[1];

					result.put("type", type);
					result.put("concept", name);

				} else {

					result.put("concept", token.getContent());
				}

			} else {

				result.put(tokenContent, token.getContent());
			}
		}

		return result;
	}

	private List<String> split(final String input) {

		final List<String> result = new LinkedList<>();

		if (input != null) {

			result.addAll(Arrays.asList(input.split("\n")));
		}

		return result;
	}

	private Token getNextToken(final List<Token> tokens) {

		Token token = tokens.remove(0);

		while (StringUtils.isBlank(token.getContent()) && !tokens.isEmpty()) {

			token = tokens.remove(0);
		}

		return token;
	}

	private void adaptHeadings(final List<String> lines, final int offset) {

		final List<String> result = new LinkedList<>();
		boolean hasNonBlankLines     = false;

		for (final String line : lines) {

			if (StringUtils.isBlank(line) && !hasNonBlankLines) {

				// ignore leading blank lines so we can include content directly in a paragraph.

			} else {

				hasNonBlankLines = true;

				if (line.startsWith("#")) {

					result.add(StringUtils.repeat("#", offset) + line);

				} else {

					result.add(line);
				}
			}
		}

		lines.clear();
		lines.addAll(result);
	}
}