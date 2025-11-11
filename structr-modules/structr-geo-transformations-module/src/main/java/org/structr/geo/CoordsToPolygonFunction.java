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

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateList;
import org.locationtech.jts.geom.GeometryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class CoordsToPolygonFunction extends GeoFunction {

	private static final Logger logger                                   = LoggerFactory.getLogger(CoordsToPolygonFunction.class.getName());
	public static final String ERROR_MESSAGE                             = "";

	@Override
	public String getName() {
		return "coords_to_polygon";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("list");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			if (sources[0] instanceof List) {

				try {

					final GeometryFactory factory    = new GeometryFactory();
					final List source                = (List)sources[0];
					final CoordinateList coordinates = new CoordinateList();

					for (final Object node : source) {

						final Coordinate coordinate = getCoordinate(node);
						if (coordinate != null) {

							coordinates.add(coordinate);
						}
					}

					return factory.createPolygon(factory.createLinearRing(coordinates.toCoordinateArray()), null);

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
	public List<Usage> getUsages() {
		return List.of(
		);
	}

	@Override
	public String getShortDescription() {
		return "Converts a coordinate array into a polygon.";
	}
}
