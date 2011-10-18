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

import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RenderMode;
import org.structr.core.EntityContext;
import org.structr.core.NodeRenderer;
import org.structr.core.entity.AbstractNode;
import org.structr.renderer.MapRenderer;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author axel
 */
public class Map extends AbstractNode {

	public final static String defaultFeatureParamName = "name";
	private static final Logger logger                 = Logger.getLogger(Map.class.getName());

	//~--- static initializers --------------------------------------------

	static {

		EntityContext.registerPropertySet(Map.class,
						  PropertyView.All,
						  Key.values());
	}

	//~--- constant enums -------------------------------------------------

	public enum Key implements PropertyKey {

		autoEnvelope, canvasX, canvasY, contentType, displayCities, dontCache, envelopeMaxX, envelopeMaxY,
		envelopeMinX, envelopeMinY, featureNameParamName, fillColor, fillOpacity, fontColor, fontName,
		fontOpacity, fontSize, labelAnchorX, labelAnchorY, labelDisplacementX, labelDisplacementY, layer,
		lineColor, lineOpacity, lineWidth, lineWidthOptimization, optimizeFtsRendering, pointDiameter,
		pointFillColor, pointFillOpacity, pointFontColor, pointFontName, pointFontOpacity, pointFontSize,
		pointShape, pointStrokeColor, pointStrokeLineWidth, shapeFile, staticFeatureName, isStatic, svgContent;
	}

	//~--- methods --------------------------------------------------------

	@Override
	public void initializeRenderers(java.util.Map<RenderMode, NodeRenderer> renderers) {

		renderers.put(RenderMode.Default,
			      new MapRenderer());
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getIconSrc() {
		return "/images/map.png";
	}

	// <editor-fold defaultstate="collapsed" desc="getter and setter methods">
	// getter and setter methods
	@Override
	public String getContentType() {
		return (String) getProperty(Key.contentType.name());
	}

	public int getCanvasX() {
		return getIntProperty(Key.canvasX.name());
	}

	public int getCanvasY() {
		return getIntProperty(Key.canvasY.name());
	}

	public double getEnvelopeMinX() {
		return getDoubleProperty(Key.envelopeMinX.name());
	}

	public double getEnvelopeMinY() {
		return getDoubleProperty(Key.envelopeMinY.name());
	}

	public double getEnvelopeMaxX() {
		return getDoubleProperty(Key.envelopeMaxX.name());
	}

	public double getEnvelopeMaxY() {
		return getDoubleProperty(Key.envelopeMaxY.name());
	}

	public String getShapeFile() {
		return (String) getProperty(Key.shapeFile.name());
	}

	public String getLayer() {
		return (String) getProperty(Key.layer.name());
	}

	public int getLineWidth() {
		return getIntProperty(Key.lineWidth.name());
	}

	public String getLineColor() {
		return (String) getProperty(Key.lineColor.name());
	}

	public double getLineOpacity() {
		return getDoubleProperty(Key.lineOpacity.name());
	}

	public String getFillColor() {
		return (String) getProperty(Key.fillColor.name());
	}

	public double getFillOpacity() {
		return getDoubleProperty(Key.fillOpacity.name());
	}

	public boolean getOptimizeFtsRendering() {
		return getBooleanProperty(Key.optimizeFtsRendering.name());
	}

	public boolean getLineWidthOptimization() {
		return getBooleanProperty(Key.lineWidthOptimization.name());
	}

	public boolean getAutoEnvelope() {
		return getBooleanProperty(Key.autoEnvelope.name());
	}

	public boolean getStatic() {
		return getBooleanProperty(Key.isStatic.name());
	}

	public boolean getDontCache() {
		return getBooleanProperty(Key.dontCache.name());
	}

	public boolean getDisplayCities() {
		return getBooleanProperty(Key.displayCities.name());
	}

	public String getFontName() {
		return (String) getProperty(Key.fontName.name());
	}

	public double getFontSize() {
		return getDoubleProperty(Key.fontSize.name());
	}

	public String getFontColor() {
		return (String) getProperty(Key.fontColor.name());
	}

      public double getFontOpacity() {
              return getDoubleProperty(Key.fontOpacity.name());
      }

	public String getStaticFeatureName() {
		return (String) getProperty(Key.staticFeatureName.name());
	}

	public String getFeatureNameParamName() {
		return (String) getProperty(Key.featureNameParamName.name());
	}

	public String getSvgContent() {
		return (String) getProperty(Key.svgContent.name());
	}

	public double getLabelAnchorX() {
		return getDoubleProperty(Key.labelAnchorX.name());
	}

	public double getLabelAnchorY() {
		return getDoubleProperty(Key.labelAnchorY.name());
	}

	public double getLabelDisplacementX() {
		return getDoubleProperty(Key.labelDisplacementX.name());
	}

	public double getLabelDisplacementY() {
		return getDoubleProperty(Key.labelDisplacementY.name());
	}

	public String getPointShape() {
		return getStringProperty(Key.pointShape.name());
	}

	public int getPointDiameter() {
		return getIntProperty(Key.pointDiameter.name());
	}

	public String getPointStrokeColor() {
		return getStringProperty(Key.pointStrokeColor.name());
	}

	public int getPointStrokeLineWidth() {
		return getIntProperty(Key.pointStrokeLineWidth.name());
	}

	public String getPointFillColor() {
		return getStringProperty(Key.pointFillColor.name());
	}

	public double getPointFillOpacity() {
		return getDoubleProperty(Key.pointFillOpacity.name());
	}

	public String getPointFontName() {
		return getStringProperty(Key.pointFontName.name());
	}

	public double getPointFontSize() {
		return getDoubleProperty(Key.pointFontSize.name());
	}

	public String getPointFontColor() {
		return getStringProperty(Key.pointFontColor.name());
	}

	public double getPointFontOpacity() {
		return getDoubleProperty(Key.pointFontOpacity.name());
	}

	//~--- set methods ----------------------------------------------------

	public void setContentType(final String contentType) {

		setProperty(Key.contentType.name(),
			    contentType);
	}

	public void setCanvasX(final int value) {

		setProperty(Key.canvasX.name(),
			    value);
	}

	public void setCanvasY(final int value) {

		setProperty(Key.canvasY.name(),
			    value);
	}

	public void setEnvelopeMinX(final double value) {

		setProperty(Key.envelopeMinX.name(),
			    value);
	}

	public void setEnvelopeMinY(final double value) {

		setProperty(Key.envelopeMinY.name(),
			    value);
	}

	public void setEnvelopeMaxX(final double value) {

		setProperty(Key.envelopeMaxX.name(),
			    value);
	}

	public void setEnvelopeMaxY(final double value) {

		setProperty(Key.envelopeMaxY.name(),
			    value);
	}

	public void setShapeFile(final String value) {

		setProperty(Key.shapeFile.name(),
			    value);
	}

	public void setLayer(final String value) {

		setProperty(Key.layer.name(),
			    value);
	}

	public void setLineWidth(final int value) {

		setProperty(Key.lineWidth.name(),
			    value);
	}

	public void setLineColor(final String value) {

		setProperty(Key.lineColor.name(),
			    value);
	}

	public void setLineOpacity(final double value) {

		setProperty(Key.lineOpacity.name(),
			    value);
	}

	public void setFillColor(final String value) {

		setProperty(Key.fillColor.name(),
			    value);
	}

	public void setFillOpacity(final double value) {

		setProperty(Key.fillOpacity.name(),
			    value);
	}

	public void setOptimizeFtsRendering(final boolean value) {

		setProperty(Key.optimizeFtsRendering.name(),
			    value);
	}

	public void setLineWidthOptimization(final boolean value) {

		setProperty(Key.lineWidthOptimization.name(),
			    value);
	}

	public void setAutoEnvelope(final boolean value) {

		setProperty(Key.autoEnvelope.name(),
			    value);
	}

	public void setStatic(final boolean value) {

		setProperty(Key.isStatic.name(),
			    value);
	}

	public void setDontCache(final boolean value) {

		setProperty(Key.dontCache.name(),
			    value);
	}

	public void setDisplayCities(final boolean value) {

		setProperty(Key.displayCities.name(),
			    value);
	}

	public void setFontName(final String value) {

		setProperty(Key.fontName.name(),
			    value);
	}

	public void setFontSize(final double value) {

		setProperty(Key.fontSize.name(),
			    value);
	}

	public void setFontColor(final String value) {

		setProperty(Key.fontColor.name(),
			    value);
	}

	public void setFontOpacity(final double value) {

		setProperty(Key.fontOpacity.name(),
			    value);
	}

	public void setStaticFeatureName(final String value) {

		setProperty(Key.staticFeatureName.name(),
			    value);
	}

	public void setFeatureNameParamName(final String value) {

		setProperty(Key.featureNameParamName.name(),
			    value);
	}

	public void setSvgContent(final String svgContent) {

		setProperty(Key.svgContent.name(),
			    svgContent);
	}


      public void setLabelAnchorX(final double value) {

              setProperty(Key.labelAnchorX.name(),
                          value);
      }

      public void setLabelAnchorY(final double value) {

              setProperty(Key.labelAnchorY.name(),
                          value);
      }

      public void setLabelDisplacementX(final double value) {

              setProperty(Key.labelDisplacementX.name(),
                          value);
      }

      public void setLabelDisplacementY(final double value) {

              setProperty(Key.labelDisplacementY.name(),
                          value);
      }

      public void setPointShape(final String value) {

              setProperty(Key.pointShape.name(),
                          value);
      }

      public void setPointDiameter(final int value) {

              setProperty(Key.pointDiameter.name(),
                          value);
      }

      public void setPointStrokeColor(final String value) {

              setProperty(Key.pointStrokeColor.name(),
                          value);
      }

      public void setPointStrokeLineWidth(final int value) {

              setProperty(Key.pointStrokeLineWidth.name(),
                          value);
      }

      public void setPointFillColor(final String value) {

              setProperty(Key.pointFillColor.name(),
                          value);
      }

      public void setPointFillOpacity(final double value) {

              setProperty(Key.pointFillOpacity.name(),
                          value);
      }

      public void setPointFontName(final String value) {

              setProperty(Key.pointFontName.name(),
                          value);
      }

      public void setPointFontSize(final double value) {

              setProperty(Key.pointFontSize.name(),
                          value);
      }

      public void setPointFontColor(final String value) {

              setProperty(Key.pointFontColor.name(),
                          value);
      }

      public void setPointFontOpacity(final double value) {

              setProperty(Key.pointFontOpacity.name(),
                          value);
      }

	// </editor-fold>
	// </editor-fold>
}
