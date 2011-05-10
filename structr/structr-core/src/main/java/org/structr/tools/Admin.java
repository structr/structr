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
package org.structr.tools;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.Traverser.*;
import org.neo4j.graphdb.index.Index;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.kernel.Traversal;
import org.structr.common.RelType;
import org.structr.core.entity.File;
import org.structr.core.entity.AbstractNode;

public class Admin {

    protected static GraphDatabaseService graphDb;
    protected static Index<Node> index;

    /**
     * Example usage:
     * 
     *      java -cp <classpath> org.structr.tools.Admin -t -dbPath /var/lib/structr -changeRels -oldRelType IS_CHILD -newRelType HAS_CHILD
     *
     *   or
     * 
     *      java -cp <classpath> org.structr.tools.Admin -t -dbPath /var/lib/structr -renameProperty -oldPropertyName mimeType -newPropertyName contentType
     *
     * @param args
     */
    public static void main(String[] args) {

        boolean changeRels = false;
        boolean addPathProperty = false;
        boolean test = false;
        boolean renameProperty = false;
        boolean removeEmptyNodes = false;
        boolean rebuildIndex = false;
        boolean listNodes = false;
        boolean removeOrphanedNodes = false;
        boolean copyDatabase = false;

        String dbPath = null;
        String targetDbPath = null;
        String filesPath = null;
        String name = null;
        String type = null;

        String nodeType = null;
        String relDirection = null;
        String oldRelType = null;
        String newRelType = null;

        String oldPropertyName = null;
        String newPropertyName = null;

        System.out.println("Structr admin tool started.");


        System.out.print("Parsing command line arguments ...");

        for (int k = 0; k < args.length; k++) {
            String arg = args[k];

            if ("-rebuildIndex".equals(arg)) {
                rebuildIndex = true;
            } else if ("-copyDatabase".equals(arg)) {
                copyDatabase = true;
            } else if ("-removeOrphanedNodes".equals(arg)) {
                removeOrphanedNodes = true;
            } else if ("-listNodes".equals(arg)) {
                listNodes = true;
            } else if ("-changeRels".equals(arg)) {
                changeRels = true;
            } else if ("-addPathProperty".equals(arg)) {
                addPathProperty = true;
            } else if ("-renameProperty".equals(arg)) {
                renameProperty = true;
            } else if ("-removeEmptyNodes".equals(arg)) {
                removeEmptyNodes = true;
            } else if ("-t".equals(arg)) {
                test = true;
            } else if ("-dbPath".equals(arg)) {
                dbPath = args[++k];
            } else if ("-targetDbPath".equals(arg)) {
                targetDbPath = args[++k];
            } else if ("-filesPath".equals(arg)) {
                filesPath = args[++k];
            } else if ("-relDirection".equals(arg)) {
                relDirection = args[++k];
            } else if ("-nodeType".equals(arg)) {
                nodeType = args[++k];
            } else if ("-oldRelType".equals(arg)) {
                oldRelType = args[++k];
            } else if ("-newRelType".equals(arg)) {
                newRelType = args[++k];
            } else if ("-oldPropertyName".equals(arg)) {
                oldPropertyName = args[++k];
            } else if ("-newPropertyName".equals(arg)) {
                newPropertyName = args[++k];
            } else if ("-name".equals(arg)) {
                name = args[++k];
            } else if ("-type".equals(arg)) {
                type = args[++k];
            }
        }

        System.out.println("done.");

        if (dbPath == null) {
            System.out.println("Argument -dbPath missing!");
            System.exit(0);
        }

        if (rebuildIndex) {

            Admin adminTool = new Admin(dbPath, filesPath);

            Transaction tx = graphDb.beginTx();
            try {

                System.out.println("Rebuilding index");
                adminTool.rebuildIndex(graphDb, index, test, tx);

                tx.success();
            } finally {
                tx.finish();
            }
            shutdown();

        } else if (listNodes) {

            Admin adminTool = new Admin(dbPath, filesPath);

            Transaction tx = graphDb.beginTx();
            try {

                System.out.println("List nodes");
                adminTool.listNodes();

                tx.success();
            } finally {
                tx.finish();
            }
            shutdown();

        } else if (copyDatabase) {

            Admin adminTool = new Admin(dbPath, filesPath);

            Transaction tx = graphDb.beginTx();
            try {

                System.out.println("Copy database");
                adminTool.copyDatabase(test, dbPath, targetDbPath, tx);

                tx.success();
            } finally {
                tx.finish();
            }
            shutdown();

        } else if (changeRels) {

            Admin adminTool = new Admin(dbPath, filesPath);

            Transaction tx = graphDb.beginTx();
            try {

                System.out.println("Changing all " + relDirection + " relationships of all " + nodeType + " nodes from " + oldRelType + " to " + newRelType);

                Direction dir = null;
                if (relDirection != null) {
                    if ("OUTGOING".equals(relDirection)) {
                        dir = Direction.OUTGOING;
                    } else if ("INCOMING".equals(relDirection)) {
                        dir = Direction.INCOMING;
                    } else if ("BOTH".equals(relDirection)) {
                        dir = Direction.BOTH;
                    }
                }

                adminTool.changeRelationships(dir, nodeType, oldRelType, newRelType, test);

                tx.success();
            } finally {
                tx.finish();
            }
            shutdown();

        } else if (removeEmptyNodes) {

            Admin adminTool = new Admin(dbPath, filesPath);

            Transaction tx = graphDb.beginTx();
            try {

                System.out.println("Remove empty nodes");
                adminTool.removeEmptyNodes(test, name, type, tx);

                tx.success();
            } finally {
                tx.finish();
            }
            shutdown();

        } else if (removeOrphanedNodes) {

            Admin adminTool = new Admin(dbPath, filesPath);

            Transaction tx = graphDb.beginTx();
            try {

                System.out.println("Remove orphaned nodes");
                adminTool.removeOrphanedNodes(test, tx);

                tx.success();
            } finally {
                tx.finish();
            }
            shutdown();

        } else if (addPathProperty) {

            Admin adminTool = new Admin(dbPath, filesPath);

            Transaction tx = graphDb.beginTx();
            try {

                System.out.println("Add path property");
                adminTool.addPathProperty(test);

                tx.success();
            } finally {
                tx.finish();
            }
            shutdown();

        } else if (renameProperty && oldPropertyName != null && newPropertyName != null) {

            Admin adminTool = new Admin(dbPath, filesPath);

            Transaction tx = graphDb.beginTx();
            try {

                System.out.println("Rename property");
                adminTool.renameProperty(test, oldPropertyName, newPropertyName);

                tx.success();
            } finally {
                tx.finish();
            }
            shutdown();

        }

        System.out.println("Structr admin tool finished.");


    }

    public Admin(String dbPath, String filesPath) {

        try {
            registerShutdownHook();

            if (graphDb == null) {
                System.out.print("Starting database ...");
                graphDb = new EmbeddedGraphDatabase(dbPath);
                System.out.println("done.");
            }


            if (graphDb != null) {
                index = graphDb.index().forNodes("fulltextAllNodes", MapUtil.stringMap("provider", "lucene", "type", "fulltext"));
            }

        } catch (Exception e) {
            System.err.print("Could not connect establish database session: "
                    + e.getMessage());
        }
    }

    private static void shutdown() {
        // indexService.shutdown();
        System.out.print("Shutting down database ...");
        graphDb.shutdown();
        System.out.println("done.");
    }

    private static void registerShutdownHook() {
        // Registers a shutdown hook for the Neo4j and index service instances
        // so that it shuts down nicely when the VM exits (even if you
        // "Ctrl-C" the running example before it's completed)
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                shutdown();
            }
        });
    }

    /**
     * Change relationship type.
     *
     * Example parameter: [-t] -changeRels -oldRelType IS_CHILD -newRelType HAS_CHILD
     *
     * @param relDirection
     * @param nodeType
     * @param oldType
     * @param newType
     * @param test
     */
    private void changeRelationships(Direction relDirection, String nodeType, String oldType, String newType, boolean test) {

        RelationshipType oldRelType = DynamicRelationshipType.withName(oldType);
        RelationshipType newRelType = DynamicRelationshipType.withName(newType);

//        Traverser traverser = graphDb.getReferenceNode().traverse(Order.BREADTH_FIRST,
//                StopEvaluator.END_OF_GRAPH, ReturnableEvaluator.ALL,
//                RelType.HAS_CHILD, Direction.OUTGOING);

//        System.out.println("Starting traversal ...");

        Iterable<Node> allNodes = graphDb.getAllNodes();

        for (Node n : allNodes) {

            if (n.hasProperty(AbstractNode.TYPE_KEY) && n.getProperty(AbstractNode.TYPE_KEY).equals(nodeType)) {

                System.out.println(nodeType + " node found: " + n.getId());

                for (Relationship rel : n.getRelationships(oldRelType, relDirection)) {
                    System.out.println("Relationship found: " + rel);


                    Node startNode = null;
                    Node endNode = null;

                    if (relDirection.equals(Direction.OUTGOING)) {
                        startNode = n;
                        endNode = rel.getEndNode();
                    } else {
                        startNode = rel.getStartNode();
                        endNode = n;
                    }

                    Relationship newRel = null;
                    if (!test) {
                        newRel = startNode.createRelationshipTo(endNode, newRelType);
                    }
                    System.out.println("New relationship created: " + newRel);

                    if (!test) {
                        rel.delete();
                    }
                    System.out.println("Old relationship deleted: " + rel);
                }

            }
        }

    }

    /**
     * Make a clean copy of the database
     *
     * Example parameter: [-t] -copyDatabase -targetDbPath <path_to_database_copy>
     *
     * @param oldType
     * @param newType
     * @param test
     */
    private void copyDatabase(boolean test, String dbPath, String targetDbPath, Transaction tx) {

//        TraversalDescription desc = Traversal.description().expand(Traversal.expanderForAllTypes(Direction.OUTGOING));
//        desc.traverse(graphDb.getReferenceNode()).nodes();

        long t0 = System.currentTimeMillis();

        System.out.print("Starting target database ...");
        EmbeddedGraphDatabase targetDb = new EmbeddedGraphDatabase(targetDbPath);
        Index<Node> targetIndex = targetDb.index().forNodes("fulltextAllNodes", MapUtil.stringMap("provider", "lucene", "type", "fulltext"));
        System.out.println("done.");

        String targetFilesPath = targetDbPath + "/files";
        System.out.print("Creating directory " + targetFilesPath + " ...");
        boolean success = (new java.io.File(targetFilesPath)).mkdir();
        if (success) {
            System.out.println("done.");
        }

        Transaction targetTx = targetDb.beginTx();
        long counter = 0L;

        // list to store all relationships of source database
        List<Relationship> allRels = new LinkedList<Relationship>();

        // lookup map to store all pairs of source node id (key) and target node id (value)
        Map<Long, Long> idMap = new HashMap<Long, Long>();

        Iterable<Node> allNodes = graphDb.getAllNodes();

        try {

            for (Node sourceNode : allNodes) {

                // Store any outgoing relationship
                for (Relationship r : sourceNode.getRelationships(Direction.OUTGOING)) {
                    allRels.add(r);
                }

                long sourceId = sourceNode.getId();


                if (test) {
                    System.out.println("Node id " + sourceId + " found. Remove -t flag to copy to target database!");
                } else {

                    Node targetNode = null;

                    // Don't create a node with id 0 in the target database!
                    // If you do, the reference node of the target database will be hidden.
                    if (sourceId == 0L) {
                        targetNode = targetDb.getReferenceNode();
                    } else {
                        targetNode = targetDb.createNode();
                    }
                    long targetId = targetNode.getId();

                    // Store target node id in map
                    idMap.put(sourceId, targetId);

                    System.out.println("Node created (source id: " + sourceId + ", target id: " + targetId);

                    for (String key : sourceNode.getPropertyKeys()) {
                        targetNode.setProperty(key, sourceNode.getProperty(key));
                    }

                    // Handle file and image nodes
                    if (targetNode.hasProperty(AbstractNode.TYPE_KEY) && (targetNode.getProperty(AbstractNode.TYPE_KEY).equals("File")
                            || targetNode.getProperty(AbstractNode.TYPE_KEY).equals("Image"))) {

                        if (sourceNode.hasProperty(File.RELATIVE_FILE_PATH_KEY)) {

                            String oldPath = dbPath + "/files/" + (String) sourceNode.getProperty(File.RELATIVE_FILE_PATH_KEY);
                            String newRelativePath = targetNode.getId() + "_" + System.currentTimeMillis();
                            String newPath = targetFilesPath + "/" + newRelativePath;

                            java.io.File source = new java.io.File(oldPath);
                            java.io.File target = new java.io.File(newPath);

                            // Copy files using NIO transfer, see http://www.baptiste-wicht.com/2010/08/file-copy-in-java-benchmark/
                            // Thanks to Babtiste Wicht!
                            nioTransferCopy(source, target);
                            targetNode.setProperty(File.RELATIVE_FILE_PATH_KEY, newRelativePath);

                            System.out.println("Copied file " + oldPath + " to " + newPath);
                        } else {
                            System.out.println("Could not copy " + sourceNode.getId());
                        }

                    }

                    Imp.indexNode(index, targetNode);
                    counter++;

                    if (counter % 1000 == 0) {
                        targetTx.success();
                        targetTx.finish();
                        targetTx = targetDb.beginTx();
                        System.out.println("Committed to database after " + counter + " nodes.");
                    }

                }
            }
            targetTx.success();
        } finally {
            targetTx.finish();
        }

        long t1 = System.currentTimeMillis();
        System.out.println(counter + " nodes copied to target database in " + (t1 - t0) + " ms");


        // Now copy all relationships
        targetTx = targetDb.beginTx();
        long relCounter = 0L;

        try {

            for (Relationship r : allRels) {

                // Create relationship in target database

                long sourceStartNodeId = r.getStartNode().getId();
                long sourceEndNodeId = r.getEndNode().getId();

                long targetStartNodeId = (long) idMap.get(sourceStartNodeId);
                long targetEndNodeId = (long) idMap.get(sourceEndNodeId);

                Node s = targetDb.getNodeById(targetStartNodeId);
                Node e = targetDb.getNodeById(targetEndNodeId);

                Relationship nr = s.createRelationshipTo(e, r.getType());
                System.out.println("Relationship created between " + targetStartNodeId + " and " + targetEndNodeId);

                // copy relationship properties (important for security, thumbnails etc.)
                for (String key : r.getPropertyKeys()) {
                    nr.setProperty(key, r.getProperty(key));
                }

                relCounter++;
                if (relCounter % 1000 == 0) {
                    targetTx.success();
                    targetTx.finish();
                    targetTx = targetDb.beginTx();
                    System.out.println("Committed to database after " + counter + " relationships.");
                }

            }


            targetTx.success();
        } finally {
            targetTx.finish();
        }

        // finally rebuild index in new database
        rebuildIndex(targetDb, targetIndex, false, targetDb.beginTx());

        targetTx.success();
        targetTx.finish();


        long t2 = System.currentTimeMillis();

        System.out.println(relCounter + " relationships created in target database in " + (t2 - t1) + " ms");

        System.out.print("Shutting down target database ...");
        targetDb.shutdown();

        System.out.println("done.");


    }

    /**
     * List nodes
     *
     * Example parameter: -listNodes
     *
     */
    private void listNodes() {

        int counter = 0;
        for (Node n : graphDb.getAllNodes()) {

            long id = n.getId();

            String name = "";
            if (n.hasProperty("name")) {
                name = (String) n.getProperty("name");
            }

            String type = "";
            if (n.hasProperty("type")) {
                type = (String) n.getProperty("type");
            }

            System.out.println("(" + counter + ") Node id: " + id + ", name: " + name + ", type: " + type);

            for (Relationship r : n.getRelationships()) {

                Node startNode = r.getStartNode();
                Node endNode = r.getEndNode();

                String startNodeName = "";
                if (startNode.hasProperty("name")) {
                    startNodeName = (String) startNode.getProperty("name");
                }

                String endNodeName = "";
                if (endNode.hasProperty("name")) {
                    endNodeName = (String) endNode.getProperty("name");
                }

                System.out.println(startNodeName + "(" + startNode.getId() + ") --- " + r.getType().name() + "(" + r.getId() + ") ---> " + endNodeName + "(" + endNode.getId() + ")");
            }


            counter++;

        }
        System.out.println("Processed " + counter + " nodes.");

    }

    /**
     * Remove empty nodes
     *
     * Empty nodes have no outgoing relationships
     *
     * Example parameter: [-t] -removeEmptyNodes -name <name> -type <type>
     *
     * @param test
     */
    private void removeEmptyNodes(boolean test, final String nameAttr, final String typeAttr, Transaction tx) {

        int counter = 0;
        for (Node n : graphDb.getAllNodes()) {

            long id = n.getId();

            if (!n.hasRelationship(Direction.OUTGOING)) {

                boolean nameMatches = false;
                if (n.hasProperty("name") && n.getProperty("name").equals(nameAttr)) {
                    nameMatches = true;
                }

                boolean typeMatches = false;
                if (n.hasProperty("type") && n.getProperty("type").equals(typeAttr)) {
                    typeMatches = true;
                }

                if (nameMatches && typeMatches) {

                    if (!test) {

                        for (Relationship r : n.getRelationships()) {
                            r.delete();
                        }
                        n.delete();

                        System.out.println("Node " + id + " had no outgoing relationships, deleted.");

                    } else {

                        System.out.println("Empty node " + id + " without outgoing relationships detected. Remove -t flag to delete");
                    }
                }

            }
            counter++;

            if (counter % 100 == 0) {

                tx.success();
                tx.finish();

                tx = graphDb.beginTx();

                System.out.println("##### Committed to database after " + counter + " nodes.");
            }

        }
        System.out.println("Processed " + counter + " nodes.");

    }

    /**
     * Remove orphaned nodes.
     *
     * Orphanded nodes have no relationships at all.
     *
     * Example parameter: [-t] -removeOrphanedNodes
     *
     * @param test
     */
    private void removeOrphanedNodes(boolean test, Transaction tx) {

        int counter = 0;
        for (Node n : graphDb.getAllNodes()) {

            long id = n.getId();

            if (!n.hasRelationship()) {


                if (!test) {

                    n.delete();

                    System.out.println("Node " + id + " had no relationships at all, deleted.");

                } else {

                    System.out.println("Orphaned node " + id + " without relationships detected. Remove -t flag to delete");
                }

            } else {

                // Better not remove such nodes ..

//
//                // test if node has a path to the reference node
//
//                Iterable<Node> nodes = Traversal.description().relationships(RelType.HAS_CHILD, Direction.INCOMING).traverse(n).nodes();
//
//                int c = 0;
//                boolean isConnectedToRootNode = false;
//                for (Node an : nodes) {
//                    c++;
//                    if (an.equals(graphDb.getReferenceNode())) {
//                        isConnectedToRootNode = true;
//                    }
//
//                }
//
//                //System.out.print("Node has " + c + " ancestor node(s)");
//                if (isConnectedToRootNode) {
//                    //System.out.println(" and is connected with the root node");
//                } else {
//                    System.out.println(" and is NOT connected with the root node!!");
//
//                    if (!test) {
//
//                        // delete all relationships of this node
//                        for (Relationship r : n.getRelationships()) {
//                            r.delete();
//                        }
//
//                        // then delete node
//                        n.delete();
//
//                        System.out.println("Node " + id + " was not connected with root node, deleted.");
//
//                    } else {
//
//                        System.out.println("Orphaned node " + id + " is not connected with root node. Remove -t flag to delete");
//                    }
//                }
//


            }

            counter++;

            if (counter % 100 == 0) {

                tx.success();
                tx.finish();

                tx = graphDb.beginTx();

                System.out.println("##### Committed to database after " + counter + " nodes.");
            }

        }
        System.out.println("Processed " + counter + " nodes.");

    }

    /**
     * Add (or set) a new 'path' property extracted from 'url' property.
     *
     * [-t] -addPathProperty
     *
     * @param test
     */
    private void addPathProperty(boolean test) {


        Traverser traverser = graphDb.getReferenceNode().traverse(Order.BREADTH_FIRST,
                StopEvaluator.END_OF_GRAPH, ReturnableEvaluator.ALL,
                RelType.HAS_CHILD, Direction.OUTGOING);

        for (Node n : traverser.getAllNodes()) {

            System.out.println("Node " + n + " found.");

            if (n.hasProperty("url")) {

                String url = (String) n.getProperty("url");
                System.out.println("URL value: " + url);

                char backslash = '\\';
                int l = url.lastIndexOf(backslash);

                if (l < 0) {
                    l = url.lastIndexOf("/");
                }

                String path = url.substring(l + 1, url.length());
                System.out.println("Path: " + path);

                if (!test) {
                    n.setProperty("relativeFilePath", path);
                }
            }
        }

    }

    /**
     * Rename propery
     *
     * [-t] -renameProperty
     *
     * @param test
     * @param oldPropertyName
     * @param newPropertyName
     */
    private void renameProperty(boolean test, String oldPropertyName, String newPropertyName) {


        for (Node n : Traversal.description().breadthFirst().relationships(RelType.HAS_CHILD, Direction.OUTGOING).prune(Traversal.pruneAfterDepth(999)).traverse(graphDb.getReferenceNode()).nodes()) {

            System.out.println("Node " + n + " found.");

            if (n.hasProperty(oldPropertyName)) {

                if (!test) {
                    n.setProperty(newPropertyName, n.getProperty(oldPropertyName));
                    n.removeProperty(oldPropertyName);
                    System.out.println("Property " + oldPropertyName + " successfully renamed to " + newPropertyName + " on node " + n.getId());
                } else {
                    System.out.println("Without test switch, property " + oldPropertyName + " would have been renamed to " + newPropertyName + " on node " + n.getId());
                }
            }
        }

    }

    /**
     * Rebuild Lucene search index
     *
     * [-t] -rebuildIndex
     *
     * @param test
     * @param tx
     */
    private void rebuildIndex(GraphDatabaseService db, Index<Node> ix, boolean test, Transaction tx) {

        int nodeCounter = 0;
        int propertyCounter = 0;

        for (Node n : Traversal.description().breadthFirst().relationships(RelType.HAS_CHILD, Direction.OUTGOING).traverse(db.getReferenceNode()).nodes()) {

            nodeCounter++;
            System.out.println(nodeCounter + ": Node " + n + " found.");

            for (String propertyKey : n.getPropertyKeys()) {

                propertyCounter++;

                if (!test) {
                    ix.remove(n, propertyKey);
                    System.out.println(propertyCounter + ": Property " + propertyKey + " removed from index.");

                    ix.add(n, propertyKey, n.getProperty(propertyKey));
                    System.out.println(propertyKey + " reindexed.");

                } else {
                    System.out.println("Without test switch, property " + propertyKey + " would have been removed from index.");
                    System.out.println("Without test switch, " + propertyKey + " would have been reindexed.");
                }

            }

            if (nodeCounter % 1000 == 0) {

                tx.success();
                tx.finish();

                tx = db.beginTx();

                System.out.println("##### Committed to database after " + nodeCounter + " nodes.");
            }

        }

        System.out.println("Processed " + propertyCounter + " properties of " + nodeCounter + " nodes ");

    }

    private static void nioTransferCopy(java.io.File source, java.io.File target) {
        FileChannel in = null;
        FileChannel out = null;

        try {
            in = new FileInputStream(source).getChannel();
            out = new FileOutputStream(target).getChannel();

            long size = in.size();
            long transferred = in.transferTo(0, size, out);

            while (transferred != size) {
                transferred += in.transferTo(transferred, size - transferred, out);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(in);
            close(out);
        }
    }

    private static void close(Closeable closable) {
        if (closable != null) {
            try {
                closable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
