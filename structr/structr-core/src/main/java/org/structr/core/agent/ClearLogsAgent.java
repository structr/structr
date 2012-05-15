/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
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
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.SuperUser;
import org.structr.core.entity.log.Activity;
import org.structr.core.node.DeleteNodeCommand;
import org.structr.core.node.GetAllNodes;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;

/**
 *
 * @author amorgner
 */
public class ClearLogsAgent extends Agent {

    private static final Logger logger = Logger.getLogger(ClearLogsAgent.class.getName());

    public ClearLogsAgent() {
        setName("ClearLogsAgent");
    }

    @Override
    public Class getSupportedTaskType() {
        return (ClearLogsTask.class);
    }

    @Override
    public ReturnValue processTask(Task task) throws FrameworkException {

        if (task instanceof ClearLogsTask) {

            long t0 = System.currentTimeMillis();
            logger.log(Level.INFO, "Starting clearing logs ...");

            long nodes = clearLog();

            long t1 = System.currentTimeMillis();
            logger.log(Level.INFO, "Clearing logs finished, {0} nodes processed in {1} s", new Object[]{nodes, (t1 - t0) / 1000});

        }

        return (ReturnValue.Success);
    }

    private long clearLog() throws FrameworkException {

	// FIXME: superuser security context
	final SecurityContext securityContext = SecurityContext.getSuperUserInstance();
        final Command deleteNode = Services.command(securityContext, DeleteNodeCommand.class);

        Command transactionCommand = Services.command(securityContext, TransactionCommand.class);
        Long numberOfLogNodes = (Long) transactionCommand.execute(new StructrTransaction() {

            @Override
            public Object execute() throws FrameworkException {

                long count = 0;

                List<AbstractNode> allNodes = (List<AbstractNode>) Services.command(securityContext, GetAllNodes.class).execute();
                for (AbstractNode s : allNodes) {
                    if (s instanceof Activity) {

                        try {

                            deleteNode.execute(s, true);
                            count++;

                        } catch (Throwable ignore) {
                        }
                    }
                }
                return count;
            }
        });

        return numberOfLogNodes;
    }
}
