/**
 * Copyright (C) 2010-2019 Structr GmbH
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

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;

public class GetWCSDataFunction extends AbstractWCSDataFunction {

	private static final Logger logger       = LoggerFactory.getLogger(GetWCSDataFunction.class.getName());
	public static final String ERROR_MESSAGE = "";

	@Override
	public String getName() {
		return "get_wcs_data";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 3);

			if (sources[0] instanceof String) {

				final Map<String, Object> data = new LinkedHashMap<>();
				final String baseUrl           = "http://geodata.rivm.nl/geoserver/wcs?";
				final String coverageId        = (String)sources[0];
				final Number min               = (Number)sources[1];
				final Number max               = (Number)sources[2];

				data.put("data", getFilteredCoveragePoints(baseUrl, coverageId, min.doubleValue(), max.doubleValue()));

				// we need to return a single object that contains all the data since Structr returns a
				// list with a single element in a different format that a list with multiple elements
				// when the enclosing resource endpoint is not a collection endpoint.. :(
				return data;

			} else {

				logger.warn("Invalid parameter for shapefile import, expected string, got {}", sources[0].getClass().getSimpleName() );
			}

			return "Invalid parameters";

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return "";

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public String usage(final boolean inJavaScriptContext) {
		return ERROR_MESSAGE;
	}

	@Override
	public String shortDescription() {
		return "Reads coverage data from a WCS endpoint and returns it";
	}
}