/*
 * Copyright (C) 2010-2024 Structr GmbH
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

import org.structr.api.Transaction;
import org.structr.api.graph.Identity;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;

import java.util.Set;

/**
 *
 *
 */
public class TransactionReference implements Transaction {

	private Transaction tx     = null;
	private int referenceCount = 0;

	public TransactionReference(final Transaction tx) {
		this.tx          = tx;
	}

	public boolean isToplevel() {
		return referenceCount == 1;
	}

	public boolean isSuccessful() {
		return tx.isSuccessful();
	}

	public void begin() {
		referenceCount++;
	}

	public void end() {
		referenceCount--;
	}

	// ----- interface Transaction -----
	@Override
	public void failure() {
		tx.failure();
	}

	@Override
	public void success() {
		tx.success();
	}

	@Override
	public long getTransactionId() {
		return tx.getTransactionId();
	}

	@Override
	public void close() {

		// only finish transaction if we are at root level
		if (--referenceCount == 0) {
			tx.close();
		}
	}

	@Override
	public Node getNode(final Identity id) {
		return tx.getNode(id);
	}

	@Override
	public Relationship getRelationship(final Identity id) {
		return tx.getRelationship(id);
	}

	@Override
	public void prefetch(final String type1, String type2, final Set<String> keys, final boolean complete) {
		tx.prefetch(type1, type2, keys, complete);
	}

	@Override
	public void prefetch(final String query, final Set<String> keys, final boolean complete) {
		tx.prefetch(query, keys, complete);
	}

	@Override
	public void setIsPing(boolean isPing) {
		tx.setIsPing(isPing);
	}
}
