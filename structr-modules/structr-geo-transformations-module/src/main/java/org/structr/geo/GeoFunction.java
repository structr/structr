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

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.DataUtilities;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.map.FeatureLayer;
import org.geotools.map.MapContent;
import org.geotools.styling.SLD;
import org.geotools.swing.JMapFrame;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.Function;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.List;
import java.util.Map;

public abstract class GeoFunction extends Function<Object, Object> {

	@Override
	public String getRequiredModule() {
		return "geo-transformations";
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Geocoding;
	}

	protected Coordinate getCoordinate(final Object source) {

		if (source instanceof Point) {

			return ((Point)source).getCoordinate();

		} else if (source instanceof Geometry) {

			return ((Geometry)source).getCoordinate();

		} else if (source instanceof Coordinate) {

			return (Coordinate)source;

		} else if (source instanceof List) {

			final List point = (List)source;
			final Object x   = point.get(0);
			final Object y   = point.get(1);

			final Double px  = this.getDoubleOrNull(x);
			final Double py  = this.getDoubleOrNull(y);

			if (px != null && py != null) {

				return new Coordinate(px, py);
			}

		} else if (source instanceof Map) {

			final Map point = (Map)source;

			if (point.containsKey("x") && point.containsKey("y")) {

				final Double px = this.getDoubleOrNull(point.get("x"));
				final Double py = this.getDoubleOrNull(point.get("y"));

				if (px != null && py != null) {

					return new Coordinate(px, py);
				}

			} else if (point.containsKey("latitude") && point.containsKey("longitude")) {

				final Double plat = this.getDoubleOrNull(point.get("latitude"));
				final Double plon = this.getDoubleOrNull(point.get("longitude"));

				if (plat != null && plon != null) {

					return new Coordinate(plat, plon);
				}

			} else if (point.containsKey("lat") && point.containsKey("lon")) {

				final Double plat = this.getDoubleOrNull(point.get("lat"));
				final Double plon = this.getDoubleOrNull(point.get("lon"));

				if (plat != null && plon != null) {

					return new Coordinate(plat, plon);
				}

			} else {

				logger.warn("Unknown coordinate object, don't know how to handle {}, ignoring", source);
			}

		} else {

			logger.warn("Unknown coordinate object, don't know how to handle {}, ignoring", source);
		}

		return null;
	}

	protected void showCoverage(final int frameWidth, final int frameHeight, final GridCoverage2D coverage) {

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

		frame.setPreferredSize(new Dimension(frameWidth, frameHeight));
		frame.pack();
		frame.setVisible(true);

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	protected void showGeometries(final int frameWidth, final int frameHeight, final Geometry... geometries) {

		try {

			final Color[] colors     = new Color[] { Color.RED, Color.BLUE, Color.GREEN, Color.BLACK, Color.GRAY, Color.PINK };
			final MapContent content = new MapContent();
			final JMapFrame frame    = new JMapFrame(content);
			int index                = 0;

			for (final Geometry geometry : geometries) {

				final SimpleFeatureType TYPE = DataUtilities.createType("test" + index, "geom:" + geometry.getClass().getSimpleName());
				final DefaultFeatureCollection featureCollection = new DefaultFeatureCollection("internal" + index, TYPE);

				featureCollection.add(SimpleFeatureBuilder.build(TYPE, new Object[] { geometry }, null));

				content.addLayer(new FeatureLayer(featureCollection, SLD.createLineStyle(colors[index], 1)));

				index++;
			}

			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setPreferredSize(new Dimension(frameWidth, frameHeight));
			frame.pack();
			frame.setVisible(true);

		} catch (Throwable t) {
			logger.error(ExceptionUtils.getStackTrace(t));
		}
	}

	protected void showFeatures(final int frameWidth, final int frameHeight, final FeatureCollection features) {

		try {

			final Color[] colors     = new Color[] { Color.RED, Color.BLUE, Color.GREEN, Color.BLACK, Color.GRAY, Color.PINK };
			final MapContent content = new MapContent();
			final JMapFrame frame    = new JMapFrame(content);
			int index                = 0;

			final FeatureIterator it = features.features();
			while (it.hasNext()) {

				final SimpleFeature feature      = (SimpleFeature)it.next();
				final DefaultFeatureCollection c = new DefaultFeatureCollection();
				c.add(feature);

				content.addLayer(new FeatureLayer(c, SLD.createLineStyle(colors[index++], 1)));
			}

			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setPreferredSize(new Dimension(frameWidth, frameHeight));
			frame.pack();
			frame.setVisible(true);

		} catch (Throwable t) {
			logger.error(ExceptionUtils.getStackTrace(t));
		}
	}
}
