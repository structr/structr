/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.api;

import org.structr.api.graph.Identity;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;

/**
 *
 */
public interface Transaction extends AutoCloseable, Prefetcher {

	void failure();
	void success();
	long getTransactionId();
	boolean isSuccessful();
	void setNodeIsCreated(final long id);
	boolean isNodeCreated(final long id);
	boolean isNodeDeleted(final long id);
	boolean isRelationshipDeleted(final long id);

	@Override
	void close();

	Node getNode(final Identity id);
	Relationship getRelationship(final Identity id);

	void setIsPing(final boolean isPing);
	int level();
}
