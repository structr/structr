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
package org.structr.extraction.chunking;

import org.structr.extraction.api.chunk.Chunk;
import org.structr.extraction.api.document.DocumentBlock;
import org.structr.extraction.api.document.ExtractedDocument;
import org.structr.extraction.api.options.SizeUnit;
import org.structr.extraction.api.options.SplitOptions;
import org.structr.extraction.api.options.SplitterType;
import org.structr.extraction.chunking.token.TokenCounter;
import org.structr.extraction.parser.DocumentTextRenderer;
import org.structr.extraction.parser.markdown.MarkdownBlockParser;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Entry point for splitting documents and texts into chunks.
 */
public final class ContentChunker {

	private ContentChunker() {
	}

	/**
	 * Splits an extracted document. Chunk offsets refer to {@link ExtractedDocument#getText()}.
	 */
	public static List<Chunk> split(final ExtractedDocument document, final SplitOptions options) {

		options.validate();

		final String language = document.getMetadata().language();
		final Locale locale   = language != null ? Locale.forLanguageTag(language) : Locale.ROOT;

		return split(document.getText(), document.getBlocks(), options, locale);
	}

	/**
	 * Splits a text. Chunk offsets refer to the text, except for the structure splitter, which reads the
	 * text as Markdown and splits the result; its offsets refer to that document's plain text, as
	 * returned by {@link #markdownDocumentText(String)}.
	 */
	public static List<Chunk> split(final String text, final SplitOptions options) {

		options.validate();

		if (options.getSplitter() == SplitterType.STRUCTURE) {

			final DocumentTextRenderer renderer = renderMarkdown(text);

			return split(renderer.getText(), renderer.getBlocks(), options, Locale.ROOT);
		}

		return split(text, List.of(), options, Locale.ROOT);
	}

	/**
	 * The plain text a Markdown text is turned into for structure splitting.
	 */
	public static String markdownDocumentText(final String text) {

		return renderMarkdown(text).getText();
	}

	// ----- private methods -----
	private static List<Chunk> split(final String text, final List<DocumentBlock> blocks, final SplitOptions options, final Locale locale) {

		final TokenCounter tokens         = TokenCounter.forEncoding(options.getTokenEncoding());
		final SizeUnit unit         = options.getSplitter() == SplitterType.TOKEN ? SizeUnit.TOKENS : options.getSizeUnit();
		final ChunkSizeMeasure measure       = new ChunkSizeMeasure(text, unit, tokens);
		final int size              = options.getChunkSize();
		final int overlap           = options.getChunkOverlap();
		final RecursiveTextSplitter recursive = new RecursiveTextSplitter(measure, size, overlap, separators(options), locale);
		final TextSpan all              = new TextSpan(0, text.length());
		final ChunkFactory factory  = new ChunkFactory(text, blocks, tokens, options.isIncludeHeadingPath());
		final List<TextSpan> spans = switch (options.getSplitter()) {

			case RECURSIVE -> recursive.split(all);

			case SENTENCE  -> new RecursiveTextSplitter(measure, size, overlap, RecursiveTextSplitter.SENTENCES, locale).split(all);
			case TOKEN     -> tokens.windows(text, size, overlap);
			case PAGE      -> pages(blocks, all, recursive);
			case STRUCTURE -> blocks.isEmpty() ? recursive.split(all) : new DocumentStructureSplitter(measure, recursive, size, overlap).split(blocks);
		};

		return factory.create(spans);
	}

	private static List<RecursiveTextSplitter.Separator> separators(final SplitOptions options) {

		return options.getSeparators() != null ? RecursiveTextSplitter.fromStrings(options.getSeparators()) : RecursiveTextSplitter.DEFAULT;
	}

	/**
	 * One chunk per page, pages larger than a chunk split recursively. Without pages, the whole text is
	 * split recursively.
	 */
	private static List<TextSpan> pages(final List<DocumentBlock> blocks, final TextSpan all, final RecursiveTextSplitter recursive) {

		final Map<Integer, TextSpan> pages = new LinkedHashMap<>();

		for (final DocumentBlock block : blocks) {

			if (block.page() != null) {

				pages.merge(block.page(), new TextSpan(block.startOffset(), block.endOffset()), (a, b) -> new TextSpan(a.start(), b.end()));
			}
		}

		if (pages.isEmpty()) {

			return recursive.split(all);
		}

		final List<TextSpan> chunks = new ArrayList<>();

		for (final TextSpan page : pages.values()) {

			chunks.addAll(recursive.split(page));
		}

		return chunks;
	}

	private static DocumentTextRenderer renderMarkdown(final String text) {

		return new DocumentTextRenderer(new MarkdownBlockParser(Integer.MAX_VALUE).parse(text));
	}
}
