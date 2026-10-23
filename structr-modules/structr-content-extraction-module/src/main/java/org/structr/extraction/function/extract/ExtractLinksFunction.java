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
package org.structr.extraction.function.extract;

import org.structr.common.error.FrameworkException;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.extraction.api.ContentExtraction;
import org.structr.extraction.api.ContentExtractionException;
import org.structr.extraction.api.document.ExtractedLink;
import org.structr.extraction.function.ContentExtractionFunction;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.File;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExtractLinksFunction extends ContentExtractionFunction {

	private static final Set<String> ACCEPTED = Set.of(OPTION_MAX_CHARACTERS, OPTION_INCLUDE_EMBEDDED);

	@Override
	public String getName() {

		return NAMESPACE + ".extractLinks";
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("file [, options ]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		final File file                   = fileArgument(sources, 0);
		final Map<String, Object> options = optionsArgument(sources, 1, ACCEPTED);

		try {

			final List<ExtractedLink> links = ContentExtraction.extractLinks(file, extractionOptions(options));

			return toGraphObjectMaps(links.stream().map(ExtractedLink::toMap).toList());

		} catch (final ContentExtractionException ex) {

			throw extractionFailed(ex);
		}
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(Usage.structrScript("Usage: ${ documents.extractLinks(file [, options ]) }"), Usage.javaScript("Usage: ${{ $.documents.extractLinks(file [, options ]); }}"));
	}

	@Override
	public String getShortDescription() {

		return "Extracts the hyperlinks of a file, with their anchor text.";
	}

	@Override
	public String getLongDescription() {

		return """
			Returns the hyperlinks of a file as a list of objects with `url` (the target as written in the document), `text` (the anchor text) and \
			`page`. Useful for connecting documents in the graph, for example linking a File to the documents it references.""";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("file", "the File to read"),
			Parameter.optional("options", "an object with any of: `maxCharacters` (text beyond is dropped) and `includeEmbedded` (also read links of embedded documents)")
		);
	}

	@Override
	public List<Example> getExamples() {

		return List.of(Example.structrScript("${ extract(documents.extractLinks(first(find('File', 'name', 'index.html'))), 'url') }", "All link targets of an HTML file"));
	}
}
