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
package org.structr.extraction.function.text;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.graph.NodeInterface;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.extraction.api.ContentExtraction;
import org.structr.extraction.api.ContentExtractionException;
import org.structr.extraction.api.chunk.Chunk;
import org.structr.extraction.api.options.SplitOptions;
import org.structr.extraction.chunking.ContentChunker;
import org.structr.extraction.function.ContentExtractionFunction;
import org.structr.schema.action.ActionContext;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SplitContentFunction extends ContentExtractionFunction {

	private static final Set<String> ACCEPTED_FOR_FILES = accepted();

	@Override
	public String getName() {

		return NAMESPACE + ".splitContent";
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("textOrFile [, options ]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		final boolean isFile = sources != null && sources.length > 0 && sources[0] instanceof NodeInterface;

		try {

			if (isFile) {

				final Map<String, Object> options = optionsArgument(sources, 1, ACCEPTED_FOR_FILES);
				final SplitOptions split          = splitOptions(options);

				return toMaps(ContentExtraction.split(ContentExtraction.extract(fileArgument(sources, 0), extractionOptions(options)), split));
			}

			final String text                 = textArgument(sources, 0);
			final Map<String, Object> options = optionsArgument(sources, 1, SPLIT_OPTIONS);

			return toMaps(ContentExtraction.split(text, splitOptions(options)));

		} catch (final ContentExtractionException ex) {

			throw extractionFailed(ex);

		} catch (final IllegalArgumentException ex) {

			throw new FrameworkException(422, getName() + "(): " + ex.getMessage());
		}
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(Usage.structrScript("Usage: ${ documents.splitContent(textOrFile [, options ]) }"), Usage.javaScript("Usage: ${{ $.documents.splitContent(textOrFile [, options ]); }}"));
	}

	@Override
	public String getShortDescription() {

		return "Splits a text or the content of a file into chunks for embedding and retrieval.";
	}

	@Override
	public String getLongDescription() {

		return """
			Splits a text, or the extracted content of a File, into chunks and returns them as a list of objects with `position`, `text`, `startOffset`, \
			`endOffset`, `charCount`, `tokenCount`, `pageStart`, `pageEnd`, `headingPath` and `contentHash`. Nothing is stored; the function is meant \
			for trying out split settings and for custom pipelines.

			ContentChunker:
			- `structure` (the default setting): follows the document structure. Headings of level 1 and 2 always start a new chunk, whole blocks \
			are packed into chunks, a heading stays with the block after it, and only blocks larger than a chunk are divided. A text is read as Markdown.
			- `recursive`: LangChain's recursive splitter. Divides at blank lines, then line breaks, sentences, words and finally anywhere, and packs \
			the pieces into chunks with overlap. The separators can be set with the option `separators`.
			- `sentence`: packs whole sentences into chunks.
			- `token`: fixed windows of `chunkSize` tokens, each `chunkSize - chunkOverlap` tokens after the previous one.
			- `page`: one chunk per page, larger pages are split recursively.

			Sizes are measured in tokens (OpenAI's tiktoken encodings, exact for OpenAI models and an approximation for others) or in characters.""";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(Parameter.mandatory("textOrFile", "a text, or a File whose content is extracted first"),
			Parameter.optional("options", """
				an object with any of: `splitter`, `chunkSize`, `chunkOverlap`, `sizeUnit` (`tokens` or `characters`), `separators` (a list of texts, \
				for the recursive splitter), `includeHeadingPath` (start each chunk with the path of its section), `tokenEncoding` (`cl100k_base`, \
				`o200k_base`, ...). For a File also the options of `documents.extractContent()`: `ocr`, `ocrLanguages`, `maxCharacters`, `includeEmbedded`. \
				Defaults come from the `contentextraction.split.*` settings."""));
	}

	@Override
	public List<Example> getExamples() {

		return List.of(Example.javaScript("""
				${{
				    let file   = $.first($.find('File', { name: 'manual.pdf' }));
				    let chunks = $.documents.splitContent(file, { chunkSize: 256, chunkOverlap: 32, includeHeadingPath: true });

				    // how large the chunks turned out
				    return chunks.map(c => c.tokenCount);
				}}
				""", "Try out a chunk size on a file"),
			Example.structrScript("${ size(documents.splitContent('A long text ...', { splitter: 'sentence', chunkSize: 200, sizeUnit: 'characters' })) }", "Number of chunks of a text"));
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"The overlap must be smaller than the chunk size.",
			"`startOffset` and `endOffset` refer to the extracted plain text of a File (`documents.extractContent(file).text`), or to the text that was passed in. The structure splitter reads a text as Markdown; its offsets then refer to the plain text of that Markdown, without the markup.",
			"With `includeHeadingPath`, the section path is added in front of the chunk text and counts towards `tokenCount`, but not towards the chunk size."
		);
	}

	// ----- private methods -----
	private static List<GraphObjectMap> toMaps(final List<Chunk> chunks) {

		return toGraphObjectMaps(chunks.stream().map(Chunk::toMap).toList());
	}

	private static Set<String> accepted() {

		final Set<String> accepted = new HashSet<>(SPLIT_OPTIONS);

		accepted.addAll(EXTRACTION_OPTIONS);

		return Set.copyOf(accepted);
	}
}
