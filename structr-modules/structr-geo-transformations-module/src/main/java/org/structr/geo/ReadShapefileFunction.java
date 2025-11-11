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
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.data.PrjFileReader;
import org.geotools.data.shapefile.dbf.DbaseFileHeader;
import org.geotools.data.shapefile.dbf.DbaseFileReader;
import org.geotools.data.shapefile.files.ShpFileType;
import org.geotools.data.shapefile.files.ShpFiles;
import org.geotools.data.shapefile.shp.ShapefileReader;
import org.geotools.data.shapefile.shp.ShapefileReader.Record;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;

public class ReadShapefileFunction extends GeoFunction {

	private static final Logger logger                                   = LoggerFactory.getLogger(ReadShapefileFunction.class.getName());
	public static final String ERROR_MESSAGE                             = "";

	@Override
	public String getName() {
		return "read_shapefile";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("filename");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			if (sources[0] instanceof String) {

				final String filename = (String)sources[0];
				if (filename != null) {

					final SecurityContext securityContext = ctx.getSecurityContext();
					final String tempUuid                 = "structr-" + NodeServiceCommand.getNextUuid();

					// make security context available to the code inside the following block
					securityContext.storeTemporary(tempUuid);

					try {

						// we create a custom URL that references the SecurityContext stored above
						final URL contentUrl                  = new URL(tempUuid + ":" + filename);
						final GeometryFactory gf              = new GeometryFactory();
						final ShpFiles shpFiles               = new ShpFiles(contentUrl);
						final ShapefileReader reader          = new ShapefileReader(shpFiles, true, true, gf);
						final CoordinateReferenceSystem wgs84 = CRS.decode("EPSG:4326");
						final CoordinateReferenceSystem crs   = readCRS(shpFiles, reader);
						final List<Map<String, Object>> data  = new LinkedList<>();
						final Map<String, Object> result      = new LinkedHashMap<>();
						MathTransform transform               = null;

						if (crs != null) {

							transform = CRS.findMathTransform(crs, wgs84, true);
						}

						final List<String> metadataFields        = new LinkedList<>();
						final List<Map<String, Object>> metadata = readDBF(shpFiles, metadataFields);
						Iterator<Map<String, Object>> iterator   = null;
						int count                                = 0;

						if (metadata != null) {
							iterator = metadata.iterator();
						}

						while (reader.hasNext() && (iterator == null || iterator.hasNext())) {

							final Map<String, Object> item = new LinkedHashMap<>();
							final Record record            = reader.nextRecord();
							final Object shape             = record.shape();

							if (shape instanceof Geometry) {

								Geometry geometry = (Geometry)shape;

								// transform to WGS-84
								if (transform != null) {
									geometry = JTS.transform(geometry, transform);
								}

								item.put("wkt", geometry.toString());

								// store data as well
								if (iterator != null) {
									item.put("metadata", iterator.next());
								}

								data.add(item);
							}

							if (++count % 1000 == 0) {
								logger.info("Number of geometries: {}", count);
							}
						}

						reader.close();

						result.put("geometries", data);
						result.put("fields",     metadataFields);

						return result;

					} catch (Throwable t) {

						logger.error(ExceptionUtils.getStackTrace(t));

					} finally {

						securityContext.clearTemporary(tempUuid);
					}
				}

			} else {

				logger.warn("Invalid parameter for shapefile import, expected string, got {}", sources[0].getClass().getSimpleName() );
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
		return "Reads a shapefile from a Structr path and returns it as a list of WKT strings.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	// ----- private methods -----
	private static CoordinateReferenceSystem readCRS(ShpFiles shpFiles, ShapefileReader shpReader) {

		try {

			final PrjFileReader prjReader = new PrjFileReader(shpFiles.getReadChannel(ShpFileType.PRJ, shpReader));

			try {
				return prjReader.getCoordinateReferenceSystem();

			} finally {

				prjReader.close();
			}

		} catch (IOException | FactoryException e) {

			try {

				// default to RD for now...
				return CRS.decode("EPSG:28992");

			} catch (FactoryException ex) {
				logger.error("", ex);
			}
        }

		return null;
	}

	private static List<Map<String, Object>> readDBF(ShpFiles shpFiles, final List<String> metadataFields) {


		try {

			final DbaseFileReader reader = new DbaseFileReader(shpFiles, true, Charset.forName("utf-8"));
			final List<Map<String, Object>> data = new LinkedList<>();

			try {

				final DbaseFileHeader header = reader.getHeader();
				final int num                = header.getNumFields();

				for (int i=0; i<num; i++) {
					metadataFields.add(header.getFieldName(i));
				}

				while (reader.hasNext()) {

					final Map<String, Object> row = new LinkedHashMap<>();
					final Object[] entry          = reader.readEntry();

					if (entry != null) {

						for (int i=0; i<entry.length; i++) {

							final Object o = entry[i];
							if (o != null) {

								row.put(header.getFieldName(i), o);
							}
						}
					}

					data.add(row);
				}

			} finally {

				reader.close();
			}

			return data;

		} catch (IOException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}

		return null;
	}
}
