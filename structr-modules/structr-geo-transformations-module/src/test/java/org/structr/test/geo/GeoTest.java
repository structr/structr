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
package org.structr.test.geo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.App;
import org.structr.geo.ImportGPXFunction;
import org.structr.geo.LatLonToUTMFunction;
import org.structr.geo.UTMToLatLonFunction;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

/**
 *
 */
public class GeoTest {

	private static final Logger logger = LoggerFactory.getLogger(GeoTest.class.getName());

	protected static SecurityContext securityContext = null;
	protected static String basePath                 = null;
	protected static App app                         = null;

	@Test
	public void testLatLonToUTM() {

		final LatLonToUTMFunction func = new LatLonToUTMFunction();

		try {

			final Object result1 = func.apply(null, null, new Object[] { 53.85499997165232, 8.081674915658844 });
			assertEquals("Invalid UTM conversion result", "32U 439596 5967780" , result1);

			final Object result2 = func.apply(null, null, new Object[] { 51.319997116243364, 7.49998773689121 });
			assertEquals("Invalid UTM conversion result", "32U 395473 5686479", result2);

			final Object result3 = func.apply(null, null, new Object[] { -38.96442577579118, 7.793498600057568 });
			assertEquals("Invalid UTM conversion result", "32H 395473 5686479", result3);

			final Object result4 = func.apply(null, null, new Object[] { 51.319997116243364, -166.5000122631088});
			assertEquals("Invalid UTM conversion result", "3U 395473 5686479", result4);

			final Object result5 = func.apply(null, null, new Object[] { -36.59789213337618, -164.5312529421211 });
			assertEquals("Invalid UTM conversion result", "3H 541926 5949631", result5);

		} catch (FrameworkException fex) {
			logger.warn("", fex);
			fail("Unexpected exception");
		}

	}

	@Test
	public void testGPXImport() {

		try {

			final ImportGPXFunction func = new ImportGPXFunction();
			final StringBuilder gpx      = new StringBuilder();

			gpx.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>");
			gpx.append("<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" version=\"1.1\" creator=\"Wikipedia\"");
			gpx.append("    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
			gpx.append("    xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">");
			gpx.append(" <!-- Kommentare sehen so aus -->");
			gpx.append(" <metadata>");
			gpx.append("  <name>Dateiname</name>");
			gpx.append("  <desc>Validiertes GPX-Beispiel ohne Sonderzeichen</desc>");
			gpx.append("  <author>");
			gpx.append("   <name>Autorenname</name>");
			gpx.append("  </author>");
			gpx.append(" </metadata>");
			gpx.append(" <wpt lat=\"52.518611\" lon=\"13.376111\">");
			gpx.append("  <ele>35.0</ele>");
			gpx.append("  <time>2011-12-31T23:59:59Z</time>");
			gpx.append("  <name>Reichstag (Berlin)</name>");
			gpx.append("  <sym>City</sym>");
			gpx.append(" </wpt>");
			gpx.append(" <wpt lat=\"48.208031\" lon=\"16.358128\">");
			gpx.append("  <ele>179</ele>");
			gpx.append("  <time>2011-12-31T23:59:59Z</time>");
			gpx.append("  <name>Parlament (Wien)</name>");
			gpx.append("  <sym>City</sym>");
			gpx.append(" </wpt>");
			gpx.append(" <wpt lat=\"46.9466\" lon=\"7.44412\">");
			gpx.append("  <time>2011-12-31T23:59:59Z</time>");
			gpx.append("  <name>Bundeshaus (Bern)</name>");
			gpx.append("  <sym>City</sym>");
			gpx.append(" </wpt>");
			gpx.append(" <rte>");
			gpx.append("  <name>Routenname</name>");
			gpx.append("  <desc>Routenbeschreibung</desc>");
			gpx.append("  <rtept lat=\"52.0\" lon=\"13.5\">");
			gpx.append("   <ele>33.0</ele>");
			gpx.append("   <time>2011-12-13T23:59:59Z</time>");
			gpx.append("   <name>rtept 1</name>");
			gpx.append("  </rtept>");
			gpx.append("  <rtept lat=\"49\" lon=\"12\">");
			gpx.append("   <name>rtept 2</name>");
			gpx.append("  </rtept>");
			gpx.append("  <rtept lat=\"47.0\" lon=\"7.5\">");
			gpx.append("  </rtept>");
			gpx.append(" </rte>");
			gpx.append(" <trk>");
			gpx.append("  <name>Trackname1</name>");
			gpx.append("  <desc>Trackbeschreibung</desc>");
			gpx.append("  <trkseg>");
			gpx.append("   <trkpt lat=\"52.520000\" lon=\"13.380000\">");
			gpx.append("    <ele>36.0</ele>");
			gpx.append("    <time>2011-01-13T01:01:01Z</time>");
			gpx.append("   </trkpt>");
			gpx.append("   <trkpt lat=\"48.200000\" lon=\"16.260000\">");
			gpx.append("    <ele>180</ele>");
			gpx.append("    <time>2011-01-14T01:59:01Z</time>");
			gpx.append("   </trkpt>");
			gpx.append("   <trkpt lat=\"46.95\" lon=\"7.4\">");
			gpx.append("    <ele>987.654</ele>");
			gpx.append("    <time>2011-01-15T23:59:01Z</time>");
			gpx.append("   </trkpt>");
			gpx.append("  </trkseg>");
			gpx.append(" </trk>");
			gpx.append(" <trk>");
			gpx.append("  <name>Trackname2</name>");
			gpx.append("  <trkseg>");
			gpx.append("   <trkpt lat=\"47.2\" lon=\"7.41\">");
			gpx.append("    <time>2011-01-16T23:59:01Z</time>");
			gpx.append("   </trkpt>");
			gpx.append("   <trkpt lat=\"52.53\" lon=\"13.0\">");
			gpx.append("   </trkpt>");
			gpx.append("  </trkseg>");
			gpx.append(" </trk>");
			gpx.append("</gpx>");

			final GraphObjectMap obj = (GraphObjectMap)func.apply(null, null, new Object[] { gpx.toString() });

			System.out.println(obj);


		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception");
		}
	}

	@Test
	public void testUTMLatLonRoundTrip() {

		final LatLonToUTMFunction latLonUtm = new LatLonToUTMFunction();
		final UTMToLatLonFunction utmLatLon = new UTMToLatLonFunction();
		final String sourceUTM              = "32U 439596 5967780";

		try {
			final GraphObjectMap result1 = (GraphObjectMap)utmLatLon.apply(null, null, new Object[] { sourceUTM });
			final String result2         = (String)latLonUtm.apply(null, null, new Object[] { result1.getProperty(UTMToLatLonFunction.latitudeProperty), result1.getProperty(UTMToLatLonFunction.longitudeProperty) } );

			assertEquals("Invalid UTM to lat/lon roundtrip result", sourceUTM, result2);

		} catch (FrameworkException fex) {
			logger.warn("", fex);
			fail("Unexpected exception");
		}
	}

	@Test
	public void testLatLonUTMRoundtrip() {

		final LatLonToUTMFunction latLonUtm = new LatLonToUTMFunction();
		final UTMToLatLonFunction utmLatLon = new UTMToLatLonFunction();
		final double latitude               = 51.319997116243364;
		final double longitude              = 7.49998773689121;

		try {
			final String result1         = (String)latLonUtm.apply(null, null, new Object[] { latitude, longitude } );
			final GraphObjectMap result2 = (GraphObjectMap)utmLatLon.apply(null, null, new Object[] { result1 } );

			assertEquals("Invalid UTM to lat/lon roundtrip result", (Double)latitude,  result2.getProperty(UTMToLatLonFunction.latitudeProperty));
			assertEquals("Invalid UTM to lat/lon roundtrip result", (Double)longitude, result2.getProperty(UTMToLatLonFunction.longitudeProperty));

		} catch (FrameworkException fex) {
			logger.warn("", fex);
			fail("Unexpected exception");
		}
	}

	@Test
	public void testUTMToLatLon() {

		final UTMToLatLonFunction func = new UTMToLatLonFunction();

		try {

			final Object result6 = func.apply(null, null, new Object[] { "32 N 439596 5967780" });
			assertEquals("Invalid UTM conversion result", 53.854999971652326, get(result6, 0));
			assertEquals("Invalid UTM conversion result", 8.081674915658844, get(result6, 1));

			final Object result7 = func.apply(null, null, new Object[] { "32U 395473 5686479" });
			assertEquals("Invalid UTM conversion result", 51.319997116243364, get(result7, 0));
			assertEquals("Invalid UTM conversion result", 7.49998773689121, get(result7, 1));

			final Object result8 = func.apply(null, null, new Object[] { "32 395473 5686479" });
			assertEquals("Invalid UTM conversion result", 51.319997116243364, get(result8, 0));
			assertEquals("Invalid UTM conversion result", 7.49998773689121, get(result8, 1));

			final Object result9 = func.apply(null, null, new Object[] { "32H 395473 5686479" });
			assertEquals("Invalid UTM conversion result", -38.964425775791184, get(result9, 0));
			assertEquals("Invalid UTM conversion result", 7.793498600057567, get(result9, 1));

			final Object result10 = func.apply(null, null, new Object[] { "3U 395473 5686479" });
			assertEquals("Invalid UTM conversion result", 51.319997116243364, get(result10, 0));
			assertEquals("Invalid UTM conversion result", -166.5000122631088, get(result10, 1));

			final Object result11 = func.apply(null, null, new Object[] { "3 395473 5686479" });
			assertEquals("Invalid UTM conversion result", 51.319997116243364, get(result11, 0));
			assertEquals("Invalid UTM conversion result", -166.5000122631088, get(result11, 1));

			final Object result12 = func.apply(null, null, new Object[] { "3H 541926 5949631" });
			assertEquals("Invalid UTM conversion result", -36.59789213337618, get(result12, 0));
			assertEquals("Invalid UTM conversion result", -164.5312529421211, get(result12, 1));



		} catch (FrameworkException fex) {
			logger.warn("", fex);
			fail("Unexpected exception");
		}

	}

	private Object get(final Object map, final int index) {

		if (map instanceof GraphObjectMap) {

			switch (index) {

				case 0:
					return ((GraphObjectMap)map).getProperty(UTMToLatLonFunction.latitudeProperty);

				case 1:
					return ((GraphObjectMap)map).getProperty(UTMToLatLonFunction.longitudeProperty);
			}
		}

		return null;
	}
}
