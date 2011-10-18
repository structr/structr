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

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.NotFoundException;
import org.structr.common.SecurityContext;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.node.GraphDatabaseCommand;

/**
 *
 * @author amorgner
 */
public class CleanUpFilesAgent extends Agent {

    private static final Logger logger = Logger.getLogger(CleanUpFilesAgent.class.getName());

    public CleanUpFilesAgent() {
        setName("CleanUpFilesAgent");
    }

    @Override
    public Class getSupportedTaskType() {
        return (CleanUpFilesTask.class);
    }

    @Override
    public ReturnValue processTask(Task task) {

        if (task instanceof CleanUpFilesTask) {

            long t0 = System.currentTimeMillis();
            logger.log(Level.INFO, "Starting cleaning up files ...");

            long nodes = cleanUpFiles();

            long t1 = System.currentTimeMillis();
            logger.log(Level.INFO, "Cleaning up files finished, {0} nodes processed in {1} s", new Object[]{nodes, (t1 - t0) / 1000});

        }

        return (ReturnValue.Success);
    }

    private long cleanUpFiles() {

        File filesFolder = new File(Services.getFilesPath());
        File[] files = filesFolder.listFiles();

	// FIXME: superuser security context
	final SecurityContext securityContext = SecurityContext.getSuperUserInstance();

	Command graphDbCommand = Services.command(securityContext, GraphDatabaseCommand.class);
        GraphDatabaseService graphDb = (GraphDatabaseService) graphDbCommand.execute();

        long count = 0;

        for (File file : files) {

            String fileName = file.getName();
            String[] parts = StringUtils.split(fileName, "_");

            String nodeId = parts[0];
            long id = Long.parseLong(nodeId);

            try {
                
                graphDb.getNodeById(id);
                
            } catch (NotFoundException nfe) {
                
                logger.log(Level.INFO, "Removing unreferenced file {0})", fileName);
                file.delete();
                count++;
            }

        }
        return count;

    }
}
