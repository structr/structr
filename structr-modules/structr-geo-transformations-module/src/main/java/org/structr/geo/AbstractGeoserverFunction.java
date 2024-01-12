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
package org.structr.geo;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import org.apache.commons.io.IOUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.coverage.processing.Operations;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.Hints;
import org.geotools.geometry.jts.JTS;
import org.geotools.gml3.v3_2.GMLConfiguration;
import org.geotools.process.raster.PolygonExtractionProcess;
import org.geotools.referencing.CRS;
import org.geotools.xml.Parser;
import org.jaitools.numeric.Range;
import org.json.JSONObject;
import org.json.XML;
import org.opengis.coverage.Coverage;
import org.opengis.feature.Feature;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.structr.common.error.FrameworkException;
import org.structr.rest.common.HttpHelper;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public abstract class AbstractGeoserverFunction extends GeoFunction {

	private static final String[] GEOMETRY_NAMES    = { "geometry", "geometrie" };
	private static final int GEOSERVER_ERROR_STATUS = 502;

	protected URL getWCSDescriptionUrl(final String baseUrl, final String coverageId) throws MalformedURLException {

		final StringBuilder buf = new StringBuilder(baseUrl);

		buf.append("/wcs?service=WCS");
		buf.append("&request=DescribeCoverage");
		buf.append("&coverageId=").append(coverageId);
		buf.append("&version=2.0.1");

		return new URL(buf.toString());
	}

	protected URL getWCSCoverageUrl(final String baseUrl, final String coverageId, final Geometry subset, final String axisLabels) throws MalformedURLException {

		final StringBuilder buf = new StringBuilder(baseUrl);

		buf.append("/wcs?service=WCS");
		buf.append("&request=GetCoverage");
		buf.append("&coverageId=").append(coverageId);
		buf.append("&format=image/tiff");
		buf.append("&version=2.0.1");

		if (subset != null) {

			final Geometry bbox            = subset.getEnvelope();
			final Coordinate[] coordinates = bbox.getCoordinates();
			if (coordinates.length >= 3) {

				final Coordinate lowerLeft  = coordinates[0];
				final Coordinate upperRight = coordinates[2];
				final String[] labels       = axisLabels.split("[\\W]+");

				buf.append("&subset=");
				buf.append(labels[0]);
				buf.append("(");
				buf.append(lowerLeft.x);
				buf.append(",");
				buf.append(lowerLeft.y);
				buf.append(")");
				buf.append("&subset=");
				buf.append(labels[1]);
				buf.append("(");
				buf.append(upperRight.x);
				buf.append(",");
				buf.append(upperRight.y);
				buf.append(")");

			} else {

				logger.warn("Unable to use coverage subset geometry {}, ignoring. Subset geometry must have a bounding box.", subset.toString());
			}
		}

		return new URL(buf.toString());
	}

	protected URL getWFSUrl(final String baseUrl, final String version, final String typeName, final String parameters) throws MalformedURLException {

		final StringBuilder buf = new StringBuilder(baseUrl);

		buf.append("/wfs?SERVICE=WFS");
		buf.append("&version=");
		buf.append(version);
		buf.append("&request=GetFeature");
		buf.append("&typeNames=").append(typeName);

		if (parameters !=null) {
			buf.append(parameters);
		}

		return new URL(buf.toString());
	}

	protected List<Geometry> getFilteredCoverageGeometries(final String baseUrl, final String coverageId, final Geometry boundingBox, final double min, final double max) throws FrameworkException {

		try {

			final List<Geometry> result            = new LinkedList<>();
			final GridCoverage2D coverage          = getWCSCoverage(baseUrl, coverageId, boundingBox);
			final CoordinateReferenceSystem crs    = coverage.getCoordinateReferenceSystem();
			final CoordinateReferenceSystem wgs    = CRS.decode("EPSG:4326");
			final PolygonExtractionProcess extr    = new PolygonExtractionProcess();
			final Collection<Number> noDataValues  = new LinkedList<>();
			final List<Range> classificationRanges = new LinkedList<>();
			final MathTransform transform          = CRS.findMathTransform(crs, wgs);

			classificationRanges.add(new Range(min, true, max, true));

			final SimpleFeatureCollection features = extr.execute(coverage, 0, true, null, noDataValues, classificationRanges, null);
			final SimpleFeatureIterator it         = features.features();

			while (it.hasNext()) {

				final SimpleFeature f = it.next();
				final Object obj      = f.getAttribute("the_geom");

				if (obj instanceof Geometry) {

					result.add(JTS.transform((Geometry)obj, transform));
				}
			}

			return result;

		} catch (FrameworkException fex) {

			throw fex;

		} catch (Throwable t) {

			throw new FrameworkException(422, t.getMessage());
		}
	}

	protected List<Map<String, Object>> getWFSData(final String baseUrl, final String version, final String typeName, final String parameters) throws FrameworkException {

		final List<Map<String, Object>> data = new LinkedList<>();

		try {

			final URL url                      = getWFSUrl(baseUrl, version, typeName, parameters);
			final HttpURLConnection connection = (HttpURLConnection)url.openConnection();

			connection.connect();

			final int statusCode = connection.getResponseCode();
			if (statusCode == 200) {

				try (final InputStream is = connection.getInputStream()) {

					final GMLConfiguration config  = new GMLConfiguration();
					final List<Feature> features   = new LinkedList<>();
					final Parser parser            = new Parser(config);
					final Object result            = parser.parse(is);

					if (result instanceof Map) {

						final Map<String, Object> map = (Map)result;
						final Object member           = map.get("member");

						if (member != null) {

							if (member instanceof List) {

								features.addAll((List)member);

							} else if (member instanceof Feature) {

								features.add((Feature)member);
							}

						} else {

							// single result that was not parsed into a Feature impl
							data.add(map);
						}

					} else if (result instanceof Feature) {

						features.add((Feature)result);
					}

					for (final Object obj : features) {

						if (obj instanceof Feature) {

							final Feature feature         = (Feature)obj;
							final Map<String, Object> map = new LinkedHashMap<>();

							map.put("name", feature.getName().toString());
							map.put("id",   feature.getIdentifier().getID());

							for (final Property property : feature.getProperties()) {

								map.put(property.getName().toString(), property.getValue());
							}

							data.add(map);

						} else if (obj instanceof Map) {

							data.add((Map)obj);

						} else {

							// unknown type
							logger.warn("Unknown feature  type {}, ignoring.", obj);
						}
					}
				}

			} else {

				try (final InputStream is = connection.getErrorStream()) {

					final String content = IOUtils.toString(is, "utf-8");

					logger.warn("Error fetching WFS data: {}", content);

					throw new FrameworkException(GEOSERVER_ERROR_STATUS, content);
				}
			}

		} catch (FrameworkException fex) {

			// re-throw FrameworkException
			throw fex;

		} catch (Throwable t) {

			throw new FrameworkException(GEOSERVER_ERROR_STATUS, t.getMessage());
		}

		return data;
	}

	protected Map<String, Object> getWCSCoverageDescription(final String baseUrl, final String coverageId) {

		final Map<String, Object> data = new LinkedHashMap<>();

		try {

			final URL url                      = getWCSDescriptionUrl(baseUrl, coverageId);
			final HttpURLConnection connection = (HttpURLConnection)url.openConnection();

			connection.connect();

			final int statusCode = connection.getResponseCode();

			data.put("status", statusCode);

			if (statusCode == 200) {

				try (final InputStream is = connection.getInputStream()) {

					final String content = IOUtils.toString(is, "utf-8");
					final JSONObject obj = XML.toJSONObject(content);
					if (obj != null) {

						data.put("id",         getString(obj, "wcs:CoverageDescriptions.wcs:CoverageDescription.wcs:CoverageId"));
						data.put("fieldName",  getString(obj, "wcs:CoverageDescriptions.wcs:CoverageDescription.gmlcov:rangeType.swe:DataRecord.swe:field.name"));
						data.put("nilValue",   getDouble(obj, "wcs:CoverageDescriptions.wcs:CoverageDescription.gmlcov:rangeType.swe:DataRecord.swe:field.swe:Quantity.swe:nilValues.swe:NilValues.swe:nilValue.content"));
						data.put("axisLabels", getString(obj, "wcs:CoverageDescriptions.wcs:CoverageDescription.gml:boundedBy.gml:Envelope.axisLabels"));
					}
				}

			} else {

				try (final InputStream is = connection.getErrorStream()) {

					final String content = IOUtils.toString(is, "utf-8");
					final JSONObject obj = XML.toJSONObject(content);
					if (obj != null) {

						data.put("message",  getString(obj, "ows:ExceptionReport.ows:Exception.ows:ExceptionText"));
					}
				}
			}

		} catch (Throwable t) {
			data.put("message", t.getMessage());
		}

		return data;
	}

	protected GeometryAttribute getGeometryAttributeFromFeature(final Feature feature) {

		for (final String name : GEOMETRY_NAMES) {

			final Object value = feature.getProperty(name);
			if (value instanceof GeometryAttribute) {

				return (GeometryAttribute)value;
			}
		}

		return null;
	}

	protected Geometry getGeometryFromMap(final Map map) {

		for (final String name : GEOMETRY_NAMES) {

			final Object value = map.get(name);
			if (value instanceof Geometry) {

				return (Geometry)value;
			}
		}

		return null;
	}

	protected GridCoverage2D getWCSCoverage(final String baseUrl, final String coverageId, final Geometry subset) throws FrameworkException {

		try {

			final Map<String, Object> coverageDescription = getWCSCoverageDescription(baseUrl, coverageId);
			final int statusCode                          = get(coverageDescription, "status", 500);

			if (statusCode == 200) {

				final File tmpFile = File.createTempFile("structr", ".tiff");
				final URL url      = getWCSCoverageUrl(baseUrl, coverageId, subset, get(coverageDescription, "axisLabels", "X Y"));

				try {

					HttpHelper.streamURLToFile(url.toString(), tmpFile);

					final AbstractGridFormat format   = GridFormatFinder.findFormat(tmpFile);
					final GridCoverage2DReader reader = format.getReader(tmpFile, new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE));

					return (GridCoverage2D) reader.read(null);

				} finally {

					// remove tmp file after fetching the data
					tmpFile.delete();
				}

			} else {

				throw new FrameworkException(statusCode, get(coverageDescription, "message", "Error"));
			}

		} catch (Throwable t) {
			throw new FrameworkException(GEOSERVER_ERROR_STATUS, t.getMessage());
		}
	}

	protected double[] getExtrema(final Coverage coverage, final int sampleDimensionIndex) {

		final GridCoverage2D extrema = (GridCoverage2D)Operations.DEFAULT.extrema(coverage);

		final double[] min = (double[])extrema.getProperty("minimum");
		final double[] max = (double[])extrema.getProperty("maximum");

		return new double[] { min[0], max[0] };
	}

	protected Double getDouble(final JSONObject root, final String path) {

		final String[] parts = path.split("[\\.]+");
		JSONObject current   = root;

		for (final String part : parts) {

			final JSONObject candidate = current.optJSONObject(part);
			if (candidate == null) {

				return current.optDouble(part);

			} else {

				current = candidate;
			}
		}

		return null;
	}

	protected String getString(final JSONObject root, final String path) {

		final String[] parts = path.split("[\\.]+");
		JSONObject current   = root;

		for (final String part : parts) {

			final JSONObject candidate = current.optJSONObject(part);
			if (candidate == null) {

				return current.optString(part);

			} else {

				current = candidate;
			}
		}

		return null;
	}

	protected <T> T get(final Map<String, Object> data, final String key, final T defaultValue) {

		final Object value = data.get(key);
		if (value != null) {

			return (T)value;
		}

		return defaultValue;
	}
}