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
import org.structr.core.graph.NodeInterface;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.extraction.ContentExtractionTraits;
import org.structr.extraction.api.ContentExtraction;
import org.structr.extraction.entity.ExtractedContentNode;
import org.structr.extraction.function.ContentExtractionFunction;
import org.structr.schema.action.ActionContext;
import org.structr.extraction.function.ResultMaps;

import java.util.List;

public class GetContentContextFunction extends ContentExtractionFunction {

	@Override
	public String getName() {

		return NAMESPACE + ".getContentContext";
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("extractedContentNode [, before [, after ]]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		if (sources == null || sources.length == 0 || !(sources[0] instanceof NodeInterface node) || !node.is(ContentExtractionTraits.EXTRACTED_CONTENT_NODE)) {

			throw new FrameworkException(422, getDisplayName(false) + ": expected an ExtractedContentNode as argument 1.");
		}

		final int before = count(sources, 1);
		final int after  = count(sources, 2);

		return ResultMaps.toGraphObjectMap(ContentExtraction.getContext(node.as(ExtractedContentNode.class), before, after).toMap());
	}

	@Override
	public String getShortDescription() {

		return "Returns a chunk of extracted content together with the chunks around it, as one text.";
	}

	@Override
	public String getLongDescription() {

		return """
			Returns an object with `text` (the chunk and its neighbours as one continuous text, with overlapping parts included once), `nodes` \
			(the chunks in reading order), `file`, `pageStart`, `pageEnd`, `headingPath` and `embeddedPath`. Neighbours are taken from the same \
			embedded document (archive entry, attachment) only. Useful for giving a language model more context than a single search hit.""";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(Parameter.mandatory("extractedContentNode", "the chunk"), Parameter.optional("before", "number of chunks before it, default 1"),
			Parameter.optional("after", "number of chunks after it, default 1"));
	}

	@Override
	public List<Example> getExamples() {

		return List.of(Example.javaScript("${{ $.documents.getContentContext($.documents.fulltextSearch('invoice address')[0].node, 2, 2).text; }}", "The best hit with two chunks of context on each side"));
	}

	// ----- private methods -----
	private int count(final Object[] sources, final int index) throws FrameworkException {

		if (sources.length <= index || sources[index] == null) {

			return 1;
		}

		if (sources[index] instanceof Number number && number.doubleValue() >= 0 && number.doubleValue() == Math.floor(number.doubleValue())) {

			return number.intValue();
		}

		throw new FrameworkException(422, getDisplayName(false) + ": argument " + (index + 1) + " must be a whole number of 0 or more.");
	}
}
