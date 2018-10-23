/**
 * Copyright (C) 2010-2018 Structr GmbH
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

import org.structr.bolt.BoltDatabaseService;
import org.structr.core.graph.Tx;
import org.structr.flow.api.FlowElement;
import org.structr.flow.api.FlowHandler;
import org.structr.flow.api.Fork;
import org.structr.flow.impl.FlowFork;
import org.structr.flow.impl.FlowNode;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 *
 */
public class ForkHandler implements FlowHandler<Fork> {

	private static final ExecutorService threadExecutor = Executors.newFixedThreadPool(10);

	@Override
	public FlowElement handle(Context context, Fork flowElement) throws FlowException {

		FlowNode forkBody = flowElement.getForkBody();

		if (forkBody != null) {

			Context forkContext = new Context(context);
			ForkTask task = new ForkTask(forkContext, forkBody, flowElement);

			// Could be written into context for future additions like a FlowJoin element
			Future<Object> future = threadExecutor.submit(task);
			context.queueForkFuture(future);

		}

		return flowElement.next();
	}


	private class ForkTask implements Callable<Object> {
		private final Fork fork;
		private final FlowNode startNode;
		private final Context context;

		ForkTask(final Context context, final FlowNode startNode, final Fork fork) {
			this.startNode = startNode;
			this.context = context;
			this.fork = fork;
		}

		@Override
		public Object call() throws Exception {

			if (startNode != null) {

				Object result = null;

				// Clean up any potentially existing tx when using a recycled thread
				BoltDatabaseService.closeThreadTx();

				try (final Tx tx = this.fork.createTransaction()) {

					fork.handle(context);

					final FlowEngine engine = new FlowEngine(context);

					result = engine.execute(context, startNode);

					tx.success();
				}

				// Clean up session
				BoltDatabaseService.closeThreadTx();

				return result;

			}

			return null;

		}
	}

}
