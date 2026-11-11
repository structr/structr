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

import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.extraction.api.ContentExtraction;
import org.structr.extraction.api.ContentExtractionException;
import org.structr.extraction.api.document.OcrInfo;
import org.structr.extraction.api.options.EffectiveContentOptions;
import org.structr.extraction.api.processing.AutoExtractionDecision;
import org.structr.extraction.function.ContentExtractionFunction;
import org.structr.extraction.traits.properties.ContentFileStateProperties;
import org.structr.schema.action.ActionContext;
import org.structr.extraction.function.ResultMaps;
import org.structr.web.entity.File;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExtractContentInfoFunction extends ContentExtractionFunction {

	@Override
	public String getName() {

		return NAMESPACE + ".extractContentInfo";
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("file");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		final File file                        = fileArgument(sources, 0);
		final Traits traits                    = Traits.of(StructrTraits.FILE);
		final AutoExtractionDecision decision  = ContentExtraction.shouldExtract(file);
		final Map<String, Object> info         = new LinkedHashMap<>();

		info.put("extract",       decision.extract());
		info.put("source",        decision.source());
		info.put("configuration", decision.configurationId());
		info.put("target",        decision.targetPath());
		info.put("status",        file.getProperty(traits.key(ContentFileStateProperties.STATUS)));
		info.put("error",         file.getProperty(traits.key(ContentFileStateProperties.ERROR)));
		info.put("date",          file.getProperty(traits.key(ContentFileStateProperties.DATE)));
		info.put("chunkCount",    ContentExtraction.getExtractedContent(file).size());
		info.put("figureCount",   Iterables.count(file.getProperty(traits.key(ContentFileStateProperties.FIGURES))));
		info.put("ocrUsed",       file.getProperty(traits.key(ContentFileStateProperties.OCR)));

		try {

			final OcrInfo ocr = ContentExtraction.getOcrInfo();

			info.put("ocrAvailable", ocr.available());
			info.put("ocrLanguages", ocr.languages().stream().sorted().toList());

		} catch (final ContentExtractionException ex) {

			info.put("ocrAvailable", false);
			info.put("ocrLanguages", List.of());
		}

		try {

			final EffectiveContentOptions options = ContentExtraction.resolveOptions(file, Map.of());

			info.put("options",  options.toMap());
			info.put("warnings", options.warnings());

		} catch (final IllegalArgumentException ex) {

			info.put("options",  null);
			info.put("warnings", List.of(ex.getMessage()));
		}

		return ResultMaps.toGraphObjectMap(info);
	}

	@Override
	public String getShortDescription() {

		return "Explains whether and how a file is extracted, and its current extraction state.";
	}

	@Override
	public String getLongDescription() {

		return """
			Returns an object with:
			- `extract`, `source`, `configuration` and `target`: whether the file is extracted automatically, and why: the nearest \
			`ContentExtractionConfiguration` decides, attached to the file itself (`file`) or to one of its folders (`folder`); `configuration` \
			is its UUID and `target` the path it is attached to. Without one, the file is not extracted (`default`).
			- `status`, `error`, `date`, `chunkCount` and `ocrUsed`: the state of the last extraction; `figureCount`: the number of stored \
			figures of the document (`DocumentFigure`s).
			- `ocrAvailable` and `ocrLanguages`: whether Tesseract is installed, with which languages.
			- `options`: every option the file would be processed with, as `{ value, source }`, where source is `setting`, `config:<path>` or \
			`call`; and `warnings`, e.g. about an overlap that had to be reduced.""";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(Parameter.mandatory("file", "the File"));
	}

	@Override
	public List<Example> getExamples() {

		return List.of(Example.structrScript("${ documents.extractContentInfo(first(find('File', 'name', 'manual.pdf'))).options.chunkSize.source }", "Where the chunk size of a file comes from"));
	}
}
