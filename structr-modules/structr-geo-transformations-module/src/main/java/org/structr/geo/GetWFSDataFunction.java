/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.geo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.ArgumentTypeException;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Signature;
import org.structr.schema.action.ActionContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GetWFSDataFunction extends AbstractGeoserverFunction {

	private static final Logger logger       = LoggerFactory.getLogger(GetWFSDataFunction.class.getName());
	public static final String ERROR_MESSAGE = "usage: get_wfs_data(baseUrl, version, typeName [, parameterString ])";

	@Override
	public String getName() {
		return "get_wfs_data";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("baseUrl, version, typeName, [, params ]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndTypes(sources, 3, String.class, String.class, String.class, String.class);

			final Map<String, Object> data = new LinkedHashMap<>();
			final String baseUrl           = (String)sources[0];
			final String version           = (String)sources[1];
			final String typeName          = (String)sources[2];
			final String parameters        = (String)sources[3];

			data.put("data", getWFSData(baseUrl, version, typeName, parameters));

			// we need to return a single object that contains all the data since Structr returns a
			// list with a single element in a different format that a list with multiple elements
			// when the enclosing resource endpoint is not a collection endpoint.. :(
			return data;

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return "";

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());

		} catch (ArgumentTypeException te) {

			logParameterError(caller, sources, te.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public String usage(final boolean inJavaScriptContext) {
		return ERROR_MESSAGE;
	}

	@Override
	public String getShortDescription() {
		return "Reads features from a WFS endpoint and returns geometries.";
	}
}