package org.structr.renderer;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import org.geotools.data.DataUtilities;
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

import org.structr.common.CurrentRequest;
import org.structr.common.MapHelper;
import org.structr.common.RenderMode;
import org.structr.common.SecurityContext;
import org.structr.common.StructrOutputStream;
import org.structr.core.Command;
import org.structr.core.NodeRenderer;
import org.structr.core.Services;
import org.structr.core.TemporaryValue;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.SuperUser;
import org.structr.core.entity.geo.GeoObject;
import org.structr.core.entity.geo.Map;
import org.structr.core.node.GraphDatabaseCommand;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchNodeCommand;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Color;

import java.io.File;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner, Christian Morgner
 */
public class MapRenderer implements NodeRenderer<Map> {

	private static final String MAP_CACHE = "mapCache";
	private static final Logger logger    = Logger.getLogger(MapRenderer.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void renderNode(StructrOutputStream out, Map currentNode, AbstractNode startNode, String editUrl,
			       Long editNodeId, RenderMode renderMode) {

		if ((editNodeId != null) && (currentNode.getId() == editNodeId.longValue())) {

			currentNode.renderEditFrame(out,
						    editUrl);

		} else {

			SecurityContext securityContext = CurrentRequest.getSecurityContext();

			if (securityContext.isVisible(currentNode)) {

				// If Map result is not cached in database
				if (currentNode.getDontCache() == Boolean.TRUE) {

					long id                = startNode.getId();
					ServletContext context =
						CurrentRequest.getRequest().getSession().getServletContext();
					java.util.Map<Long, TemporaryValue<String>> mapCache =
						(java.util.Map<Long,
							       TemporaryValue<String>>) context.getAttribute(MAP_CACHE);

					if (mapCache == null) {

						mapCache = new ConcurrentHashMap<Long, TemporaryValue<String>>(1000,
							0.9f,
							8);
						context.setAttribute(MAP_CACHE,
								     mapCache);
					}

					// Cache and use the start node ID as key!
					TemporaryValue<String> cachedSVGMap = (TemporaryValue<String>) mapCache.get(id);

					if (cachedSVGMap == null) {

						// Cache 1 minute
						// TODO: Make configurable
						cachedSVGMap = new TemporaryValue<String>(null,
							TimeUnit.MINUTES.toMillis(1));

						// Update cache
						mapCache.put(id,
							     cachedSVGMap);

						// context.setAttribute(MAP_CACHE, mapCache);
					}

					if ((cachedSVGMap.getStoredValue() == null) || cachedSVGMap.isExpired()) {

						StringBuilder content = new StringBuilder(500000);

						renderSVGMap(currentNode,
							     content,
							     startNode);
						cachedSVGMap.refreshStoredValue(content.toString());
					}

					out.append(cachedSVGMap.getStoredValue());

					return;
				}

				String cachedSVGMap = currentNode.getSvgContent();

				if (StringUtils.isBlank(cachedSVGMap)) {

					StringBuilder cache = new StringBuilder();

					renderSVGMap(currentNode,
						     cache,
						     startNode);
					currentNode.setSvgContent(cache.toString());
					out.append(cache);

				} else {
					out.append(cachedSVGMap);
				}
			}
		}
	}

	/**
	 * Render SVG content directly to output stream
	 *
	 * @param currentNode
	 * @param out
	 * @param startNode
	 */
	private void renderSVGMap(Map currentNode, StringBuilder out, final AbstractNode startNode) {

		Command graphDbCommand       = Services.command(GraphDatabaseCommand.class);
		GraphDatabaseService graphDb = (GraphDatabaseService) graphDbCommand.execute();
		MapContext mapContext        = null;

		try {

			long t0                  = System.currentTimeMillis();
			String featureName       = null;
			String staticFeatureName = currentNode.getStaticFeatureName();
			GeoObject geoNode        = null;
			boolean auto             = false;

			if (StringUtils.isNotBlank(staticFeatureName)) {
				featureName = staticFeatureName;
			} else {

				// first check if we were called by a geo object which has bounds defined
				if ((startNode != null) && (startNode instanceof GeoObject)) {

					geoNode     = (GeoObject) startNode;
					featureName = geoNode.getName();

				} else {

					HttpServletRequest request  = CurrentRequest.getRequest();
					String featureNameParamName = currentNode.getFeatureNameParamName();

					if (featureNameParamName == null) {
						featureNameParamName = Map.defaultFeatureParamName;
					}

					// get the feature name from the request
					if (request != null) {
						featureName = request.getParameter(featureNameParamName);
					}
				}
			}

			int cx = currentNode.getCanvasX();
			int cy = currentNode.getCanvasY();

			auto = (geoNode != null)
			       ? geoNode.getAutoEnvelope()
			       : currentNode.getAutoEnvelope();

			List<MapLayer> layers       = new LinkedList<MapLayer>();
			MapLayer layer              = null;
			ReferencedEnvelope envelope = null;
			String shapeFilePath        = currentNode.getShapeFile();

			if (shapeFilePath != null) {

				// open data store from shapefile
				File shapeFile               = new File(shapeFilePath);
				ShapefileDataStore dataStore = new ShapefileDataStore(shapeFile.toURI().toURL());

				// build map layer with style
				StyleBuilder sb = new StyleBuilder();
				Symbolizer sym  = sb.createLineSymbolizer(Color.decode(currentNode.getLineColor()),
					currentNode.getLineWidth());

				layer = new MapLayer(dataStore.getFeatureSource(),
						     sb.createStyle(sym));
				layers.add(layer);
			}

			// open data store from neo4j database
			Neo4jSpatialDataStore n4jstore = new Neo4jSpatialDataStore(graphDb);
			String layerName               = currentNode.getLayer();

			if (StringUtils.isEmpty(layerName)) {

				logger.log(Level.SEVERE,
					   "No layer name!");
			}

			SimpleFeatureSource featureSource = n4jstore.getFeatureSource(layerName);

			if (auto) {

				if (featureName == null) {

					// if no feature name is given, show all features of layer
					envelope = featureSource.getBounds();
				} else {

					// first, find the feature which corresponds with the requested feature
					// (or the name of the node, if the request value is empty)
					List<Filter> filterList = new LinkedList<Filter>();

//                                      filterList.add(CQL.toFilter("NAME like '" + StringEscapeUtils.escapeSql(featureName) + "'"));
					filterList.add(CQL.toFilter("NAME = '"
								    + StringEscapeUtils.escapeSql(featureName) + "'"));

					Filter filter                             =
						MapHelper.featureFactory.or(filterList);
					Query query                               = new Query(layerName,
						filter);
					SimpleFeatureCollection featureCollection = featureSource.getFeatures(query);

					if ((featureCollection != null) &&!(featureCollection.isEmpty())) {

						SimpleFeature requestedFeature = featureCollection.features().next();

						envelope = (ReferencedEnvelope) requestedFeature.getBounds();
					}

					if (geoNode == null) {

						List<AbstractNode> result = (List<AbstractNode>) Services.command(
										SearchNodeCommand.class).execute(
										new SuperUser(),
										null,
										false,
										false,
										Search.andExactName(featureName));

						for (AbstractNode n : result) {

							if ((n instanceof GeoObject) && n.isNotDeleted()) {
								geoNode = (GeoObject) n;
							}
						}
					}
				}

				// Write envelope data back to geo node
				if ((geoNode != null) && geoNode.getAutoEnvelope()) {

					geoNode.setEnvelopeMinX(envelope.getMinX());
					geoNode.setEnvelopeMaxX(envelope.getMaxX());
					geoNode.setEnvelopeMinY(envelope.getMinY());
					geoNode.setEnvelopeMaxY(envelope.getMaxY());
				}

			} else {

				Double eminx;
				Double emaxx;
				Double eminy;
				Double emaxy;

				if (geoNode != null) {

					eminx = geoNode.getEnvelopeMinX();
					emaxx = geoNode.getEnvelopeMaxX();
					eminy = geoNode.getEnvelopeMinY();
					emaxy = geoNode.getEnvelopeMaxY();

				} else {

					eminx = currentNode.getEnvelopeMinX();
					emaxx = currentNode.getEnvelopeMaxX();
					eminy = currentNode.getEnvelopeMinY();
					emaxy = currentNode.getEnvelopeMaxY();
				}

				if ((eminx != null) && (emaxx != null) && (eminy != null) && (emaxy != null)) {

					envelope = new ReferencedEnvelope(eminx,
									  emaxx,
									  eminy,
									  emaxy,
									  null);

				} else {

					logger.log(Level.WARNING,
						   "Manual envelope parameter incomplete");
				}
			}

			// Expand envelope as needed
			MapHelper.expandEnvelope(envelope,
						 new Double(cx),
						 new Double(cy));

			// search all features within this bounding
			SimpleFeatureCollection features = MapHelper.getIntersectingFeatures(graphDb,
				envelope,
				layerName);

			// create a style for displaying the polygons
			Symbolizer polygonSymbolizer = MapHelper.createPolygonSymbolizer(currentNode.getLineColor(),
				currentNode.getLineWidth(),
				currentNode.getLineOpacity(),
				currentNode.getFillColor(),
				currentNode.getFillOpacity());
			Symbolizer textSymbolizer = MapHelper.createTextSymbolizer(currentNode.getFontName(),
				currentNode.getFontSize(),
				currentNode.getFontColor(),
				currentNode.getFontOpacity(),
				currentNode.getLabelAnchorX(),
				currentNode.getLabelAnchorY(),
				currentNode.getLabelDisplacementX(),
				currentNode.getLabelDisplacementY());
			Rule rule = MapHelper.styleFactory.createRule();

			rule.symbolizers().add(polygonSymbolizer);
			rule.symbolizers().add(textSymbolizer);

			FeatureTypeStyle fts = MapHelper.styleFactory.createFeatureTypeStyle(new Rule[] { rule });
			Style style          = MapHelper.styleFactory.createStyle();

			style.featureTypeStyles().add(fts);

//                      long t12 = System.currentTimeMillis();
			// add features and style as a map layer to the list of map layers
			layers.add(new MapLayer(features,
						style));

//                      long t13 = System.currentTimeMillis();
//
//                      logger.log(Level.INFO, "new MapLayer() took {0} ms", new Object[] { t13 - t12 });
			boolean displayCities = (currentNode.getDisplayCities() == Boolean.TRUE);

			if ((geoNode != null) && "Country".equals(geoNode.getType()) && displayCities) {

				List<AbstractNode> subNodes = geoNode.getLinkedNodes();    // no sorting needed here
				List<GeoObject> geoObjects  = new LinkedList<GeoObject>();

				for (AbstractNode node : subNodes) {

					if (node instanceof GeoObject) {

						GeoObject geo            = (GeoObject) node;
						String geoContainerClass = geo.getGeoContainerClass();

						if ((geoContainerClass != null)
							&& geoNode.getType().equals(geoContainerClass)) {
							geoObjects.add((GeoObject) node);
						}
					}
				}

				SimpleFeatureCollection collection = MapHelper.createPointsFromGeoObjects(geoObjects);
				Symbolizer subnodeTextSym          =
					MapHelper.createTextSymbolizer(currentNode.getPointFontName(),
					currentNode.getPointFontSize(),
					currentNode.getPointFontColor(),
					currentNode.getPointFontOpacity(),
					currentNode.getLabelAnchorX(),
					currentNode.getLabelAnchorY(),
					currentNode.getLabelDisplacementX(),
					currentNode.getLabelDisplacementY());
				Symbolizer subnodePointSym =
					MapHelper.createPointSymbolizer(currentNode.getPointShape(),
					currentNode.getPointDiameter(),
					currentNode.getPointStrokeColor(),
					currentNode.getPointStrokeLineWidth(),
					currentNode.getPointFillColor(),
					currentNode.getPointFillOpacity());

//                              Symbolizer cityPolygonSymbolizer = MapHelper.createPolygonSymbolizer("#000000", 1, 1, "#000000", 1);
				Rule rule2 = MapHelper.styleFactory.createRule();

				rule2.symbolizers().add(subnodeTextSym);

//                              rule2.symbolizers().add(cityPolygonSymbolizer);
				rule2.symbolizers().add(subnodePointSym);

				FeatureTypeStyle fts2 = MapHelper.styleFactory.createFeatureTypeStyle(new Rule[] {
								rule2 });
				Style style2 = MapHelper.styleFactory.createStyle();

				style2.featureTypeStyles().add(fts2);

				SimpleFeatureSource source          = DataUtilities.source(collection);
				SimpleFeatureCollection subFeatures = source.getFeatures();

				if (!subFeatures.isEmpty()) {

					// add features and style as a map layer to the list of map layers
					layers.add(new MapLayer(subFeatures,
								style2));
				}
			}

			// create a map context
			mapContext = new DefaultMapContext(layers.toArray(new MapLayer[] {}));

			// render map to SVG
			MapHelper.renderSVGDocument(out,
						    mapContext,
						    envelope,
						    cx,
						    cy,
						    currentNode.getOptimizeFtsRendering(),
						    currentNode.getLineWidthOptimization());

			// clear map content
			mapContext.dispose();

			long t1 = System.currentTimeMillis();

			logger.log(Level.FINE,
				   "SVG image successfully rendered in {0} ms",
				   (t1 - t0));

		} catch (Throwable t) {

			logger.log(Level.SEVERE,
				   "Error while rendering map to SVG",
				   t);

		} finally {

			if (mapContext != null) {
				mapContext.dispose();
			}
		}
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getContentType(Map currentNode) {
		return ("image/svg+xml");
	}
}
