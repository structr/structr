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
import org.structr.extraction.api.ContentExtraction;
import org.structr.extraction.function.ContentExtractionFunction;
import org.structr.schema.action.ActionContext;
import org.structr.extraction.function.ResultMaps;

import java.util.List;

public class DetectLanguageFunction extends ContentExtractionFunction {

	@Override
	public String getName() {

		return NAMESPACE + ".detectLanguage";
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("text");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		return ResultMaps.toGraphObjectMap(ContentExtraction.detectLanguage(textArgument(sources, 0)).toMap());
	}

	@Override
	public String getShortDescription() {

		return "Detects the language of a text.";
	}

	@Override
	public String getLongDescription() {

		return """
			Returns an object with `language` (the ISO 639-1 code of the most likely language, e.g. `de`), `confidence` (between 0 and 1) and \
			`alternatives`, all candidates with their confidence. For texts shorter than 20 characters, `language` is null: the result would be \
			a guess. About 70 languages are recognized.""";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(Parameter.mandatory("text", "the text"));
	}

	@Override
	public List<Example> getExamples() {

		return List.of(Example.structrScript("${ documents.detectLanguage('Dies ist ein deutscher Satz über Graphdatenbanken.').language }", "Returns \"de\""));
	}
}
