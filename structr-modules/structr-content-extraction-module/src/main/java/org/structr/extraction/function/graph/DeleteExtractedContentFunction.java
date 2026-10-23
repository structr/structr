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

import org.structr.common.Permission;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.extraction.api.ContentExtraction;
import org.structr.extraction.entity.ExtractedContentNode;
import org.structr.extraction.function.ContentExtractionFunction;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.File;

import java.util.List;

public class DeleteExtractedContentFunction extends ContentExtractionFunction {

	@Override
	public String getName() {

		return NAMESPACE + ".deleteExtractedContent";
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("file");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		final File file = fileArgument(sources, 0);
		if (!file.isGranted(Permission.write, ctx.getSecurityContext())) {

			throw new FrameworkException(403, getName() + "(): deleting the content of " + file.getPath() + " requires write permission on the file.");
		}

		ContentExtraction.deleteExtractedContent(file);

		return null;
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(Usage.structrScript("Usage: ${ documents.deleteExtractedContent(file) }"), Usage.javaScript("Usage: ${{ $.documents.deleteExtractedContent(file); }}"));
	}

	@Override
	public String getShortDescription() {

		return "Deletes the stored chunks of a file and resets its extraction state.";
	}

	@Override
	public String getLongDescription() {

		return """
			Deletes all `ExtractedContentNode`s of a file and clears `contentExtractionStatus` and the related properties. The file itself is not \
			changed. If the file is marked for automatic extraction, it is extracted again the next time its content changes.""";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(Parameter.mandatory("file", "the File"));
	}

	@Override
	public List<Example> getExamples() {

		return List.of(Example.structrScript("${ documents.deleteExtractedContent(first(find('File', 'name', 'draft.docx'))) }", "Remove the chunks of a file"));
	}

	@Override
	public List<String> getNotes() {

		return List.of("Requires write permission on the file.");
	}
}
