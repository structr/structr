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
package org.structr.feed.entity;

import org.structr.common.SecurityContext;
import org.structr.core.graph.NodeInterface;

import java.util.Date;


public interface DataFeed extends NodeInterface {

	String getUrl();
	String getFeedType();
	String getDescription();
	Long getUpdateInterval();
	Date getLastUpdated();
	Long getMaxAge();
	Integer getMaxItems();
	Iterable<NodeInterface> getItems();

	void updateIfDue(final SecurityContext securityContext);
	void cleanUp(final SecurityContext securityContext);
	void updateFeed(final SecurityContext securityContext);
	void updateFeed(final SecurityContext securityContext, final boolean cleanUp);
}
