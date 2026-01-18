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
package org.structr.geo;

import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.ArgumentTypeException;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GetWCSDataFunction extends AbstractGeoserverFunction {

	private static final Logger logger       = LoggerFactory.getLogger(GetWCSDataFunction.class.getName());
	public static final String ERROR_MESSAGE = "usage: getWcsData(baseUrl, coverageId, boundingBox, min, max)";

	@Override
	public String getName() {
		return "getWcsData";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("baseUrl, coverageId, bBox, min, max");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndTypes(sources, 5, String.class, String.class, Geometry.class, Number.class, Number.class);

			final Map<String, Object> data = new LinkedHashMap<>();
			final String baseUrl           = (String)sources[0];
			final String coverageId        = (String)sources[1];
			final Geometry boundingBox     = (Geometry)sources[2];
			final Number min               = (Number)sources[3];
			final Number max               = (Number)sources[4];

			data.put("data", getFilteredCoverageGeometries(baseUrl, coverageId, boundingBox, min.doubleValue(), max.doubleValue()));

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
	public List<Usage> getUsages() {
		return List.of(
		);
	}

	@Override
	public String getShortDescription() {
		return "Reads coverage data from a WCS endpoint and returns it.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}
}