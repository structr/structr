/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.entity.geo;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.gis.spatial.GeometryEncoder;
import org.neo4j.gis.spatial.Layer;
import org.neo4j.gis.spatial.Search;
import org.neo4j.gis.spatial.SpatialDatabaseRecord;
import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.gis.spatial.SpatialIndexReader;
import org.neo4j.gis.spatial.query.SearchIntersectWindow;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.GraphDatabaseCommand;

/**
 *
 * @author axel
 */
public class GeoObject extends AbstractNode {

    private final static String ICON_SRC = "/images/world.png";
    private static final Logger logger = Logger.getLogger(GeoObject.class.getName());
    public final static String LONGITUDE_KEY = "longitude";
    public final static String LATITUDE_KEY = "latitude";
    protected Layer layer;
    protected SpatialIndexReader spatialIndex;
    protected GeometryEncoder enc;
    protected Envelope env;

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }

    /**
     * Initialize the envelope of the feature which corresponds with this node
     * in the layer with the given name. The matching is done based on a comparison
     * of this node's name with the given name attribute.
     *
     * @param layerName
     * @param nameAttribute
     * @param featureName
     * @return
     */
    public void initEnvelope(final String layerName, final String nameAttribute, final String featureName) {

        env = getEnvelope(layerName, nameAttribute, featureName);

    }

    protected Envelope getEnvelope(final String layerName, final String nameAttribute, final String featureName) {

        Envelope result = null;

        Command graphDbCommand = Services.command(GraphDatabaseCommand.class);
        GraphDatabaseService graphDb = (GraphDatabaseService) graphDbCommand.execute();


        // find geometry node
        SpatialDatabaseService spatialService = new SpatialDatabaseService(graphDb);
        layer = spatialService.getLayer(layerName);
        spatialIndex = layer.getIndex();
        enc = layer.getGeometryEncoder();

        for (Node g : layer.getDataset().getAllGeometryNodes()) {

            // TODO: handle country names that differ slightly
            if (enc.hasAttribute(g, nameAttribute) && enc.getAttribute(g, nameAttribute).equals(featureName)) {

                // bounding box of this country
                result = enc.decodeEnvelope(g);
                return result;
            }
        }
        logger.log(Level.WARNING, "No envelope found.");
        return null;
    }

    private Coordinate getCentre() {
        if (env != null) {
            return env.centre();
        } else {
            logger.log(Level.SEVERE, "Envelope not initialized");
        }
        return null;
    }

    public double getCenterX() {
        Coordinate centre = getCentre();
        if (centre != null) {
            return centre.x;
        } else {
            logger.log(Level.SEVERE, "Envelope not initialized");
        }
        return 0.0;
    }

    public double getCenterY() {
        Coordinate centre = getCentre();
        if (centre != null) {
            return centre.y;
        } else {
            logger.log(Level.SEVERE, "Geo object has no centroid");
        }
        return 0.0;
    }

    public double getMinX() {
        if (env != null) {
            return env.getMinX();
        } else {
            logger.log(Level.SEVERE, "Envelope not initialized");
        }
        return 0.0;
    }

    public double getMinY() {
        if (env != null) {
            return env.getMinY();
        } else {
            logger.log(Level.SEVERE, "Envelope not initialized");
        }
        return 0.0;
    }

    public double getMaxX() {
        if (env != null) {
            return env.getMaxX();
        } else {
            logger.log(Level.SEVERE, "Envelope not initialized");
        }
        return 0.0;
    }

    public double getMaxY() {
        if (env != null) {
            return env.getMaxY();
        } else {
            logger.log(Level.SEVERE, "Envelope not initialized");
        }
        return 0.0;
    }

    /**
     * Return an array with the property values of the given key of any feature
     * in the given layer which intersects with this geodata object
     *
     * @param layerName
     * @param propertyKey
     * @return
     */
    public String[] getIntersectingFeatures(final String layerName, final String propertyKey, final String featureName) {

        Envelope localEnv = null;
        if (layerName.equals(layer.getName())) {
            // reuse envelope
            localEnv = env;
        } else {
            localEnv = getEnvelope(layerName, propertyKey, featureName);
        }

        if (localEnv != null) {

            // search within this bounding box
            Search searchQuery = new SearchIntersectWindow(localEnv);
            spatialIndex.executeSearch(searchQuery);
            List<SpatialDatabaseRecord> results = searchQuery.getResults();

            List<String> result = new ArrayList<String>();

            for (SpatialDatabaseRecord r : results) {
                String value = (String) r.getProperty(propertyKey);
                result.add(value);
            }
            return (String[]) result.toArray(new String[result.size()]);

        } else {
            logger.log(Level.SEVERE, "No envelope found");
        }
        return null;
    }

    /**
     * Return an array with the property values of the given key of any feature
     * in the given layer which intersects with this geodata object
     *
     * @param layerName
     * @param propertyKey
     * @return
     */
    public Geometry[] getIntersectingGeometries(final String layerName, final String propertyKey) {

        Envelope localEnv = null;
        if (layerName.equals(layer.getName())) {
            // reuse envelope
            localEnv = env;
        } else {
            localEnv = getEnvelope(layerName, propertyKey, getName());
        }

        if (localEnv != null) {

            // search within this bounding box
            Search searchQuery = new SearchIntersectWindow(localEnv);
            spatialIndex.executeSearch(searchQuery);
            List<SpatialDatabaseRecord> results = searchQuery.getResults();

            List<Geometry> result = new ArrayList<Geometry>();

            for (SpatialDatabaseRecord r : results) {
                result.add(r.getGeometry());
            }
            return (Geometry[]) result.toArray(new Geometry[result.size()]);

        } else {
            logger.log(Level.SEVERE, "No envelope found");
        }
        return null;
    }

    public double getLongitude() {
        Object p = getProperty(LONGITUDE_KEY);
        return (p != null ? (Double) p : Double.NaN);
    }

    public double getLatitude() {
        Object p = getProperty(LATITUDE_KEY);
        return (p != null ? (Double) p : Double.NaN);
    }

    public void setLongitude(double longitude) {
        setProperty(LONGITUDE_KEY, longitude);
    }

    public void setLatitude(double latitude) {
        setProperty(LATITUDE_KEY, latitude);
    }
}
