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

	@Override
	public boolean isNodeDeleted(final long id) {
		return tx.isNodeDeleted(id);
	}

	@Override
	public boolean isRelationshipDeleted(final long id) {
		return tx.isRelationshipDeleted(id);
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
	public void prefetch(final String type1, String type2, final Set<String> keys) {
		tx.prefetch(type1, type2, keys);
	}

	@Override
	public void prefetch(final String query, final Set<String> keys) {
		tx.prefetch(query, keys);
	}

	@Override
	public void prefetch(final String query, final Set<String> outgoingKeys, final Set<String> incomingKeys) {
		tx.prefetch(query, outgoingKeys, incomingKeys);
	}

	@Override
	public void prefetch2(final String query, final Set<String> outgoingKeys, final Set<String> incomingKeys) {
		tx.prefetch2(query, outgoingKeys, incomingKeys);
	}

	@Override
	public void setIsPing(boolean isPing) {
		tx.setIsPing(isPing);
	}
}
