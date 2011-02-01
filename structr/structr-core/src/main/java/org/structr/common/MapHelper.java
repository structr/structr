/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.common;

import com.vividsolutions.jts.geom.Envelope;
import java.awt.BasicStroke;
import java.util.Map;
import org.geotools.styling.Fill;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.StyleFactory;

import org.geotools.styling.AnchorPoint;
import org.geotools.styling.Displacement;
import org.geotools.styling.Font;
import org.geotools.styling.LabelPlacement;
import org.geotools.styling.TextSymbolizer;
import org.neo4j.gis.spatial.Layer;
import org.neo4j.gis.spatial.Search;
import org.neo4j.gis.spatial.SpatialDatabaseRecord;
import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.gis.spatial.SpatialIndexReader;
import org.neo4j.gis.spatial.query.SearchIntersectWindow;
import org.neo4j.graphdb.GraphDatabaseService;
import org.opengis.filter.expression.PropertyName;

import java.awt.Color;
import java.util.logging.Logger;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.batik.svggen.DOMGroupManager;
import org.apache.batik.svggen.SVGGeneratorContext;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.commons.lang.StringEscapeUtils;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.MapContext;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.Symbolizer;
import org.neo4j.gis.spatial.geotools.data.Neo4jSpatialDataStore;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.structr.core.entity.geo.MetaDataShape;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author axel
 */
public abstract class MapHelper {

    private static final Logger logger = Logger.getLogger(MapHelper.class.getName());
    public static FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory(null);
    public static StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(null);
    public static FilterFactory featureFactory = CommonFactoryFinder.getFilterFactory(null);

    /**
     * Create a Symbolizer to draw polygon features with given line and fill style
     */
    public static Symbolizer createPolygonSymbolizer(final String lineColor, final int lineWidth, final double lineOpacity,
            final String fillColor, final double fillOpacity) {

        // create a partially opaque outline stroke
        org.geotools.styling.Stroke stroke = styleFactory.createStroke(
                filterFactory.literal(Color.decode(lineColor)),
                filterFactory.literal(lineWidth),
                filterFactory.literal(lineOpacity));

        // create a partial opaque fill
        Fill fill = styleFactory.createFill(
                filterFactory.literal(Color.decode(fillColor)),
                filterFactory.literal(fillOpacity));

        /*
         * Setting the geometryPropertyName arg to null signals that we want to
         * draw the default geometry of features
         */
        PolygonSymbolizer sym = styleFactory.createPolygonSymbolizer(stroke, fill, null);

        return sym;
    }

    /**
     * Create a Symbolizer to draw label text
     */
    public static Symbolizer createTextSymbolizer(final String fontName, final double fontSize,
            final String fontColor, final double fontOpacity) {

        Font gtFont = styleFactory.createFont(
                filterFactory.literal(fontName),
                filterFactory.literal(Font.Style.NORMAL),
                filterFactory.literal(Font.Weight.BOLD),
                filterFactory.literal(fontSize));

        AnchorPoint ap = styleFactory.createAnchorPoint(
                filterFactory.literal(0.5),
                filterFactory.literal(0.0));

        Displacement dp = styleFactory.createDisplacement(
                filterFactory.literal(0.0),
                filterFactory.literal(0.0));

        LabelPlacement placement = styleFactory.createPointPlacement(
                ap,
                dp,
                filterFactory.literal(0.0));

        PropertyName pn = filterFactory.property("NAME");

        // create a partial opaque fill
        Fill fill = styleFactory.createFill(
                filterFactory.literal(Color.decode(fontColor)),
                filterFactory.literal(fontOpacity));

        TextSymbolizer sym = styleFactory.createTextSymbolizer(fill, new Font[]{gtFont}, null, pn, placement, null);




        return sym;
    }

    /**
     * Convert a BoundingBox to Envelope
     *
     * @param bbox
     * @return
     */
//    public static Envelope envelope(final BoundingBox bbox) {
//        return new Envelope(
//                bbox.getMinX(), bbox.getMaxX(), bbox.getMinX(), bbox.getMaxY());
//    }
    /**
     * Expand envelope to fit nicely into canvas
     * 
     * @param envelope
     * @param canvasX
     * @param canvasY
     * @return
     */
    public static Envelope expandEnvelope(final Envelope envelope, final double canvasX, final double canvasY) {

        Envelope result = envelope;
        // expand envelope
        result.expandBy(0.2);

        double minX = result.getMinX();
        double minY = result.getMinY();
        double maxX = result.getMaxX();
        double maxY = result.getMaxY();

        double x = maxX - minX;
        double y = maxY - minY;

        double a = x / y;
        double b = canvasX / canvasY;

        if (a < b) {
            result.expandBy((canvasX * y / canvasY) - x, 0);
        }
        return result;
    }

    /**
     * Get all features from neo4j spatial which intersect with the
     * given envelope in the given layer
     *
     * @param graphDb
     * @param envelope
     * @param layerName
     * @return
     */
    public static SimpleFeatureCollection getIntersectingFeatures(final GraphDatabaseService graphDb, final Envelope envelope, final String layerName) {

        // open data store from neo4j database
        Neo4jSpatialDataStore n4jstore = new Neo4jSpatialDataStore(graphDb);
        SimpleFeatureSource featureSource = null;

        try {

            featureSource = n4jstore.getFeatureSource(layerName);

            // search all features within this bounding
            SpatialDatabaseService spatialService = new SpatialDatabaseService(graphDb);
            Layer layer = spatialService.getLayer(layerName);
            SpatialIndexReader spatialIndex = layer.getIndex();

            Search searchQuery = new SearchIntersectWindow(envelope);
            spatialIndex.executeSearch(searchQuery);
            List<SpatialDatabaseRecord> results = searchQuery.getResults();
            List<Filter> filterList = new ArrayList<Filter>();
            for (SpatialDatabaseRecord r : results) {
                filterList.add(CQL.toFilter("NAME like '" + StringEscapeUtils.escapeSql((String) r.getProperty("NAME")) + "'"));
            }
            // create a filter with all features
            Filter filter = filterFactory.or(filterList);
            Query query = new Query(layerName, filter);
            return featureSource.getFeatures(query);

        } catch (Throwable t) {
            logger.log(Level.SEVERE, "Could not get intersection features", t);
        }
        return null;
    }

    /**
     * Render map to SVG document
     *
     * @param out           output
     * @param mapContext    context with map data
     * @param envelope      map extent
     * @param canvasX       x coordinate of paint area
     * @param canvasY       y coordinate of paint area
     * @param optimizeFtsRendering
     * @param lineWidthOptimization
     */
    synchronized public static void renderSVGDocument(StringBuilder out, final MapContext mapContext,
            final ReferencedEnvelope envelope, final int canvasX, final int canvasY,
            final Boolean optimizeFtsRendering, final Boolean lineWidthOptimization) {

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = db.getDOMImplementation().createDocument(
                    "http://www.w3.org/2000/svg", "svg", null);
            SVGGeneratorContext context = SVGGeneratorContext.createDefault(document);
            SVGGraphics2D g = new SVGGraphics2D(context, false) {

                /**
                 * Strokes the outline of a <code>Shape</code> using the settings of the
                 * current <code>Graphics2D</code> context.  The rendering attributes
                 * applied include the <code>Clip</code>, <code>Transform</code>,
                 * <code>Paint</code>, <code>Composite</code> and
                 * <code>Stroke</code> attributes.
                 * @param s the <code>Shape</code> to be rendered
                 * @see #setStroke(Stroke)
                 * @see #setPaint(Paint)
                 * @see java.awt.Graphics#setColor
                 * @see #setTransform(AffineTransform)
                 * @see #setClip(Shape)
                 * @see #setComposite(java.awt.Composite)
                 */
                @Override
                public void draw(Shape s) {

                    // Extract metadata
                    Map<String, Object> customMetadata = ((MetaDataShape) s).getCustomMetadata();

                    // Only BasicStroke can be converted to an SVG attribute equivalent.
                    // If the GraphicContext's Stroke is not an instance of BasicStroke,
                    // then the stroked outline is filled.
                    Stroke stroke = gc.getStroke();
                    if (stroke instanceof BasicStroke) {
                        Element svgShape = shapeConverter.toSVG(s);

                        // Add custom meta data to SVG elements
                        if (customMetadata != null && !(customMetadata.isEmpty()))  {
                            for (Map.Entry<String, Object> entry : customMetadata.entrySet()) {
                                svgShape.setAttribute(entry.getKey(), entry.getValue().toString());
                            }
                        }

                        if (svgShape != null) {
                            domGroupManager.addElement(svgShape, DOMGroupManager.DRAW);
                        }
                    } else {
                        Shape strokedShape = stroke.createStrokedShape(s);
                        fill(strokedShape);
                    }
                }
            };
 
            context.setEmbeddedFontsOn(false);

//            context.setStyleHandler(new StyleHandler() {
//
//                @Override
//                public void setStyle(Element element, Map map, SVGGeneratorContext generatorContext) {
//
//                    if (element.getTagName().equals("text")) {
//
//                        SVGSVGElement root = (SVGSVGElement) g.getRoot();
//                        NodeList textNodes = generatorContext.getDOMFactory().getElementsByTagName(null)
//
//                        NodeList nodes = root.getIntersectionList(element.ge, root)
//
//                        element.setAttributeNS(null, "onmouseover", "activatePath(this)");
//                        element.setAttributeNS(null, "onmouseout", "deactivatePath(this)");
//
//                    }
//
//                }
//            });

            g.setSVGCanvasSize(new Dimension(canvasX, canvasY));
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Rectangle rect = new Rectangle(g.getSVGCanvasSize());

            StreamingRenderer renderer = new StreamingRenderer();
            renderer.setContext(mapContext);

            HashMap rendererParams = new HashMap();
            rendererParams.put(StreamingRenderer.OPTIMIZED_DATA_LOADING_KEY, Boolean.TRUE);
            rendererParams.put(StreamingRenderer.OPTIMIZE_FTS_RENDERING_KEY, optimizeFtsRendering);
            rendererParams.put(StreamingRenderer.LINE_WIDTH_OPTIMIZATION_KEY, lineWidthOptimization);
            rendererParams.put(StreamingRenderer.TEXT_RENDERING_KEY, StreamingRenderer.TEXT_RENDERING_STRING);
            renderer.setRendererHints(rendererParams);


            //renderer.paint(g, rect, mapContext.getAreaOfInterest());
            renderer.paint(g, rect, envelope);

            StringWriter sw = new StringWriter();

            g.stream(sw);
            out.append(sw.getBuffer());
            sw.flush();

        } catch (Throwable t) {
            logger.log(Level.SEVERE, "Error creating SVG document", t);
        }
    }
}
