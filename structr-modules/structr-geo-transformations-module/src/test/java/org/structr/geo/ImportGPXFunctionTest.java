/**
 * Copyright (C) 2010-2016 Structr GmbH
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
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.geo;

import static org.junit.Assert.fail;
import org.junit.Test;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;

/**
 *
 */
public class ImportGPXFunctionTest {

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

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

}
