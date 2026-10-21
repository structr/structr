/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.messaging.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.core.graph.TransactionCommand;

/**
 * Defers work with side effects outside of the database, like publishing a message or starting a consumer thread,
 * until the transaction that triggers it has committed.
 */
public final class AfterCommit {

	private static final Logger logger = LoggerFactory.getLogger(AfterCommit.class);

	private AfterCommit() {
	}

	/**
	 * Runs the given work once the transaction of the current thread has committed, and not at all if it is rolled back.
	 * A transaction that is repeated because the database terminated it does the work only once, after the attempt that
	 * commits. Outside of a transaction, the work runs right away, because there is no commit to wait for.
	 *
	 * The work runs outside of any transaction, so it must not use the nodes of the committed transaction. Its errors
	 * are only logged: the transaction cannot be rolled back anymore, and an exception would keep the procedures queued
	 * after it from running.
	 */
	public static void run(final Runnable work) {

		final Runnable guarded = () -> {

			try {

				work.run();

			} catch (RuntimeException rex) {

				logger.error("Unable to run work after the commit of a transaction", rex);
			}
		};

		if (TransactionCommand.inTransaction()) {

			TransactionCommand.queuePostCommitProcedure(guarded);

		} else {

			guarded.run();
		}
	}
}
