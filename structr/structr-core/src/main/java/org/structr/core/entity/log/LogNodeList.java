/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.entity.log;

import java.util.Date;
import org.neo4j.graphdb.GraphDatabaseService;
import org.structr.core.Decorator;
import org.structr.core.Services;
import org.structr.core.entity.NodeList;
import org.structr.core.entity.StructrNode;
import org.structr.core.node.GraphDatabaseCommand;

public class LogNodeList<T extends StructrNode> extends NodeList<T> {

    private final GraphDatabaseService graphDb = (GraphDatabaseService) Services.createCommand(GraphDatabaseCommand.class).execute();

    public LogNodeList() {
        // add creating decorator..
        addDecorator(new Decorator<T>() {

            @Override
            public void decorate(StructrNode t) {
                t.init(graphDb.createNode());
            }
        });

        // add creating decorator..
        addDecorator(new Decorator<T>() {

            @Override
            public void decorate(StructrNode t) {
                if (t instanceof Activity) {
                    ((Activity) t).setStartTimestamp(new Date());
                }
            }
        });
    }
}
