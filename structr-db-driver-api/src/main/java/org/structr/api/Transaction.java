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
package org.structr.api;

import org.structr.api.graph.Identity;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;

import java.util.Set;

/**
 *
 */
public interface Transaction extends AutoCloseable {

	void failure();
	void success();
	long getTransactionId();

	@Override
	void close();

	Node getNode(final Identity id);
	Relationship getRelationship(final Identity id);

	void prefetch(final String type1, final String type2, final Set<String> keys);
	void prefetch(final String query, final Set<String> keys);

	void setIsPing(final boolean isPing);
}
