/*
 * Copyright (C) 2010-2025 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.flow.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.flow.api.FlowHandler;
import org.structr.flow.impl.FlowFork;
import org.structr.flow.impl.FlowNode;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ForkHandler implements FlowHandler {

	private static final Logger logger = LoggerFactory.getLogger(ForkHandler.class);
	private static final ExecutorService threadExecutor = Executors.newSingleThreadExecutor();

	@Override
	public FlowNode handle(Context context, FlowNode flowNode) throws FlowException {

		final FlowFork flowElement = flowNode.as(FlowFork.class);
		final FlowNode forkBody    = flowElement.getForkBody();

		if (forkBody != null) {

			Context forkContext = new Context(context);
			ForkTask task = new ForkTask(forkContext, flowElement.getSecurityContext().getCachedUserId(), forkBody.getUuid(), flowElement.getUuid());

			TransactionCommand.queuePostProcessProcedure(() -> {
				// Could be written into context for future additions like a FlowJoin element
				Future<Object> future = threadExecutor.submit(task);
				context.queueForkFuture(future);
			});

		}

		return flowElement.next();
	}


	private class ForkTask implements Callable<Object> {

		private final Context context;
		private SecurityContext securityContext = null;
		private NodeInterface fork              = null;
		private NodeInterface startNode         = null;

		ForkTask(final Context context, final String secContextUserId, final String startNodeUuid, final String forkUuid) {

			this.context = context;

			App app = StructrApp.getInstance(SecurityContext.getSuperUserInstance());

			if (secContextUserId != null) {

				try (final Tx tx = app.tx()) {

					NodeInterface principalNode = app.nodeQuery("Principal").uuid(secContextUserId).getFirst();
					if (principalNode != null) {

						this.securityContext = SecurityContext.getInstance(principalNode.as(Principal.class), AccessMode.Frontend);
					}

					tx.success();

				} catch (FrameworkException ex) {

					logger.warn("Could not resolve securityContext user for ForkTask. " + ex.getMessage());
				}

			} else {

				this.securityContext = SecurityContext.getInstance(null, AccessMode.Frontend);

			}

			if (securityContext != null) {

				app = StructrApp.getInstance(securityContext);

				try (final Tx tx = app.tx()) {

					this.startNode = app.nodeQuery(StructrTraits.FLOW_NODE).uuid(startNodeUuid).getFirst();
					this.fork = app.nodeQuery(StructrTraits.FLOW_FORK).uuid(forkUuid).getFirst();

					tx.success();

				} catch (FrameworkException ex) {

					logger.warn("Could not resolve entities for ForkTask. " + ex.getMessage());
				}

			}

		}

		@Override
		public Object call() throws Exception {

			if (securityContext != null) {

				final App app = StructrApp.getInstance(securityContext);

				if (startNode != null && fork != null) {

					Object result = null;

					try (final Tx tx = app.tx()) {

						fork.as(FlowFork.class).handle(context);

						final FlowEngine engine = new FlowEngine(context);

						result = engine.execute(context, startNode.as(FlowNode.class));

						tx.success();
					}

					return result;

				}

			}

			return null;

		}
	}

}
