/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.entity.geo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.awt.Color;
import java.util.logging.Logger;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.geotools.data.Query;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.DefaultMapContext;
import org.geotools.map.MapContext;
import org.geotools.map.MapLayer;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.Symbolizer;
import org.neo4j.gis.spatial.geotools.data.Neo4jSpatialDataStore;
import org.neo4j.graphdb.GraphDatabaseService;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;

import org.structr.core.entity.StructrNode;
import org.structr.core.entity.User;
import org.structr.common.MapHelper;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.node.GraphDatabaseCommand;

/**
 *
 * @author axel
 */
public class Map extends StructrNode {

    private final static String ICON_SRC = "/images/map.png";
    private static final Logger logger = Logger.getLogger(Map.class.getName());
    public static final String SVG_CONTENT_KEY = "svgContent";
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
    public static final String SHAPEFILE_KEY = "shapeFile";
    public static final String LAYER_KEY = "layer";
    public static final String OPTIMIZE_FTS_RENDERING_KEY = "optimizeFtsRendering";
    public static final String LINE_WIDTH_OPTIMIZATION_KEY = "lineWidthOptimization";
    public static final String AUTO_ENVELOPE_KEY = "autoEnvelope";

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }

    @Override
    public void renderView(StringBuilder out, final StructrNode startNode,
            final String editUrl, final Long editNodeId, final User user) {

        if (editNodeId != null && getId() == editNodeId.longValue()) {

            renderEditFrame(out, editUrl);

        } else {

            if (isVisible()) {
                renderSVGMap(out);
            }
        }
    }

    public String getSvgContent() {
        return (String) getProperty(SVG_CONTENT_KEY);
    }

    public void setSvgContent(final String svgContent) {
        setProperty(SVG_CONTENT_KEY, svgContent);
    }

    private void renderSVGMap(StringBuilder out) {

        Command graphDbCommand = Services.command(GraphDatabaseCommand.class);
        GraphDatabaseService graphDb = (GraphDatabaseService) graphDbCommand.execute();

        MapContext mapContext = null;
        try {

            long t0 = System.currentTimeMillis();

            // get the feature name from the request
            String featureName = getRequest() != null ? getRequest().getParameter("name") : null;

            int cx = getCanvasX();
            int cy = getCanvasY();

            boolean auto = getAutoEnvelope();

            List<MapLayer> layers = new ArrayList<MapLayer>();
            MapLayer layer = null;
            ReferencedEnvelope envelope = null;

            String shapeFilePath = getShapeFile();
            if (shapeFilePath != null) {

                // open data store from shapefile
                File shapeFile = new File(shapeFilePath);
                ShapefileDataStore dataStore = new ShapefileDataStore(shapeFile.toURI().toURL());

                // build map layer with style
                StyleBuilder sb = new StyleBuilder();
                Symbolizer sym = sb.createLineSymbolizer(Color.decode(getLineColor()), getLineWidth());
                layer = new MapLayer(dataStore.getFeatureSource(), sb.createStyle(sym));
                layers.add(layer);

            }

            // open data store from neo4j database
            Neo4jSpatialDataStore n4jstore = new Neo4jSpatialDataStore(graphDb);

            String layerName = getLayer();
            if (StringUtils.isEmpty(layerName)) {
                logger.log(Level.SEVERE, "No layer name!");
            }

            SimpleFeatureSource featureSource = n4jstore.getFeatureSource(layerName);

            if (auto) {


                if (featureName == null) {

                    // if no feature name is given, show all features of layer
                    envelope = featureSource.getBounds();

                } else {

                    // first, find the feature which corresponds with the requested feature
                    // (or the name of the node, if the request value is empty)
                    List<Filter> filterList = new ArrayList<Filter>();
                    filterList.add(CQL.toFilter("NAME like '" + StringEscapeUtils.escapeSql(featureName) + "'"));
                    Filter filter = MapHelper.featureFactory.or(filterList);
                    Query query = new Query(layerName, filter);

                    SimpleFeatureCollection featureCollection = featureSource.getFeatures(query);

                    if (featureCollection != null && !(featureCollection.isEmpty())) {
                        SimpleFeature requestedFeature = featureCollection.features().next();
                        envelope = (ReferencedEnvelope) requestedFeature.getBounds();
                    }

                }


            } else {

                Double eminx = getEnvelopeMinX();
                Double emaxx = getEnvelopeMaxX();
                Double eminy = getEnvelopeMinY();
                Double emaxy = getEnvelopeMaxY();

                if (eminx != null && emaxx != null && eminy != null && emaxy != null) {

                    envelope = new ReferencedEnvelope(eminx, emaxx, eminy, emaxy, null);

                } else {
                    logger.log(Level.WARNING, "Manual envelope parameter incomplete");
                }
            }

            // Expand envelope as needed
            MapHelper.expandEnvelope(envelope, new Double(cx), new Double(cy));

            // search all features within this bounding
            SimpleFeatureCollection features = MapHelper.getIntersectingFeatures(graphDb, envelope, layerName);

            // create a style for displaying the polygons
            Symbolizer polygonSymbolizer = MapHelper.createPolygonSymbolizer(getLineColor(), getLineWidth(), getLineOpacity(), getFillColor(), getFillOpacity());
            Symbolizer textSymbolizer = MapHelper.createTextSymbolizer(getFontName(), getFontSize(), getFontColor(), getFontOpacity());


            Rule rule = MapHelper.styleFactory.createRule();
            rule.symbolizers().add(polygonSymbolizer);
            rule.symbolizers().add(textSymbolizer);
            FeatureTypeStyle fts = MapHelper.styleFactory.createFeatureTypeStyle(new Rule[]{rule});
            Style style = MapHelper.styleFactory.createStyle();
            style.featureTypeStyles().add(fts);

            // add features and style as a map layer to the list of map layers
            layers.add(new MapLayer(features, style));

            // create a map context
            mapContext = new DefaultMapContext(layers.toArray(new MapLayer[]{}));

            // render map to SVG
            MapHelper.renderSVGDocument(out, mapContext, envelope, cx, cy, getOptimizeFtsRendering(), getLineWidthOptimization());

            // clear map content
            mapContext.dispose();

            long t1 = System.currentTimeMillis();

            logger.log(Level.INFO, "SVG image successfully rendered in {0} ms", (t1 - t0));


        } catch (Throwable t) {
            logger.log(Level.SEVERE, "Error while rendering map to SVG", t);
        } finally {
            if (mapContext != null) {
                mapContext.dispose();
            }
        }
    }

// <editor-fold defaultstate="collapsed" desc="getter and setter methods">
// getter and setter methods
    public int getCanvasX() {
        return getIntProperty(CANVAS_X_KEY);
    }

    public int getCanvasY() {
        return getIntProperty(CANVAS_Y_KEY);
    }

    public double getEnvelopeMinX() {
        return getDoubleProperty(ENVELOPE_MIN_X_KEY);
    }

    public double getEnvelopeMinY() {
        return getDoubleProperty(ENVELOPE_MIN_Y_KEY);
    }

    public double getEnvelopeMaxX() {
        return getDoubleProperty(ENVELOPE_MAX_X_KEY);
    }

    public double getEnvelopeMaxY() {
        return getDoubleProperty(ENVELOPE_MAX_Y_KEY);
    }

    public String getShapeFile() {
        return (String) getProperty(SHAPEFILE_KEY);
    }

    public String getLayer() {
        return (String) getProperty(LAYER_KEY);
    }

    public int getLineWidth() {
        return getIntProperty(LINE_WIDTH_KEY);
    }

    public String getLineColor() {
        return (String) getProperty(LINE_COLOR_KEY);
    }

    public double getLineOpacity() {
        return getDoubleProperty(LINE_OPACITY_KEY);
    }

    public String getFillColor() {
        return (String) getProperty(FILL_COLOR_KEY);
    }

    public double getFillOpacity() {
        return getDoubleProperty(FILL_OPACITY_KEY);
    }

    public boolean getOptimizeFtsRendering() {
        return getBooleanProperty(OPTIMIZE_FTS_RENDERING_KEY);
    }

    public boolean getLineWidthOptimization() {
        return getBooleanProperty(LINE_WIDTH_OPTIMIZATION_KEY);
    }

    public boolean getAutoEnvelope() {
        return getBooleanProperty(AUTO_ENVELOPE_KEY);
    }

    public String getFontName() {
        return (String) getProperty(FONT_NAME_KEY);
    }

    public double getFontSize() {
        return getDoubleProperty(FONT_SIZE_KEY);
    }

    public String getFontColor() {
        return (String) getProperty(FONT_COLOR_KEY);
    }

    public double getFontOpacity() {
        return getDoubleProperty(FONT_OPACITY_KEY);
    }

    // ##############
    public void setCanvasX(final int value) {
        setProperty(CANVAS_X_KEY, value);
    }

    public void setCanvasY(final int value) {
        setProperty(CANVAS_Y_KEY, value);
    }

    public void setEnvelopeMinX(final double value) {
        setProperty(ENVELOPE_MIN_X_KEY, value);
    }

    public void setEnvelopeMinY(final double value) {
        setProperty(ENVELOPE_MIN_Y_KEY, value);
    }

    public void setEnvelopeMaxX(final double value) {
        setProperty(ENVELOPE_MAX_X_KEY, value);
    }

    public void setEnvelopeMaxY(final double value) {
        setProperty(ENVELOPE_MAX_Y_KEY, value);
    }

    public void setShapeFile(final String value) {
        setProperty(SHAPEFILE_KEY, value);
    }

    public void setLayer(final String value) {
        setProperty(LAYER_KEY, value);
    }

    public void setLineWidth(final int value) {
        setProperty(LINE_WIDTH_KEY, value);
    }

    public void setLineColor(final String value) {
        setProperty(LINE_COLOR_KEY, value);
    }

    public void setLineOpacity(final double value) {
        setProperty(LINE_OPACITY_KEY, value);
    }

    public void setFillColor(final String value) {
        setProperty(FILL_COLOR_KEY, value);
    }

    public void setFillOpacity(final double value) {
        setProperty(FILL_OPACITY_KEY, value);
    }

    public void setOptimizeFtsRendering(final boolean value) {
        setProperty(OPTIMIZE_FTS_RENDERING_KEY, value);
    }

    public void setLineWidthOptimization(final boolean value) {
        setProperty(LINE_WIDTH_OPTIMIZATION_KEY, value);
    }

    public void setAutoEnvelope(final boolean value) {
        setProperty(AUTO_ENVELOPE_KEY, value);
    }

    public void setFontName(final String value) {
        setProperty(FONT_NAME_KEY, value);
    }

    public void setFontSize(final double value) {
        setProperty(FONT_SIZE_KEY, value);
    }

    public void setFontColor(final String value) {
        setProperty(FONT_COLOR_KEY, value);
    }

    public void setFontOpacity(final double value) {
        setProperty(FONT_OPACITY_KEY, value);
    }
    // </editor-fold>
}
