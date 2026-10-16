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
import org.structr.extraction.function.ContentExtractionFunction;
import org.structr.schema.action.ActionContext;
import org.structr.extraction.function.ResultMaps;
import org.structr.web.entity.File;

import java.util.List;

public class ExtractMetadataFunction extends ContentExtractionFunction {

	@Override
	public String getName() {

		return NAMESPACE + ".extractMetadata";
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("file");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		final File file = fileArgument(sources, 0);

		if (sources.length > 1) {

			throw new FrameworkException(422, getName() + "(): expects exactly one argument, the File.");
		}

		try {

			return ResultMaps.toGraphObjectMap(ContentExtraction.extractMetadata(file).toMap());

		} catch (final ContentExtractionException ex) {

			throw extractionFailed(ex);
		}
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(Usage.structrScript("Usage: ${ documents.extractMetadata(file) }"), Usage.javaScript("Usage: ${{ $.documents.extractMetadata(file); }}"));
	}

	@Override
	public String getShortDescription() {

		return "Reads the metadata of a file, such as title, author, dates and page count.";
	}

	@Override
	public String getLongDescription() {

		return """
			Returns the metadata of a file: `mimeType`, `title`, `author`, `language` (as declared by the document), `pageCount`, `created` and `modified`, \
			plus `raw`, an object with everything the parser reported. The text of the file is not kept and no OCR runs, which makes this function \
			suitable for checking files before extracting them.""";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(Parameter.mandatory("file", "the File to read"));
	}

	@Override
	public List<Example> getExamples() {

		return List.of(Example.structrScript("${ documents.extractMetadata(first(find('File', 'name', 'report.docx'))).author }", "The author of a document"));
	}
}
