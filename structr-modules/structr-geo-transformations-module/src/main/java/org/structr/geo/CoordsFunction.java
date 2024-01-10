/*
 * Copyright (C) 2010-2024 Structr GmbH
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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateList;
import com.vividsolutions.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class CoordsFunction extends GeoFunction {

	private static final Logger logger       = LoggerFactory.getLogger(CoordsFunction.class.getName());
	public static final String ERROR_MESSAGE = "";

	@Override
	public String getName() {
		return "get_coordinates";
	}

	@Override
	public String getSignature() {
		return "geometry";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			if (sources[0] instanceof Geometry) {

				final List<List<Double>> result = new LinkedList<>();
				final Geometry geometry         = (Geometry)sources[0];

				for (final Coordinate c : geometry.getCoordinates()) {

					result.add(Arrays.asList(c.x, c.y));
				}

				return result;
			}

			if (sources[0] instanceof CoordinateList) {

				final List<List<Double>> result = new LinkedList<>();
				final CoordinateList list       = (CoordinateList)sources[0];

				for (final Coordinate c : list.toCoordinateArray()) {

					result.add(Arrays.asList(c.x, c.y));
				}

				return result;
			}

			if (sources[0] instanceof Coordinate[]) {

				final List<List<Double>> result = new LinkedList<>();
				final Coordinate[] array        = (Coordinate[])sources[0];

				for (final Coordinate c : array) {

					result.add(Arrays.asList(c.x, c.y));
				}

				return result;
			}

			if (sources[0] instanceof Coordinate) {

				final List<Double> result = new LinkedList<>();
				final Coordinate c        = (Coordinate)sources[0];

				result.add(c.x);
				result.add(c.y);

				return result;
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
		return "Returns the coordinates of a geometry.";
	}
}
