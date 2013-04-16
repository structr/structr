/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.web.agent;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.agent.Agent;
import org.structr.core.agent.RefreshImageFromUrlTask;
import org.structr.core.agent.ReturnValue;
import org.structr.core.agent.Task;
import org.structr.core.entity.AbstractNode;
import org.structr.web.entity.Image;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.web.node.ExtractAndSetImageDimensionsAndFormat;
import org.structr.web.node.SaveImageFromUrl;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author amorgner
 */
public class RefreshImageFromUrlAgent extends Agent {

	private static final Logger logger = Logger.getLogger(RefreshImageFromUrlAgent.class.getName());

	//~--- constructors ---------------------------------------------------

	public RefreshImageFromUrlAgent() {

		setName("RefreshImageFromUrlAgent");

	}

	//~--- methods --------------------------------------------------------

	@Override
	public ReturnValue processTask(Task task) throws FrameworkException {

		if (task instanceof RefreshImageFromUrlTask) {

			long t0 = System.currentTimeMillis();

			logger.log(Level.INFO, "Starting image refresh ...");
			refreshImageFromUrl(task.getNodes());

			long t1 = System.currentTimeMillis();

			logger.log(Level.INFO, "Image refresh finished in {0} ms", (t1 - t0) / 1000);

		}

		return (ReturnValue.Success);

	}

	private void refreshImageFromUrl(final Set<AbstractNode> nodes) throws FrameworkException {

		// FIXME: superuser security context
		final SecurityContext securityContext = SecurityContext.getSuperUserInstance();

		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

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

	//~--- get methods ----------------------------------------------------

	@Override
	public Class getSupportedTaskType() {

		return (RefreshImageFromUrlTask.class);

	}

}
