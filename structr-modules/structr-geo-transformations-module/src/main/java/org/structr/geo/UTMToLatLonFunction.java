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

import org.geotools.api.geometry.Position;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.geometry.Position2D;
import org.geotools.referencing.CRS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.property.DoubleProperty;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class UTMToLatLonFunction extends GeoFunction {

	private static final String ERROR_MESSAGE            = "Usage: ${utmToLatLon(utmString)}.";
	private static final Logger logger                   = LoggerFactory.getLogger(UTMToLatLonFunction.class.getName());
	private static final String UTMHemisphere            = "SSSSSSSSSSNNNNNNNNNNN";
	private static final String UTMzdlChars              = "CDEFGHJKLMNPQRSTUVWXX";
	public static final DoubleProperty latitudeProperty  = new DoubleProperty("latitude");
	public static final DoubleProperty longitudeProperty = new DoubleProperty("longitude");

	@Override
	public String getName() {
		return "utmToLatLon";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("utmString");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			final String utmString = (String)sources[0];
			if (utmString != null) {

				final String[] parts = utmString.split("[\\s]+");

				if (parts.length < 3) {

					logger.warn("Unsupported UTM string: this implementation only supports the full UTM format with spaces, e.g. 32U 439596 5967780 or 32 N 439596 5967780.");

				} else if (parts.length == 3) {

					// full UTM string
					// 32U 439596 5967780
					final String zone = parts[0];
					final String east = parts[1];
					final String north = parts[2];

					return utmToLatLon(zone, getHemisphereFromZone(zone), east, north);

				} else if (parts.length == 4) {

					// full UTM string with hemisphere indication
					// 32 N 439596 5967780
					final String zone       = parts[0];
					final String hemisphere = parts[1];
					final String east       = parts[2];
					final String north      = parts[3];

					return utmToLatLon(zone, hemisphere, east, north);

				}

			} else {

				logger.warn("Invalid argument(s), cannot convert to double: {}, {}", new Object[] { sources[0], sources[1] });
			}

		} catch (ArgumentNullException ae) {

			boolean isJs = ctx != null ? ctx.isJavaScriptContext() : false;
			logParameterError(caller, sources, ae.getMessage(), isJs);
			return "Unsupported UTM string";

		} catch (ArgumentCountException ae) {

			boolean isJs = ctx != null ? ctx.isJavaScriptContext() : false;
			logParameterError(caller, sources, ae.getMessage(), isJs);
			return usage(isJs);
		}

		return "Unsupported UTM string";
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
				Usage.javaScript("Usage: ${{ $.utmToLatLon(utmString) }}."),
				Usage.structrScript("Usage: ${utmToLatLon(utmString)}.")
		);
	}

	@Override
	public String getShortDescription() {
		return "Converts the given UTM string to latitude/longitude coordinates.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("""
						${utmToLatLon('32U 395473 5686479')}
						> {latitude=53.85499997165232, longitude=8.081674915658844}
						"""),
				Example.javaScript("""
						${{ $.utmToLatLon('32U 395473 5686479') 
						> {latitude=53.85499997165232, longitude=8.081674915658844}
						}}""")
		);
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
				Parameter.mandatory("utmString", "UTM location string")
				);
	}

	// ----- private methods -----

	private String getHemisphereFromZone(final String zone) {

		String band = null;

		switch (zone.length()) {

			case 3:
				// we can safely assume a format of "32U"
				band = zone.substring(2);
				break;

			case 2:
				// can be either single digit zone plus band
				// or double digit zone w/o band..
				if (zone.matches("\\d\\D")) {

					// single-digit zone plus band
					band = zone.substring(1);
				}
				break;
		}

		if (band != null) {

			final int pos = UTMzdlChars.indexOf(band);
			if (pos >= 0) {

				return UTMHemisphere.substring(pos, pos+1);
			}
		}

		logger.warn("Unable to determine hemisphere from UTM zone, assuming NORTHERN hemisphere.");

		return "N";
	}


	private GraphObjectMap utmToLatLon(final String zone, final String hemisphere, final String east, final String north) {

		final GraphObjectMap obj = new GraphObjectMap();

		// clean zone string (remove all non-digits)
		final String cleanedZone = zone.replaceAll("[\\D]+", "");
		final StringBuilder epsg = new StringBuilder("EPSG:32");

		switch (hemisphere) {

			case "N":
				epsg.append("6");
				break;

			case "S":
				epsg.append("7");
				break;
		}

		// append "0" to zone number of single-digit
		if (cleanedZone.length() == 1) {
			epsg.append("0");
		}

		// append zone number
		epsg.append(cleanedZone);

		try {

			final CoordinateReferenceSystem src = CRS.decode(epsg.toString());
			final CoordinateReferenceSystem dst = CRS.decode("EPSG:4326");
			final MathTransform transform       = CRS.findMathTransform(src, dst, true);
			final Position sourcePt             = new Position2D(getDoubleOrNull(east), getDoubleOrNull(north));
			final Position targetPt             = transform.transform(sourcePt, null);

			obj.put(latitudeProperty, targetPt.getOrdinate(0));
			obj.put(longitudeProperty, targetPt.getOrdinate(1));

		} catch (Throwable t) {

			logger.warn("", t);
		}

		return obj;
	}
}
