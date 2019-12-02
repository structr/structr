/**
 * Copyright (C) 2010-2019 Structr GmbH
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
import com.vividsolutions.jts.geom.Polygon;
import java.awt.BorderLayout;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.coverage.processing.Operations;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.Hints;
import org.geotools.geometry.jts.JTS;
import org.geotools.process.raster.PolygonExtractionProcess;
import org.geotools.referencing.CRS;
import org.jaitools.numeric.Range;
import org.opengis.coverage.Coverage;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.structr.rest.common.HttpHelper;

public abstract class AbstractWCSDataFunction extends GeoFunction {

	protected URL getWCSUrl(final String baseUrl, final String coverageId) throws MalformedURLException {

		final StringBuilder buf = new StringBuilder(baseUrl);

		buf.append("&service=WCS");
		buf.append("&request=GetCoverage");
		buf.append("&coverageId=").append(coverageId);
		buf.append("&format=image/tiff");
		buf.append("&version=2.0.1");
		buf.append("&subset=X(240373.6329517475,254330.02993994422)");
		buf.append("&subset=Y(435015.8480670558,451025.2900452074)");

		return new URL(buf.toString());
	}

	protected List<List<List<Double>>> getFilteredCoveragePoints(final String baseUrl, final String coverageId, final double min, final double max) {

		final List<List<List<Double>>> result = new LinkedList<>();

		for (final Geometry geometry : getFilteredCoverageGeometries(baseUrl, coverageId, min, max)) {

			if (geometry instanceof Polygon) {

				for (final Polygon polygon : JTS.makeValid((Polygon)geometry, true)) {

					final List<List<Double>> list = new LinkedList<>();

					for (final Coordinate c : polygon.getCoordinates()) {

						list.add(Arrays.asList(c.x, c.y));
					}

					result.add(list);
				}
			}
		}

		return result;
	}

	protected List<Geometry> getFilteredCoverageGeometries(final String baseUrl, final String coverageId, final double min, final double max) {

		final List<Geometry> result = new LinkedList<>();

		try {

			final GridCoverage2D coverage          = getWCSCoverage(baseUrl, coverageId);
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

		} catch (Throwable t) {

			t.printStackTrace();
		}

		return result;
	}

	protected GridCoverage2D getWCSCoverage(final String baseUrl, final String coverageId) {

		try {

			final File tmpFile = File.createTempFile("structr", ".tiff");
			final URL url      = getWCSUrl(baseUrl, coverageId);

			try {

				HttpHelper.streamURLToFile(url.toString(), tmpFile);

				final AbstractGridFormat format   = GridFormatFinder.findFormat(tmpFile);
				final GridCoverage2DReader reader = format.getReader(tmpFile, new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE));

				return (GridCoverage2D) reader.read(null);

			} finally {

				// remove tmp file after fetching the data
				tmpFile.delete();
			}

		} catch (Throwable t) {

			t.printStackTrace();
		}

		return null;
	}

	protected double[] getExtrema(final Coverage coverage, final int sampleDimensionIndex) {

		final GridCoverage2D extrema = (GridCoverage2D)Operations.DEFAULT.extrema(coverage);

		final double[] min = (double[])extrema.getProperty("minimum");
		final double[] max = (double[])extrema.getProperty("maximum");

		return new double[] { min[0], max[0] };
	}

	protected void showJFrame(final GridCoverage2D coverage) {

		final RenderedImage image           = coverage.getRenderedImage();
		final int type                      = BufferedImage.TYPE_BYTE_INDEXED;

		final BufferedImage src = new BufferedImage(image.getWidth(), image.getHeight(), type);
		src.setData(image.getData());

		final AffineTransform at = new AffineTransform();
		at.scale(20.0, 20.0);

		final AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
		final BufferedImage dst         = scaleOp.filter(src, null);
		final JFrame frame              = new JFrame();

		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(new JLabel(new ImageIcon(dst)));

		frame.pack();
		frame.setVisible(true);

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
}