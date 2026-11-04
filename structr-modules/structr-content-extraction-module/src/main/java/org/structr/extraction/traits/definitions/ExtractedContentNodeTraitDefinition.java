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
package org.structr.extraction.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.api.JavaMethod;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.ArrayProperty;
import org.structr.core.property.EndNode;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.extraction.ContentExtractionTraits;
import org.structr.extraction.api.ContentExtraction;
import org.structr.extraction.entity.ExtractedContentNode;
import org.structr.extraction.query.ContentFileSearch;
import org.structr.extraction.traits.wrappers.ExtractedContentNodeTraitWrapper;
import org.structr.schema.action.ActionContext;

import java.util.Map;
import java.util.Set;

/**
 * A chunk of a file's content: its text, where it is in the document, and (written by the Enterprise AI
 * module) its embedding. The embedding is vector-indexed by Structr's index management as
 * ExtractedContentNode_embedding_vector, with the size ContentExtraction.embeddingDimensions() returns.
 */
public class ExtractedContentNodeTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String FILE_PROPERTY            = "file";
	public static final String NEXT_PROPERTY            = "next";
	public static final String PREVIOUS_PROPERTY        = "previous";
	public static final String TEXT_PROPERTY            = "text";
	public static final String POSITION_PROPERTY        = "position";
	public static final String START_OFFSET_PROPERTY    = "startOffset";
	public static final String END_OFFSET_PROPERTY      = "endOffset";
	public static final String CHAR_COUNT_PROPERTY      = "charCount";
	public static final String TOKEN_COUNT_PROPERTY     = "tokenCount";
	public static final String PAGE_START_PROPERTY      = "pageStart";
	public static final String PAGE_END_PROPERTY        = "pageEnd";
	public static final String HEADING_PATH_PROPERTY    = "headingPath";
	public static final String EMBEDDED_PATH_PROPERTY   = "embeddedPath";
	public static final String LANGUAGE_PROPERTY        = "language";
	public static final String CONTENT_HASH_PROPERTY    = "contentHash";
	public static final String EMBEDDING_PROPERTY       = "embedding";
	public static final String EMBEDDING_MODEL_PROPERTY = "embeddingModel";
	public static final String FIGURE_PROPERTY          = "figure";

	public ExtractedContentNodeTraitDefinition() {

		super(ContentExtractionTraits.EXTRACTED_CONTENT_NODE);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(ExtractedContentNode.class, (traits, node) -> new ExtractedContentNodeTraitWrapper(traits, node));
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {

		return newSet(

			new JavaMethod("searchFiles", false, true) {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {

					return ContentFileSearch.search(actionContext.getSecurityContext(), arguments.get("query"), arguments.get("folder"), arguments.get("limit"), arguments.get("maxSnippets"));
				}

				@Override
				public String getDescription() {

					return "Searches files by their extracted content like a search box: every word must occur, as a prefix or with small typos, ignoring case. Returns the best files with snippets around the matches.";
				}
			}
		);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(final TraitsInstance traitsInstance) {

		final Property<NodeInterface> file     = new StartNode(traitsInstance, FILE_PROPERTY, ContentExtractionTraits.FILE_HAS_EXTRACTED_CONTENT);
		final Property<NodeInterface> next     = new EndNode(traitsInstance, NEXT_PROPERTY, ContentExtractionTraits.EXTRACTED_CONTENT_NEXT);
		final Property<NodeInterface> previous = new StartNode(traitsInstance, PREVIOUS_PROPERTY, ContentExtractionTraits.EXTRACTED_CONTENT_NEXT);
		// text and embedded path share one fulltext index, so a search finds a chunk by the name of the
		// archive entry or attachment it is in, too
		final Property<String> text            = new StringProperty(TEXT_PROPERTY).fulltextIndexed(ContentExtractionTraits.CONTENT_FULLTEXT_INDEX_NAME);
		final Property<Integer> position       = new IntProperty(POSITION_PROPERTY).indexed();
		final Property<Integer> startOffset    = new IntProperty(START_OFFSET_PROPERTY);
		final Property<Integer> endOffset      = new IntProperty(END_OFFSET_PROPERTY);
		final Property<Integer> charCount      = new IntProperty(CHAR_COUNT_PROPERTY);
		final Property<Integer> tokenCount     = new IntProperty(TOKEN_COUNT_PROPERTY);
		final Property<Integer> pageStart      = new IntProperty(PAGE_START_PROPERTY);
		final Property<Integer> pageEnd        = new IntProperty(PAGE_END_PROPERTY);
		final Property<String[]> headingPath   = new ArrayProperty<>(HEADING_PATH_PROPERTY, String.class);
		final Property<String> embeddedPath    = new StringProperty(EMBEDDED_PATH_PROPERTY).indexed().fulltextIndexed(ContentExtractionTraits.CONTENT_FULLTEXT_INDEX_NAME);
		final Property<String> language        = new StringProperty(LANGUAGE_PROPERTY);
		final Property<String> contentHash     = new StringProperty(CONTENT_HASH_PROPERTY).indexed();

		// a vector of e.g. 1536 doubles is 12 KB per chunk, so it is not serialized unless asked for
		final Property<Double[]> embedding     = new ArrayProperty<>(EMBEDDING_PROPERTY, Double.class).disableSerialization(true).vectorIndexed(ContentExtraction::embeddingDimensions, "cosine");
		final Property<String> embeddingModel  = new StringProperty(EMBEDDING_MODEL_PROPERTY);

		// set for the chunk with the text of a DocumentFigure, which is not part of the text in reading order
		final Property<NodeInterface> figure   = new StartNode(traitsInstance, FIGURE_PROPERTY, ContentExtractionTraits.FIGURE_CONTENT);

		return newSet(file, next, previous, text, position, startOffset, endOffset, charCount, tokenCount, pageStart, pageEnd, headingPath, embeddedPath, language, contentHash, embedding, embeddingModel, figure);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public, newSet(TEXT_PROPERTY, POSITION_PROPERTY, PAGE_START_PROPERTY, PAGE_END_PROPERTY, HEADING_PATH_PROPERTY, EMBEDDED_PATH_PROPERTY, FILE_PROPERTY, FIGURE_PROPERTY),
			PropertyView.Ui, newSet(FILE_PROPERTY, NEXT_PROPERTY, PREVIOUS_PROPERTY, TEXT_PROPERTY, POSITION_PROPERTY, START_OFFSET_PROPERTY, END_OFFSET_PROPERTY, CHAR_COUNT_PROPERTY,
				TOKEN_COUNT_PROPERTY, PAGE_START_PROPERTY, PAGE_END_PROPERTY, HEADING_PATH_PROPERTY, EMBEDDED_PATH_PROPERTY, LANGUAGE_PROPERTY, CONTENT_HASH_PROPERTY, EMBEDDING_MODEL_PROPERTY, FIGURE_PROPERTY)
		);
	}

	@Override
	public Relation getRelation() {

		return null;
	}
}
