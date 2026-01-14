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

import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.linemerge.LineMerger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class LineStringsToPolygonsFunction extends GeoFunction {

	private static final Logger logger       = LoggerFactory.getLogger(LineStringsToPolygonsFunction.class.getName());
	public static final String ERROR_MESSAGE = "";

	@Override
	public String getName() {
		return "lineStringsToPolygons";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("list");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			final GeometryFactory factory = new GeometryFactory();
			final List<Polygon> polygons  = new LinkedList<>();
			final LineMerger merger       = new LineMerger();

			if (sources[0] instanceof Collection) {

				merger.add((Collection)sources[0]);

			} else if (sources[0] instanceof Geometry) {

				merger.add((Geometry)sources[0]);

			} else {

				logger.warn("Invalid parameter, expected geometry or list of geometries, got {}", sources[0].getClass().getSimpleName() );

				return "Invalid parameters";
			}

			// merge line strings and return closed polygons wherever possible
			for (final Object obj : merger.getMergedLineStrings()) {

				final LineString lineString = (LineString)obj;
				final CoordinateList list   = new CoordinateList(lineString.getCoordinates());

				list.closeRing();

				polygons.add(factory.createPolygon(factory.createLinearRing(list.toCoordinateArray()), null));
			}

			return polygons;

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
		return "Merges line strings to polygons.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}
}
