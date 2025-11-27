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

import org.geotools.api.geometry.Position;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.geometry.Position2D;
import org.geotools.referencing.CRS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class LatLonToUTMFunction extends GeoFunction {

	private static final String ERROR_MESSAGE = "Usage: ${lat_lon_to_utm(latitude, longitude)}. Example: ${lat_lon_to_utm(41.3445, 7.35)}";
	private static final Logger logger        = LoggerFactory.getLogger(LatLonToUTMFunction.class.getName());
	private static final String UTMzdlChars   = "CDEFGHJKLMNPQRSTUVWXX";

	@Override
	public String getName() {
		return "lat_lon_to_utm";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("latitude, longitude");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 2);

			final Double lat = getDoubleOrNull(sources[0]);
			final Double lon = getDoubleOrNull(sources[1]);

			if (lat != null && lon != null) {

				try {

					final StringBuilder epsg = new StringBuilder("EPSG:32");
					final int utmZone        = getUTMZone(lat, lon);

					if (lat < 0.0) {

						// southern hemisphere
						epsg.append("7");

					} else {

						// northern hemisphere
						epsg.append("6");
					}

					if (utmZone < 10) {
						epsg.append("0");
					}

					epsg.append(utmZone);


					final CoordinateReferenceSystem src = CRS.decode("EPSG:4326");
					final CoordinateReferenceSystem dst = CRS.decode(epsg.toString());
					final MathTransform transform       = CRS.findMathTransform(src, dst, true);
					final Position sourcePt             = new Position2D(lat, lon);
					final Position targetPt             = transform.transform(sourcePt, null);
					final String code                   = dst.getName().getCode();
					final int pos                       = code.lastIndexOf(" ") + 1;
					final String zoneName               = code.substring(pos, code.length() - 1);
					final String band                   = getLatitudeBand(lat, lon);
					final StringBuilder buf             = new StringBuilder();

					buf.append(zoneName);
					buf.append(band);
					buf.append(" ");
					buf.append((int)Math.rint(targetPt.getOrdinate(0)));
					buf.append(" ");
					buf.append((int)Math.rint(targetPt.getOrdinate(1)));

					// return result
					return buf.toString();

				} catch (Throwable t) {

					logger.warn("", t);
				}

			} else {

				logger.warn("Invalid argument(s), cannot convert to double: {}, {}", new Object[] { sources[0], sources[1] });
			}

		} catch (IllegalArgumentException e) {

			boolean isJs = ctx != null ? ctx.isJavaScriptContext() : false;
			logParameterError(caller, sources, e.getMessage(), isJs);
			return usage(isJs);
		}

		return usage(ctx != null ? ctx.isJavaScriptContext() : false);
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
		);
	}

	@Override
	public String getShortDescription() {
		return "Converts the given latitude/longitude coordinates into an UTM string.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("latitude", "latitude of the desired UTM result"),
			Parameter.mandatory("longitude", "longitude of the desired UTM result")
		);
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.javaScript("""
			${{
				let latitude  = 53.85499997165232;
				let longitude = 8.081674915658844;

				// result: "32U 439596 5967780"
				let utmString = $.latLonToUtm(latitude, longitude);
			}}
			""", "Convert a lat/lon pair to UTM")
		);
	}

	// ----- private methods -----
	private int getUTMZone(final double lat, final double lon) {

		int zone = Double.valueOf(Math.floor((lon + 180.0) / 6.0)).intValue() + 1;

		if (lat >= 56.0 && lat < 64.0 && lon >= 3.0 && lon < 12.0) {
			zone = 32;
		}

		if (lat >= 72.0 && lat < 84.0) {
			if (lon >= 0.0 && lon < 9.0) {
				zone = 31;
			}

		} else if (lon >= 9.0 && lon < 21.0) {
			zone = 33;

		} else if (lon >= 21.0 && lon < 33.0) {

			zone = 35;

		} else if (lon >= 33.0 && lon < 42.0) {
			zone = 37;
		}

		return zone;
	}

	private String getLatitudeBand(final double lat, final double lon) {

		if (lat >= -80.0 && lat <= 84.0) {

			final double band = Math.floor((lat + 80.0) / 8.0);
			final int index   = Double.valueOf(band).intValue();

			return UTMzdlChars.substring(index, index+1);
		}

		return null;
	}
}
