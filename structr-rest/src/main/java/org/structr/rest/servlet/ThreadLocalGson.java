/**
 * Copyright (C) 2010-2017 Structr GmbH
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
package org.structr.rest.servlet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.IJsonInput;
import org.structr.core.Result;
import org.structr.core.Value;
import org.structr.rest.JsonInputGSONAdapter;
import org.structr.rest.adapter.FrameworkExceptionGSONAdapter;
import org.structr.rest.adapter.ResultGSONAdapter;

/**
 *
 */
public class ThreadLocalGson extends ThreadLocal<Gson> {

	private int outputNestingDepth     = 3;
	private Value<String> propertyView = null;

	public ThreadLocalGson(final Value<String> propertyView, final int outputNestingDepth) {

		this.propertyView       = propertyView;
		this.outputNestingDepth = outputNestingDepth;
	}

	@Override
	protected Gson initialValue() {

		final ResultGSONAdapter resultGsonAdapter   = new ResultGSONAdapter(propertyView, outputNestingDepth);
		final JsonInputGSONAdapter jsonInputAdapter = new JsonInputGSONAdapter();

		// create GSON serializer
		final GsonBuilder gsonBuilder = new GsonBuilder()
			.setPrettyPrinting()
			.serializeNulls()
			.registerTypeHierarchyAdapter(FrameworkException.class, new FrameworkExceptionGSONAdapter())
			.registerTypeAdapter(IJsonInput.class, jsonInputAdapter)
			.registerTypeAdapter(Result.class, resultGsonAdapter);


		final boolean lenient = Settings.JsonLenient.getValue();
		if (lenient) {

			// Serializes NaN, -Infinity, Infinity, see http://code.google.com/p/google-gson/issues/detail?id=378
			gsonBuilder.serializeSpecialFloatingPointValues();

		}

		return gsonBuilder.create();
	}
}
