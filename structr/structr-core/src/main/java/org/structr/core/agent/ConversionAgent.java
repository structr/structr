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

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.core.Services;
import org.structr.core.UnsupportedArgumentError;
import org.structr.core.entity.CsvFile;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;
import org.structr.core.node.ConvertCsvToNodeListCommand;

/**
 *
 * @author amorgner
 */
public class ConversionAgent extends Agent {

    private static final Logger logger = Logger.getLogger(ConversionAgent.class.getName());

    public ConversionAgent() {
        setName("ConversionAgent");
    }

    @Override
    public Class getSupportedTaskType() {
        return (ConversionTask.class);
    }

    @Override
    public ReturnValue processTask(Task task) {

        if (task instanceof ConversionTask) {

            ConversionTask ct = (ConversionTask) task;
            logger.log(Level.INFO, "Task found, starting conversion ...");
            convert(ct.getUser(), ct.getSourceNode(), ct.getTargetNodeClass());
            logger.log(Level.INFO, " done.");

        }

        return (ReturnValue.Success);
    }

    private void convert(final User user, final AbstractNode sourceNode, final Class targetClass) {

	// FIXME: superuser security context
	final SecurityContext securityContext = SecurityContext.getSuperUserInstance();

	if (sourceNode == null) {
            throw new UnsupportedArgumentError("Source node is null!");
        }

        if (sourceNode instanceof CsvFile) {
            Services.command(securityContext, ConvertCsvToNodeListCommand.class).execute(user, sourceNode, targetClass);
        } else {
            throw new UnsupportedArgumentError("Source node type " + sourceNode.getType() + " not supported. This agent can convert only CSV files.");
        }

    }
}
