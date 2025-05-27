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

import com.vividsolutions.jts.geom.Geometry;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;

public class ConvertGeometryFunction extends GeoFunction {

	private static final String ERROR_MESSAGE = "Usage: convert_geometry(sourceCRS, destCRS, geometry)";
	private static final Logger logger        = LoggerFactory.getLogger(ConvertGeometryFunction.class.getName());

	@Override
	public String getName() {
		return "convert_geometry";
	}

	@Override
	public String getSignature() {
		return "sourceCRS, destCRS, geometry";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndTypes(sources, 3, String.class, String.class, Geometry.class);

			final String sourceCRS  = (String)sources[0];
			final String targetCRS  = (String)sources[1];
			final Geometry geometry = (Geometry)sources[2];

			try {

				final CoordinateReferenceSystem src = CRS.decode(sourceCRS);
				final CoordinateReferenceSystem dst = CRS.decode(targetCRS);
				final MathTransform transform       = CRS.findMathTransform(src, dst, true);

				return JTS.transform(geometry, transform);

			} catch (Throwable t) {

				logger.error(ExceptionUtils.getStackTrace(t));
			}


		} catch (IllegalArgumentException e) {

			boolean isJs = ctx != null ? ctx.isJavaScriptContext() : false;
			logParameterError(caller, sources, e.getMessage(), isJs);
			return usage(isJs);
		}

		return usage(ctx != null ? ctx.isJavaScriptContext() : false);
	}

	@Override
	public String usage(final boolean inJavaScriptContext) {
		return ERROR_MESSAGE;
	}

	@Override
	public String shortDescription() {
		return "Converts the given geometry from source CRS to destination CRS";
	}
}
