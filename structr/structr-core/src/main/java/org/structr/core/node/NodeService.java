/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.node;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.SingletonService;

/**
 *
 * @author cmorgner
 */
public class NodeService implements SingletonService {

    private static final Logger logger = Logger.getLogger(NodeService.class.getName());
    private StructrNodeFactory nodeFactory = null;
    private GraphDatabaseService graphDb = null;
//    private LuceneFulltextQueryIndexService index = null;
    private Index<Node> index = null;

    // <editor-fold defaultstate="collapsed" desc="interface SingletonService">
    @Override
    public void injectArguments(Command command) {
        if (command != null) {
            command.setArgument("graphDb", graphDb);
            command.setArgument("index", index);
            command.setArgument("nodeFactory", nodeFactory);
            command.setArgument("filesPath", Services.getFilesPath());
        }
    }

    @Override
    public void initialize(Map<String, Object> context) {

//        String dbPath = (String) context.get(Services.DATABASE_PATH);
        String dbPath = Services.getDatabasePath();

        try {
            logger.log(Level.INFO, "Initializing database ({0}) ...", dbPath);
            Map<String, String> configuration = null;

            try {

                configuration = EmbeddedGraphDatabase.loadConfigurations(dbPath + "/neo4j.conf");
                graphDb = new EmbeddedGraphDatabase(dbPath, configuration);

            } catch (Throwable t) {

                logger.log(Level.INFO, "Database config not found");
                graphDb = new EmbeddedGraphDatabase(dbPath);

            }

            logger.log(Level.INFO, "Database ready.");

            logger.log(Level.FINE, "Initializing index...");
            //index = new LuceneFulltextQueryIndexService(graphDb);
            index = graphDb.index().forNodes("fulltextAllNodes", MapUtil.stringMap("provider", "lucene", "type", "fulltext"));
            logger.log(Level.FINE, "Index ready.");

            logger.log(Level.FINE, "Initializing node factory...");
            nodeFactory = new StructrNodeFactory();
            logger.log(Level.FINE, "Node factory ready.");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Database could not be initialized. {0}", e.getMessage());
            e.printStackTrace(System.out);
        }
    }

    @Override
    public void shutdown() {
        if (isRunning()) {
            graphDb.shutdown();
            graphDb = null;
        }
    }

    @Override
    public boolean isRunning() {
        return (graphDb != null);
    }

    @Override
    public String getName() {
        return NodeService.class.getSimpleName();
    }
    // </editor-fold>
}
