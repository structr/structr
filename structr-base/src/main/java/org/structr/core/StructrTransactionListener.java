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
package org.structr.core;

import org.structr.api.Predicate;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.ModificationEvent;

import java.util.Collection;
import java.util.Map;

/**
 * An interface that allows you to be notified when a graph object is
 * modified, with the option to veto the modification.
 *
 * In order to use this interface, you must register your implementation in SchemaHelper.
 *
 *
 */
public interface StructrTransactionListener {

	void beforeCommit(final SecurityContext securityContext, final Collection<ModificationEvent> modificationEvents) throws FrameworkException;
	void afterCommit(final SecurityContext securityContext, final Collection<ModificationEvent> modificationEvents);

	default void simpleBroadcast(final String messageName, final Map<String, Object> data) {
		simpleBroadcast(messageName, data, null);
	}

	void simpleBroadcast(final String messageName, final Map<String, Object> data, final Predicate<String> sessionIdPredicate);
}
