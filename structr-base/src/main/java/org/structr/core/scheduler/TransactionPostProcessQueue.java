/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.scheduler;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TransactionPostProcessQueue {

	private final Queue<Runnable> processQueue;
	private final Queue<Runnable> commitQueue;

	public TransactionPostProcessQueue() {

		this.processQueue = new ConcurrentLinkedQueue<>();
		this.commitQueue  = new ConcurrentLinkedQueue<>();
	}

	public void queueProcess(final Runnable runnable) {

		this.processQueue.add(runnable);
	}

	public Queue<Runnable> getProcessQueue() {

		return this.processQueue;
	}

	/**
	 * Work that only makes sense once the transaction actually committed.
	 *
	 * A procedure in the other queue runs either way, which is right for cleanup but wrong for anything
	 * that records a fact about the transaction: on a rollback there is no fact to record.
	 */
	public void queueCommitProcess(final Runnable runnable) {

		this.commitQueue.add(runnable);
	}

	public void applyProcessQueue(final boolean committed) {

		for (final Runnable func : processQueue) {

			func.run();
		}

		processQueue.clear();

		if (committed) {

			for (final Runnable func : commitQueue) {

				func.run();
			}
		}

		// cleared in both cases: a procedure that was not run because the transaction rolled back is
		// not pending, it is cancelled
		commitQueue.clear();
	}

}
