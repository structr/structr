/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.entity.geo;

import java.util.logging.Logger;
import org.structr.common.RenderMode;
import org.structr.core.NodeRenderer;
import org.structr.core.entity.AbstractNode;
import org.structr.renderer.MapRenderer;

/**
 *
 * @author axel
 */
public class Map extends AbstractNode
{
	@Override
	public void initializeRenderers(java.util.Map<RenderMode, NodeRenderer> renderers)
	{
		renderers.put(RenderMode.Default, new MapRenderer());
	}

	@Override
	public String getIconSrc()
	{
		return ICON_SRC;
	}

	// <editor-fold defaultstate="collapsed" desc="getter and setter methods">
// getter and setter methods
//    public String getContentType() {
//        return (String) getProperty(CONTENT_TYPE_KEY);
//    }
	public int getCanvasX()
	{
		return getIntProperty(CANVAS_X_KEY);
	}

	public int getCanvasY()
	{
		return getIntProperty(CANVAS_Y_KEY);
	}

	public double getEnvelopeMinX()
	{
		return getDoubleProperty(ENVELOPE_MIN_X_KEY);
	}

	public double getEnvelopeMinY()
	{
		return getDoubleProperty(ENVELOPE_MIN_Y_KEY);
	}

	public double getEnvelopeMaxX()
	{
		return getDoubleProperty(ENVELOPE_MAX_X_KEY);
	}

	public double getEnvelopeMaxY()
	{
		return getDoubleProperty(ENVELOPE_MAX_Y_KEY);
	}

	public String getShapeFile()
	{
		return (String)getProperty(SHAPEFILE_KEY);
	}

	public String getLayer()
	{
		return (String)getProperty(LAYER_KEY);
	}

	public int getLineWidth()
	{
		return getIntProperty(LINE_WIDTH_KEY);
	}

	public String getLineColor()
	{
		return (String)getProperty(LINE_COLOR_KEY);
	}

	public double getLineOpacity()
	{
		return getDoubleProperty(LINE_OPACITY_KEY);
	}

	public String getFillColor()
	{
		return (String)getProperty(FILL_COLOR_KEY);
	}

	public double getFillOpacity()
	{
		return getDoubleProperty(FILL_OPACITY_KEY);
	}

	public boolean getOptimizeFtsRendering()
	{
		return getBooleanProperty(OPTIMIZE_FTS_RENDERING_KEY);
	}

	public boolean getLineWidthOptimization()
	{
		return getBooleanProperty(LINE_WIDTH_OPTIMIZATION_KEY);
	}

	public boolean getAutoEnvelope()
	{
		return getBooleanProperty(AUTO_ENVELOPE_KEY);
	}

	public boolean getStatic()
	{
		return getBooleanProperty(STATIC_KEY);
	}

	public boolean getDontCache()
	{
		return getBooleanProperty(DONT_CACHE_KEY);
	}

	public boolean getDisplayCities()
	{
		return getBooleanProperty(DISPLAY_CITIES_KEY);
	}

	public String getFontName()
	{
		return (String)getProperty(FONT_NAME_KEY);
	}

	public double getFontSize()
	{
		return getDoubleProperty(FONT_SIZE_KEY);
	}

	public String getFontColor()
	{
		return (String)getProperty(FONT_COLOR_KEY);
	}

	public double getFontOpacity()
	{
		return getDoubleProperty(FONT_OPACITY_KEY);
	}

	public double getAnchorX()
	{
		return getDoubleProperty(LABEL_ANCHOR_X_KEY);
	}

	public double getAnchorY()
	{
		return getDoubleProperty(LABEL_ANCHOR_Y_KEY);
	}

	public double getDisplacementX()
	{
		return getDoubleProperty(LABEL_DISPLACEMENT_X_KEY);
	}

	public double getDisplacementY()
	{
		return getDoubleProperty(LABEL_DISPLACEMENT_X_KEY);
	}

	public String getStaticFeatureName()
	{
		return (String)getProperty(STATIC_FEATURE_NAME_KEY);
	}

	public String getFeatureNameParamName()
	{
		return (String)getProperty(FEATURE_NAME_PARAM_NAME_KEY);
	}

	public String getSvgContent()
	{
		return (String)getProperty(SVG_CONTENT_KEY);
	}

	public double getLabelAnchorX()
	{
		return getDoubleProperty(LABEL_ANCHOR_X_KEY);
	}

	public double getLabelAnchorY()
	{
		return getDoubleProperty(LABEL_ANCHOR_Y_KEY);
	}

	public double getLabelDisplacementX()
	{
		return getDoubleProperty(LABEL_DISPLACEMENT_X_KEY);
	}

	public double getLabelDisplacementY()
	{
		return getDoubleProperty(LABEL_DISPLACEMENT_Y_KEY);
	}

	public String getPointShape()
	{
		return getStringProperty(POINT_SHAPE_KEY);
	}

	public int getPointDiameter()
	{
		return getIntProperty(POINT_DIAMETER_KEY);
	}

	public String getPointStrokeColor()
	{
		return getStringProperty(POINT_STROKE_COLOR_KEY);
	}

	public int getPointStrokeLineWidth()
	{
		return getIntProperty(POINT_STROKE_LINE_WIDTH_KEY);
	}

	public String getPointFillColor()
	{
		return getStringProperty(POINT_FILL_COLOR_KEY);
	}

	public double getPointFillOpacity()
	{
		return getDoubleProperty(POINT_FILL_OPACITY_KEY);
	}

	public String getPointFontName()
	{
		return getStringProperty(POINT_FONT_NAME_KEY);
	}

	public double getPointFontSize()
	{
		return getDoubleProperty(POINT_FONT_SIZE_KEY);
	}

	public String getPointFontColor()
	{
		return getStringProperty(POINT_FONT_COLOR_KEY);
	}

	public double getPointFontOpacity()
	{
		return getDoubleProperty(POINT_FONT_OPACITY_KEY);
	}

	//########################################
	public void setContentType(final String contentType)
	{
		setProperty(CONTENT_TYPE_KEY, contentType);
	}

	public void setCanvasX(final int value)
	{
		setProperty(CANVAS_X_KEY, value);
	}

	public void setCanvasY(final int value)
	{
		setProperty(CANVAS_Y_KEY, value);
	}

	public void setEnvelopeMinX(final double value)
	{
		setProperty(ENVELOPE_MIN_X_KEY, value);
	}

	public void setEnvelopeMinY(final double value)
	{
		setProperty(ENVELOPE_MIN_Y_KEY, value);
	}

	public void setEnvelopeMaxX(final double value)
	{
		setProperty(ENVELOPE_MAX_X_KEY, value);
	}

	public void setEnvelopeMaxY(final double value)
	{
		setProperty(ENVELOPE_MAX_Y_KEY, value);
	}

	public void setShapeFile(final String value)
	{
		setProperty(SHAPEFILE_KEY, value);
	}

	public void setLayer(final String value)
	{
		setProperty(LAYER_KEY, value);
	}

	public void setLineWidth(final int value)
	{
		setProperty(LINE_WIDTH_KEY, value);
	}

	public void setLineColor(final String value)
	{
		setProperty(LINE_COLOR_KEY, value);
	}

	public void setLineOpacity(final double value)
	{
		setProperty(LINE_OPACITY_KEY, value);
	}

	public void setFillColor(final String value)
	{
		setProperty(FILL_COLOR_KEY, value);
	}

	public void setFillOpacity(final double value)
	{
		setProperty(FILL_OPACITY_KEY, value);
	}

	public void setOptimizeFtsRendering(final boolean value)
	{
		setProperty(OPTIMIZE_FTS_RENDERING_KEY, value);
	}

	public void setLineWidthOptimization(final boolean value)
	{
		setProperty(LINE_WIDTH_OPTIMIZATION_KEY, value);
	}

	public void setAutoEnvelope(final boolean value)
	{
		setProperty(AUTO_ENVELOPE_KEY, value);
	}

	public void setStatic(final boolean value)
	{
		setProperty(STATIC_KEY, value);
	}

	public void setDontCache(final boolean value)
	{
		setProperty(DONT_CACHE_KEY, value);
	}

	public void setDisplayCities(final boolean value)
	{
		setProperty(DISPLAY_CITIES_KEY, value);
	}

	public void setFontName(final String value)
	{
		setProperty(FONT_NAME_KEY, value);
	}

	public void setFontSize(final double value)
	{
		setProperty(FONT_SIZE_KEY, value);
	}

	public void setFontColor(final String value)
	{
		setProperty(FONT_COLOR_KEY, value);
	}

	public void setFontOpacity(final double value)
	{
		setProperty(FONT_OPACITY_KEY, value);
	}

	public void setStaticFeatureName(final String value)
	{
		setProperty(STATIC_FEATURE_NAME_KEY, value);
	}

	public void setFeatureNameParamName(final String value)
	{
		setProperty(FEATURE_NAME_PARAM_NAME_KEY, value);
	}

	public void setSvgContent(final String svgContent)
	{
		setProperty(SVG_CONTENT_KEY, svgContent);
	}

	public void setLabelAnchorX(final double value)
	{
		setProperty(LABEL_ANCHOR_X_KEY, value);
	}

	public void setLabelAnchorY(final double value)
	{
		setProperty(LABEL_ANCHOR_Y_KEY, value);
	}

	public void setLabelDisplacementX(final double value)
	{
		setProperty(LABEL_DISPLACEMENT_X_KEY, value);
	}

	public void setLabelDisplacementY(final double value)
	{
		setProperty(LABEL_DISPLACEMENT_Y_KEY, value);
	}

	public void setPointShape(final String value)
	{
		setProperty(POINT_SHAPE_KEY, value);
	}

	public void setPointDiameter(final int value)
	{
		setProperty(POINT_DIAMETER_KEY, value);
	}

	public void setPointStrokeColor(final String value)
	{
		setProperty(POINT_STROKE_COLOR_KEY, value);
	}

	public void setPointStrokeLineWidth(final int value)
	{
		setProperty(POINT_STROKE_LINE_WIDTH_KEY, value);
	}

	public void setPointFillColor(final String value)
	{
		setProperty(POINT_FILL_COLOR_KEY, value);
	}

	public void setPointFillOpacity(final double value)
	{
		setProperty(POINT_FILL_OPACITY_KEY, value);
	}

	public void setPointFontName(final String value)
	{
		setProperty(POINT_FONT_NAME_KEY, value);
	}

	public void setPointFontSize(final double value)
	{
		setProperty(POINT_FONT_SIZE_KEY, value);
	}

	public void setPointFontColor(final String value)
	{
		setProperty(POINT_FONT_COLOR_KEY, value);
	}

	public void setPointFontOpacity(final double value)
	{
		setProperty(POINT_FONT_OPACITY_KEY, value);
	}
	// </editor-fold>

	// Attributes
	// <editor-fold defaultstate="collapsed" desc="Attributes">
	private final static String ICON_SRC = "/images/map.png";
	private static final Logger logger = Logger.getLogger(Map.class.getName());
	public final static String defaultFeatureParamName = "name";
	public static final String SVG_CONTENT_KEY = "svgContent";
	public final static String CONTENT_TYPE_KEY = "contentType";
	public static final String ENVELOPE_MIN_X_KEY = "envelopeMinX";
	public static final String ENVELOPE_MAX_X_KEY = "envelopeMaxX";
	public static final String ENVELOPE_MIN_Y_KEY = "envelopeMinY";
	public static final String ENVELOPE_MAX_Y_KEY = "envelopeMaxY";
	public static final String CANVAS_X_KEY = "canvasX";
	public static final String CANVAS_Y_KEY = "canvasY";
	public static final String LINE_COLOR_KEY = "lineColor";
	public static final String LINE_WIDTH_KEY = "lineWidth";
	public static final String LINE_OPACITY_KEY = "lineOpacity";
	public static final String FILL_COLOR_KEY = "fillColor";
	public static final String FILL_OPACITY_KEY = "fillOpacity";
	public static final String FONT_NAME_KEY = "fontName";
	public static final String FONT_SIZE_KEY = "fontSize";
	public static final String FONT_COLOR_KEY = "fontColor";
	public static final String FONT_OPACITY_KEY = "fontOpacity";
	public static final String LABEL_ANCHOR_X_KEY = "labelAnchorX";
	public static final String LABEL_ANCHOR_Y_KEY = "labelAnchorY";
	public static final String LABEL_DISPLACEMENT_X_KEY = "labelDisplacementX";
	public static final String LABEL_DISPLACEMENT_Y_KEY = "labelDisplacementY";
	public static final String SHAPEFILE_KEY = "shapeFile";
	public static final String POINT_SHAPE_KEY = "pointShape";
	public static final String POINT_DIAMETER_KEY = "pointDiameter";
	public static final String POINT_STROKE_COLOR_KEY = "pointStrokeColor";
	public static final String POINT_STROKE_LINE_WIDTH_KEY = "pointStrokeLineWidth";
	public static final String POINT_FILL_COLOR_KEY = "pointFillColor";
	public static final String POINT_FILL_OPACITY_KEY = "pointFillOpacity";
	public static final String POINT_FONT_NAME_KEY = "pointFontName";
	public static final String POINT_FONT_SIZE_KEY = "pointFontSize";
	public static final String POINT_FONT_COLOR_KEY = "pointFontColor";
	public static final String POINT_FONT_OPACITY_KEY = "pointFontOpacity";
	public static final String LAYER_KEY = "layer";
	public static final String OPTIMIZE_FTS_RENDERING_KEY = "optimizeFtsRendering";
	public static final String LINE_WIDTH_OPTIMIZATION_KEY = "lineWidthOptimization";
	public static final String AUTO_ENVELOPE_KEY = "autoEnvelope";
	public static final String FEATURE_NAME_PARAM_NAME_KEY = "featureNameParamName";
	public static final String STATIC_FEATURE_NAME_KEY = "staticFeatureName";
	public static final String STATIC_KEY = "static"; // Don't take request parameters into account
	public static final String DONT_CACHE_KEY = "dontCache";
	public static final String DISPLAY_CITIES_KEY = "displayCities";
	// </editor-fold>
}
