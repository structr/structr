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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.geotools.referencing.GeodeticCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;

import java.awt.geom.Point2D;

public class LineSegmentFunction extends GeoFunction {

	private static final Logger logger       = LoggerFactory.getLogger(LineSegmentFunction.class.getName());
	public static final String ERROR_MESSAGE = "";

	@Override
	public String getName() {
		return "line_segment";
	}

	@Override
	public String getSignature() {
		return "point, azimuth, length";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 3);

			final Coordinate coordinate = getCoordinate(sources[0]);

			if (coordinate != null && sources[1] instanceof Number && sources[2] instanceof Number) {

				try {

					final GeometryFactory factory = new GeometryFactory();
					final GeodeticCalculator calc = new GeodeticCalculator();
					final Number azimuth          = (Number)sources[1];
					final Number length           = (Number)sources[2];

					calc.setStartingGeographicPoint(coordinate.y, coordinate.x);
					calc.setDirection(azimuth.doubleValue(), length.doubleValue());

					final Point2D destination = calc.getDestinationGeographicPoint();
					final Coordinate dest     = new Coordinate(destination.getY(), destination.getX());

					return factory.createLineString(new Coordinate[] { coordinate, dest });

				} catch (Throwable t) {

					logger.error(ExceptionUtils.getStackTrace(t));
				}
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
		return "Returns a line segment with start point, azimuth and length";
	}
}
