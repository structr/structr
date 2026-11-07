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
package org.structr.extraction.function.graph;

import org.structr.common.error.FrameworkException;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.extraction.api.ContentExtraction;
import org.structr.extraction.function.ContentExtractionFunction;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class GetExtractedTextFunction extends ContentExtractionFunction {

	@Override
	public String getName() {

		return NAMESPACE + ".getExtractedText";
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("file");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		return ContentExtraction.getExtractedText(fileArgument(sources, 0));
	}

	@Override
	public String getShortDescription() {

		return "Returns the stored text of a file, reassembled from its chunks.";
	}

	@Override
	public String getLongDescription() {

		return """
			Joins the stored chunks of a file (`file.extractedContentNodes`) into one text, including overlapping parts once, without extracting \
			the file again. Returns an empty text if the file has not been processed.""";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(Parameter.mandatory("file", "the File"));
	}

	@Override
	public List<Example> getExamples() {

		return List.of(Example.structrScript("${ documents.getExtractedText(first(find('File', 'name', 'manual.pdf'))) }", "The stored text of a file"));
	}
}
