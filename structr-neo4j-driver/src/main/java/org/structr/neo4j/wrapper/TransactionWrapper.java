/**
 * Copyright (C) 2010-2017 Structr GmbH
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
package org.structr.neo4j.wrapper;

import java.util.HashSet;
import java.util.Set;
import org.neo4j.graphdb.NotInTransactionException;
import org.structr.api.Transaction;

/**
 *
 */
public class TransactionWrapper implements Transaction {

	private static final ThreadLocal<TransactionWrapper> transactions = new ThreadLocal<>();
	private final Set<EntityWrapper> modifiedEntites                  = new HashSet<>();
	private org.neo4j.graphdb.Transaction tx                          = null;

	public TransactionWrapper(final org.neo4j.graphdb.Transaction tx) {
		transactions.set(this);
		this.tx = tx;
	}

	@Override
	public void failure() {
		tx.failure();
	}

	@Override
	public void success() {
		tx.success();
	}

	@Override
	public void close() {

		tx.close();
		transactions.remove();

		for (final EntityWrapper entity : modifiedEntites) {
			entity.clearCaches();
		}
	}

	public void registerModified(final EntityWrapper entity) {
		modifiedEntites.add(entity);
	}

	// ----- public static methods -----
	public static TransactionWrapper getCurrentTransaction() {

		final TransactionWrapper tx = transactions.get();
		if (tx == null) {

			throw new NotInTransactionException();
		}

		return tx;
	}
}
