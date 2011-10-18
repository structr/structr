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

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.Image;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.ExtractAndSetImageDimensionsAndFormat;
import org.structr.core.node.SaveImageFromUrl;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;

/**
 *
 * @author amorgner
 */
public class RefreshImageFromUrlAgent extends Agent {

    private static final Logger logger = Logger.getLogger(RefreshImageFromUrlAgent.class.getName());

    public RefreshImageFromUrlAgent() {
        setName("RefreshImageFromUrlAgent");
    }

    @Override
    public Class getSupportedTaskType() {
        return (RefreshImageFromUrlTask.class);
    }

    @Override
    public ReturnValue processTask(Task task) {

        if (task instanceof RefreshImageFromUrlTask) {

            long t0 = System.currentTimeMillis();
            logger.log(Level.INFO, "Starting image refresh ...");

            refreshImageFromUrl(task.getNodes());

            long t1 = System.currentTimeMillis();
            logger.log(Level.INFO, "Image refresh finished in {0} ms", (t1 - t0) / 1000);

        }

        return (ReturnValue.Success);
    }

    private void refreshImageFromUrl(final Set<AbstractNode> nodes) {

	// FIXME: superuser security context
	final SecurityContext securityContext = SecurityContext.getSuperUserInstance();
        Command transactionCommand = Services.command(securityContext, TransactionCommand.class);
        transactionCommand.execute(new StructrTransaction() {

            @Override
            public Object execute() throws Throwable {

                List<Image> images = new LinkedList<Image>();

                for (AbstractNode node : nodes) {

                    if (node instanceof Image) {

                        Image image = (Image) node;
                        Services.command(securityContext, SaveImageFromUrl.class).execute(image);
                        images.add(image);

                    }
                    Services.command(securityContext, ExtractAndSetImageDimensionsAndFormat.class).execute(images);
                }

                return null;
            }
        });

    }
}
