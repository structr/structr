/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.function;

import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SearchVectorFunction extends CoreFunction implements QueryFunction {

	private static final int DEFAULT_LIMIT = 10;
	private static final int MAX_LIMIT     = 1000;

	@Override
	public String getName() {

		return "searchVector";
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("indexName, vector [, limit ]");
	}

	@Override
	public String getNamespaceIdentifier() {

		return "searchVector";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		if (sources == null || sources.length < 2 || sources.length > 3 || !(sources[0] instanceof String indexName) || StringUtils.isBlank(indexName)) {

			throw new FrameworkException(422, getName() + "(): expected an index name, a vector and optionally a limit.");
		}

		final double[] vector = vector(sources[1]);
		int limit             = DEFAULT_LIMIT;

		if (sources.length == 3 && sources[2] != null) {

			if (!(sources[2] instanceof Number number) || number.intValue() < 1) {

				throw new FrameworkException(422, getName() + "(): limit must be a positive number.");
			}

			limit = Math.min(MAX_LIMIT, number.intValue());
		}

		final Map<NodeInterface, Double> result = StructrApp.getInstance(ctx.getSecurityContext()).getNodesFromVectorIndex(indexName, limit, vector);
		final List<Map<String, Object>> list    = new LinkedList<>();

		for (final Map.Entry<NodeInterface, Double> entry : result.entrySet()) {

			list.add(Map.of("node", entry.getKey(), "score", entry.getValue()));
		}

		return list;
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(
			Usage.structrScript("Usage: ${searchVector(indexName, vector [, limit ])}."),
			Usage.javaScript("Usage: ${{ $.searchVector(indexName, vector [, limit ]) }}.")
		);
	}

	@Override
	public String getShortDescription() {

		return "Returns the entities whose vectors are nearest to the given vector, with their similarity scores, from the given vector index.";
	}

	@Override
	public String getLongDescription() {

		return """
			Finds the nearest neighbours of a vector in a vector index and returns a list of objects with `node` and `score`, best first. The \
			score is the similarity between 0 and 1 (1 for the same direction with cosine similarity).

			A vector index is created for a `Double[]` attribute whose *Vector* size is set in the schema (`vectorDimensions`, and optionally \
			`vectorSimilarity`, cosine or euclidean). Its name is generated as `<type name>_<attribute name>_vector`, for example \
			`Product_embedding_vector`. Only vectors of exactly that size are indexed.

			The vectors can come from anywhere, e.g. from an embedding model (in Structr Enterprise, `llmEmbed()`), and the vector given here \
			must come from the same source. Only entities the current user can read are returned, so there can be fewer than `limit` results.

			Vector indexes need Neo4j 5.13 or later.""";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("indexName", "the name of the vector index, `<type name>_<attribute name>_vector`"),
			Parameter.mandatory("vector", "a list of numbers of the size of the index"),
			Parameter.optional("limit", "the number of nearest neighbours, default 10, at most 1000")
		);
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.javaScript("""
				${{
					// Product.embedding is a Double[] attribute with vector size 768
					let query = $.llmEmbed(config, 'nomic-embed-text', 'a warm jacket for hiking');

					$.searchVector('Product_embedding_vector', query, 5).map(hit => hit.node.name);
				}}
				""", "The five products whose descriptions are closest in meaning to a search phrase (with the Enterprise llmEmbed function)")
		);
	}

	@Override
	public FunctionCategory getCategory() {

		return FunctionCategory.Database;
	}

	// ----- private methods -----
	private double[] vector(final Object value) throws FrameworkException {

		final Object source = value instanceof Object[] array ? Arrays.asList(array) : value;

		if (value instanceof double[] array) {

			return array;
		}

		if (source instanceof Iterable<?> iterable) {

			final List<Double> values = new ArrayList<>();

			for (final Object element : iterable) {

				if (!(element instanceof Number number)) {

					throw new FrameworkException(422, getName() + "(): the vector must be a list of numbers.");
				}

				values.add(number.doubleValue());
			}

			if (!values.isEmpty()) {

				return values.stream().mapToDouble(Double::doubleValue).toArray();
			}
		}

		throw new FrameworkException(422, getName() + "(): the vector must be a non-empty list of numbers.");
	}
}
