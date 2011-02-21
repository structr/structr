/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.agent;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.StructrNode;
import org.structr.core.node.GetAllNodes;
import org.structr.core.node.IndexNodeCommand;

/**
 *
 * @author amorgner
 */
public class RebuildIndexAgent extends Agent {

    private static final Logger logger = Logger.getLogger(RebuildIndexAgent.class.getName());

    public RebuildIndexAgent() {
        setName("RebuildIndexAgent");
    }

    @Override
    public Class getSupportedTaskType() {
        return (RebuildIndexTask.class);
    }

    @Override
    public ReturnValue processTask(Task task) {

        if (task instanceof RebuildIndexTask) {

//            RebuildIndexTask rebuildIndexTask = (RebuildIndexTask) task;
            logger.log(Level.INFO, "Starting rebuilding index ...");
            long nodes = rebuildIndex();
            logger.log(Level.INFO, "Rebuilding index finished, {0} nodes processed", nodes);

        }

        return (ReturnValue.Success);
    }

    private long rebuildIndex() {

        long nodes = 0;

        Command indexer = Services.command(IndexNodeCommand.class);

        List<StructrNode> allNodes = (List<StructrNode>) Services.command(GetAllNodes.class).execute();
        for (StructrNode s : allNodes) {
            indexer.execute(s);
            nodes++;

        }
        return nodes;
    }
}
