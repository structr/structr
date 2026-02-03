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
package org.structr.docs.ontology.parser.token;

import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.IndentedCodeBlock;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.profile.pegdown.Extensions;
import com.vladsch.flexmark.profile.pegdown.PegdownOptionsAdapter;
import com.vladsch.flexmark.util.ast.Block;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.util.resource.Resource;
import org.structr.core.function.tokenizer.Token;
import org.structr.docs.formatter.markdown.MarkdownMarkdownFileFormatter;
import org.structr.docs.ontology.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An identifier that is augmented with a type so we know what it is.
 */
public class MarkdownFileToken extends NamedConceptToken {

	public MarkdownFileToken(final ConceptToken conceptToken, final IdentifierToken identifierToken) {

		super(conceptToken, identifierToken);
	}

	public boolean isUnknown() {
		return "unknown".equals(conceptToken.getToken());
	}

	public void addAdditionalNamedConcept(final NamedConceptToken conceptToken) {

	}

	@Override
	public AnnotatedConcept resolve(final Ontology ontology) {

		final Resource baseResource = ontology.getBaseResource();
		final String path           = identifierToken.resolve(ontology);

		// baseResource can be null in the tests
		if (baseResource != null) {

			final Resource docsResource = baseResource.resolve("docs");
			final Path folderPath       = docsResource.resolve(path).getPath();
			final String fileName       = StringUtils.substringAfterLast(path, "/");
			final String cleanedName    = cleanName(StringUtils.substringBeforeLast(fileName, "."));

			try {

				// handle children
				final List<String> lines = Files.readAllLines(folderPath);
				final MutableDataSet options = new MutableDataSet();

				options.setAll(PegdownOptionsAdapter.flexmarkOptions(false, Extensions.ALL));

				final Parser parser    = Parser.builder(options).build();
				final Document doc     = parser.parse(StringUtils.join(lines, "\n"));
				final TreeItem root    = new TreeItem(null, cleanedName, 0);

				// the current tree item, will be changed during evaluation
				TreeItem current = root;

				for (final Node child : doc.getChildren()) {

					current = handleNode(child, current);
				}

				// resolve content and put it into the ontology
				final Concept concept = root.resolve(ontology);
				if (concept != null) {

					return new AnnotatedConcept(concept);
				}

			} catch (IOException ioex) {
				ioex.printStackTrace();
			}
		}

		return null;
	}

	@Override
	public void updateContent(final String key, final String value) {

		if ("content".equals(key)) {

			final String path   = identifierToken.getToken().getContent();
			final Path filePath = Path.of("structr/docs/" + path);

			if (Files.exists(filePath)) {

				// store markdown content in file
				try (final BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile()))) {

					writer.write(value);
					writer.flush();
				} catch (IOException ioex) {
					ioex.printStackTrace();
				}
			}

		} else {

			throw new UnsupportedOperationException("Cannot update " + key + " of " + this);
		}
	}

	// ----- private methods -----
	private Map<String, String> getMetadata(final Ontology ontology) {

		final Map<String, String> metadata = new LinkedHashMap<>();

		// additional named concepts go into metadata of a concept (for now..)
		for (final NamedConceptToken additionalNamedConcept : additionalNamedConcepts) {

			final AnnotatedConcept additionalAnnotatedConcept = additionalNamedConcept.resolve(ontology);
			final Concept additionalConcept                   = additionalAnnotatedConcept.getConcept();

			metadata.put(additionalConcept.getType().getIdentifier(), additionalConcept.getName());
		}

		return metadata;
	}

	private String coalesce(final String... strings) {

		for (final String string : strings) {

			if (StringUtils.isNotBlank(string)) {

				return string;
			}
		}

		return null;
	}

	private String trimNewline(final String input) {

		if (input != null && input.endsWith("\n")) {

			return input.substring(0, input.length() - 1);
		}

		return input;
	}

	private String cleanName(final String name) {

		final Pattern pattern = Pattern.compile("^[0-9]+\\-(.*?)(\\.md)?");
		final Matcher matcher = pattern.matcher(name);

		if (matcher.matches()) {

			return matcher.group(1);
		}

		return name;
	}

	private TreeItem handleNode(final Node node, final TreeItem current) {

		if (node instanceof Heading newHeading) {

			final int newHeadingLevel     = newHeading.getLevel() - 1;
			final int currentHeadingLevel = current.getLevel();
			final String name             = newHeading.getText().unescape();

			if (newHeadingLevel > currentHeadingLevel) {

				// another level => add as child
				final TreeItem newChild = new TreeItem(current, name, newHeadingLevel);
				current.addChild(newChild);

				return newChild;
			}

			if (newHeadingLevel == currentHeadingLevel) {

				// same level => add new child to parent
				final TreeItem sameParent = current.getParent();
				if (sameParent != null) {

					final TreeItem newChild = new TreeItem(sameParent, name, newHeadingLevel);
					sameParent.addChild(newChild);

					return newChild;

				} else {

					// No common parent => this heading is likely a
					// toplevel headline or the document title.
					current.setTitle(name);
				}
			}

			if (newHeadingLevel < currentHeadingLevel) {

				// new higher level heading
				TreeItem parent = current.getParent();

				if (parent != null) {

					while (parent != null && parent.getLevel() >= newHeadingLevel) {
						parent = parent.getParent();
					}

					if (parent != null) {

						final TreeItem newChild = new TreeItem(parent, name, newHeadingLevel);

						parent.addChild(newChild);

						return newChild;

					} else {

						throw new IllegalStateException("Parent was null.");
					}
				}
			}

		} else if (node instanceof IndentedCodeBlock codeBlock) {

			current.addParagraph("");

			for (final BasedSequence line : codeBlock.getContentLines()) {

				current.addParagraph("    " + trimNewline(line.unescape()));
			}

			// current is unchanged
			return current;

		} else if (node instanceof Block paragraph) {

			current.addParagraph("");

			for (final String line : paragraph.getChars().unescape().split("\n")) {

				current.addParagraph(line);
			}

			// current is unchanged
			return current;

		} else {

			System.out.println(node.getNodeName() + ": " + node.getClass().getSimpleName());
		}

		// no change in hierarchy => return same parent
		return current;
	}


	private class TreeItem extends AbstractToken<Concept> {

		private final List<TreeItem> children = new LinkedList<>();
		private final List<String> paragraphs = new LinkedList<>();
		private final TreeItem parent;
		private final int level;
		private final String name;
		private String title = null;

		public TreeItem(final TreeItem parent, final String name, final int level) {

			this.parent = parent;
			this.name   = name;
			this.level  = level;
		}

		@Override
		public String toString() {

			final StringBuilder builder = new StringBuilder();

			builder.append(StringUtils.repeat(" ", level * 4) + name);

			for (final String paragraph : paragraphs) {

				builder.append(StringUtils.repeat(" ", level * 4) + paragraph);
			}

			builder.append("\n");

			for (final TreeItem child : children) {

				builder.append(child.toString());
				builder.append("\n");
			}

			return builder.toString();
		}

		public TreeItem getParent() {
			return parent;
		}

		public void addChild(final TreeItem child) {
			children.add(child);
		}

		public int getLevel() {
			return level;
		}

		public void addParagraph(final String text) {
			paragraphs.add(text);
		}

		public List<TreeItem> getChildren() {
			return children;
		}

		// ----- AbstractToken -----
		@Override
		public Concept resolve(final Ontology ontology) {

			final Concept concept = ontology.getOrCreateConcept(this, ConceptType.MarkdownTopic, name, false);
			if (concept != null) {

				concept.setShortDescription(StringUtils.join(paragraphs, "\n"));

				if (title != null) {
					concept.getMetadata().put("title", title);
				}

				for (final TreeItem child : children) {

					final Concept childConcept = child.resolve(ontology);
					if (childConcept != null) {

						ontology.createSymmetricLink(concept, Verb.Has, childConcept);
					}
				}
			}

			return concept;
		}

		@Override
		public boolean isTerminal() {
			return false;
		}

		@Override
		public Token getToken() {
			return null;
		}

		@Override
		public void renameTo(final String newName) {
			throw new UnsupportedOperationException("Not supported");
		}

		@Override
		public void updateContent(final String key, final String value) {
			throw new UnsupportedOperationException("Not supported");
		}

		public void setTitle(final String title) {
			this.title = title;
		}

		public String getTitle() {
			return title;
		}
	}
}
