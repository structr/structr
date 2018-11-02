/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.rest.adapter;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import org.structr.api.util.PagingIterable;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.Value;
import org.structr.core.rest.GraphObjectGSONAdapter;

/**
 * Controls deserialization of property sets.
 */
public class ResultGSONAdapter implements JsonSerializer<Result>, JsonDeserializer<Result> {

	private final DecimalFormat decimalFormat             = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
	private GraphObjectGSONAdapter graphObjectGsonAdapter = null;

	public ResultGSONAdapter(Value<String> propertyView, final int outputNestingDepth) {
		this.graphObjectGsonAdapter = new GraphObjectGSONAdapter(propertyView, outputNestingDepth);
	}

	@Override
	public JsonElement serialize(Result src, Type typeOfSrc, JsonSerializationContext context) {

		long t0 = System.nanoTime();

		JsonObject result = new JsonObject();

		// result fields in alphabetical order
		Iterable<? extends GraphObject> results = src.getResults();
		Integer page                            = src.getPage();
		Integer pageSize                        = src.getPageSize();
		String queryTime                        = src.getQueryTime();
		String searchString                     = src.getSearchString();
		String sortKey                          = src.getSortKey();
		String sortOrder                        = src.getSortOrder();
		GraphObject metaData                    = src.getMetaData();

		if(results != null) {

			if(src.isEmpty()) {

				final Object nonGraphObjectResult = src.getNonGraphObjectResult();
				if (nonGraphObjectResult != null) {

					result.add("result", graphObjectGsonAdapter.serializeObject(nonGraphObjectResult, System.currentTimeMillis()));


				} else {

					result.add("result", new JsonArray());
				}

			} else if (src.isPrimitiveArray()) {

				JsonArray resultArray = new JsonArray();
				for(GraphObject graphObject : results) {

					final Object value = graphObject.getProperty(GraphObject.id);	// FIXME: UUID key hard-coded, use variable in Result here!
					if (value != null) {

						resultArray.add(new JsonPrimitive(value.toString()));
					}
				}

				result.add("result", resultArray);


			} else {

				// FIXME: do we need this check, or does it cause trouble?
				if (src.size() > 1 && !src.isCollection()){
					throw new IllegalStateException(src.getClass().getSimpleName() + " is not a collection resource, but result set has size " + src.size());
				}

				// keep track of serialization time
				long startTime = System.currentTimeMillis();

				if(src.isCollection()) {

					// serialize list of results
					JsonArray resultArray = new JsonArray();
					for(GraphObject graphObject : results) {

						JsonElement element = graphObjectGsonAdapter.serialize(graphObject, startTime);
						if (element != null) {

							resultArray.add(element);

						} else {

							// stop serialization if timeout occurs
							result.add("status", new JsonPrimitive("Serialization aborted due to timeout"));
							src.setHasPartialContent(true);

							break;
						}
					}

					result.add("result", resultArray);

				} else {

					// use GraphObject adapter to serialize single result
					result.add("result", graphObjectGsonAdapter.serialize(src.get(0), startTime));
				}
			}
		}

		final long t1 = System.nanoTime();

		if(page != null) {
			result.add("page", new JsonPrimitive(page));
		}

		if(pageSize != null) {
			result.add("page_size", new JsonPrimitive(pageSize));
		}

		if(queryTime != null) {
			result.add("query_time", new JsonPrimitive(queryTime));
		}

		if(searchString != null) {
			result.add("search_string", new JsonPrimitive(searchString));
		}

		if(sortKey != null) {
			result.add("sort_key", new JsonPrimitive(sortKey));
		}

		if(sortOrder != null) {
			result.add("sort_order", new JsonPrimitive(sortOrder));
		}

		if (metaData != null) {

			JsonElement element = graphObjectGsonAdapter.serialize(metaData, System.currentTimeMillis());
			if (element != null) {

				result.add("meta_data", element);
			}
		}

		if (results instanceof PagingIterable) {

			final PagingIterable iterable = (PagingIterable)results;

			result.add("result_count", new JsonPrimitive(iterable.getResultCount()));
			result.add("page_count", new JsonPrimitive(iterable.getPageCount()));

			result.add("result_count_time", new JsonPrimitive(decimalFormat.format((System.nanoTime() - t1) / 1000000000.0)));
		}

		result.add("serialization_time", new JsonPrimitive(decimalFormat.format((t1 - t0) / 1000000000.0)));

		return result;
	}

	@Override
	public Result deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		return null;
	}

	public GraphObjectGSONAdapter getGraphObjectGSONAdapter() {
		return graphObjectGsonAdapter;
	}
}
