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
import org.geotools.geometry.jts.JTS;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.WKTReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class WKTToPolygonsFunction extends GeoFunction {

	private static final Logger logger                                   = LoggerFactory.getLogger(WKTToPolygonsFunction.class.getName());
	public static final String ERROR_MESSAGE                             = "";

	@Override
	public String getName() {
		return "wkt_to_polygons";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("wktString");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			if (sources[0] instanceof String) {

				final String wkt = (String)sources[0];
				if (wkt != null) {

					try {

						final List result      = new LinkedList<>();
						final WKTReader reader = new WKTReader();
						final Geometry source  = reader.read(wkt);

						handleGeometry(source, result);

						return result;

					} catch (Throwable t) {
						logger.error(ExceptionUtils.getStackTrace(t));
					}
				}

			} else {

				logger.warn("Invalid parameter for wkt_to_coordinates, expected string, got {}", sources[0].getClass().getSimpleName() );
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
		return "Converts a WKT string into a list of polygons.";
	}

	// ----- private methods -----
	private void handleGeometry(final Geometry source, final List result) {

		if (source instanceof GeometryCollection) {

			final GeometryCollection collection = (GeometryCollection)source;
			final int count                     = collection.getNumGeometries();

			for (int i=0; i<count; i++) {

				final Geometry geometry = collection.getGeometryN(i);
				final List nestedList   = new LinkedList<>();

				// recurse
				handleGeometry(geometry, nestedList);

				result.add(nestedList);
			}

			return;
		}

		if (source instanceof Polygon) {

			final Polygon polygon     = (Polygon)source;
			final List<Polygon> valid = JTS.makeValid(polygon, true);

			if (valid.size() > 0) {

				if (valid.size() > 1) {

					for (final Polygon p : valid) {

						final List nestedList = new LinkedList<>();
						handleGeometry(p, nestedList);
						result.add(nestedList);
					}


				} else {

					// add single polygon
					for (final Coordinate c: valid.get(0).getCoordinates()) {

						result.add(Arrays.asList(c.x, c.y));
					}
				}

			} else {

				// add single polygon
				for (final Coordinate c: source.getCoordinates()) {

					result.add(Arrays.asList(c.x, c.y));
				}
			}

			return;
		}

		if (source instanceof LineString) {

			final LineString lineString   = (LineString)source;
			final CoordinateList list     = new CoordinateList(lineString.getCoordinates());
			final GeometryFactory factory = new GeometryFactory();

			list.closeRing();

			final LinearRing ring = factory.createLinearRing( list.toCoordinateArray() );
			handleGeometry(factory.createPolygon(ring, null), result);

			return;
		}
	}
}
