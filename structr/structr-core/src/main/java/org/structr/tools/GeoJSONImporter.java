/**
 * Copyright (c) 2002-2011 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import java.io.Reader;
import org.geotools.geojson.GeoJSONUtil;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geojson.geom.GeometryJSON;
import org.neo4j.gis.spatial.Constants;
import org.neo4j.gis.spatial.EditableLayerImpl;
import org.neo4j.gis.spatial.Listener;
import org.neo4j.gis.spatial.NullListener;
import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.gis.spatial.WKBGeometryEncoder;

/**
 *
 * Importer for GeoJSON files, derived from {@see ShapefileImporter} written by Davide Savazzi
 *
 * @author Axel Morgner
 */
public class GeoJSONImporter implements Constants {

    private int commitInterval;
    // Constructor

    public GeoJSONImporter(GraphDatabaseService database, Listener monitor, int commitInterval) {
        if (commitInterval < 1) {
            throw new IllegalArgumentException("commitInterval must be > 0");
        }
        this.commitInterval = commitInterval;
        this.database = database;
        this.spatialDatabase = new SpatialDatabaseService(database);

        if (monitor == null) {
            monitor = new NullListener();
        }
        this.monitor = monitor;
    }

    public GeoJSONImporter(GraphDatabaseService database, Listener monitor) {
        this(database, monitor, 1000);
    }

    public GeoJSONImporter(GraphDatabaseService database) {
        this(database, null, 1000);
    }

    // Main
    public static void main(String[] args) throws Exception {
        String neoPath;
        String jsonPath;
        String layerName;
        int commitInterval = 1000;

        if (args.length < 2 || args.length > 4) {
            throw new IllegalArgumentException("Parameters: neo4jDirectory geojsonfile [layerName commitInterval]");
        }

        neoPath = args[0];

        jsonPath = args[1];
        // remove extension
        jsonPath = jsonPath.substring(0, jsonPath.lastIndexOf("."));

        if (args.length == 2) {
            layerName = jsonPath.substring(jsonPath.lastIndexOf(File.separator) + 1);
        } else if (args.length == 3) {
            layerName = args[2];
        } else {
            layerName = args[2];
            commitInterval = Integer.parseInt(args[3]);
        }

        GraphDatabaseService database = new EmbeddedGraphDatabase(neoPath);
        try {
            GeoJSONImporter importer = new GeoJSONImporter(database, new NullListener(), commitInterval);
            importer.importFile(jsonPath, layerName);
        } finally {
            database.shutdown();
        }
    }

    // Public methods
    public void importFile(String dataset, String layerName) throws Exception, FileNotFoundException, IOException {
        EditableLayerImpl layer = (EditableLayerImpl) spatialDatabase.getOrCreateLayer(layerName, WKBGeometryEncoder.class, EditableLayerImpl.class);

        long startTime = System.currentTimeMillis();

        File jsonFile = null;
        try {
            jsonFile = new File(dataset);
        } catch (Exception e) {
            try {
                jsonFile = new File(dataset + ".json");
            } catch (Exception e2) {
                throw new IllegalArgumentException("Failed to access the geoJSON file at either '" + dataset + "' or '" + dataset + ".json'", e);
            }
        }

        GeometryJSON gjson = new GeometryJSON();

        Reader reader = GeoJSONUtil.toReader(jsonFile);

        GeometryCollection geoCollection = gjson.readGeometryCollection(reader);

        Geometry g = gjson.read(reader);

        FeatureJSON fjson = new FeatureJSON();
        CoordinateReferenceSystem crs = fjson.readCRS(jsonFile);

        Integer geometryType = SpatialDatabaseService.convertJtsClassToGeometryType(g.getClass());

        Transaction tx = database.beginTx();
        try {
            if (crs != null) {
                layer.setCoordinateReferenceSystem(crs);
            }

            if (geometryType != null) {
                layer.setGeometryType(geometryType);
            }
            layer.add(g);
            tx.success();
        } finally {
            tx.finish();
        }

        long stopTime = System.currentTimeMillis();
        log("info | elapsed time in seconds: " + (1.0 * (stopTime - startTime) / 1000));
    }

    private void log(String message) {
        System.out.println(message);
    }

    private void log(String message, Exception e) {
        System.out.println(message);
        e.printStackTrace();
    }
    // Attributes
    private Listener monitor;
    private GraphDatabaseService database;
    private SpatialDatabaseService spatialDatabase;
}
