/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.core.graph;

import org.neo4j.graphdb.Lock;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Transaction;
import org.structr.core.TransactionSource;

/**
 *
 *
 */
public class TransactionReference implements Transaction {

	private TransactionSource source = null;
	private Transaction tx           = null;
	private int referenceCount       = 0;
	private boolean successful       = false;

	public TransactionReference(final Transaction tx) {
		this.tx = tx;
	}

	public boolean isToplevel() {
		return referenceCount == 1;
	}

	public boolean isSuccessful() {
		return successful;
	}

	public void begin() {
		referenceCount++;
	}

	public void end() {
		referenceCount--;
	}

	public int getReferenceCount() {
		return referenceCount;
	}

	public void setSource(final TransactionSource source) {
		this.source = source;
	}

	public TransactionSource getSource() {
		return source;
	}

	// ----- interface Transaction -----
	@Override
	public void failure() {
		tx.failure();
		successful = false;
	}

	@Override
	public void success() {
		tx.success();
		successful = true;
	}

	@Override
	public void finish() {
		close();
	}

	@Override
	public void close() {

		// only finish transaction if we are at root level
		if (--referenceCount == 0) {

			// fail transaction if no success() call was made
			if (!successful) {
				tx.failure();
			}

			tx.close();
		}
	}

	@Override
	public Lock acquireWriteLock(PropertyContainer entity) {
		return tx.acquireWriteLock(entity);
	}

	@Override
	public Lock acquireReadLock(PropertyContainer entity) {
		return tx.acquireReadLock(entity);
	}

	@Override
	public void terminate() {
		tx.terminate();
	}
}
