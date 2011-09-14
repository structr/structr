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
package org.structr.core.agent;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.GetAllNodes;
import org.structr.core.node.IndexNodeCommand;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;

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

            long t0 = System.currentTimeMillis();
            logger.log(Level.INFO, "Starting rebuilding index ...");

            long nodes = rebuildIndex();

            long t1 = System.currentTimeMillis();
            logger.log(Level.INFO, "Rebuilding index finished, {0} nodes processed in {1} s", new Object[]{nodes, (t1 - t0) / 1000});

        }

        return (ReturnValue.Success);
    }

    private long rebuildIndex() {


        Command transactionCommand = Services.command(TransactionCommand.class);
        Long nodes = (Long) transactionCommand.execute(new StructrTransaction() {

            @Override
            public Object execute() throws Throwable {

//                GraphDatabaseService graphDb = (GraphDatabaseService) Services.command(GraphDatabaseCommand.class).execute();

                long nodes = 0;

                Command indexer = Services.command(IndexNodeCommand.class);
                List<AbstractNode> allNodes = (List<AbstractNode>) Services.command(GetAllNodes.class).execute();
                for (AbstractNode s : allNodes) {
                    indexer.execute(s);
                    nodes++;

                }
                return nodes;
            }
        });

        return nodes;
    }
}
