/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.feed.traits.wrappers;

import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.feed.entity.DataFeed;

import java.util.Date;


public class DataFeedTraitWrapper extends AbstractNodeTraitWrapper implements DataFeed {

	public DataFeedTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	public String getUrl() {
		return wrappedObject.getProperty(traits.key("url"));
	}

	public String getFeedType() {
		return wrappedObject.getProperty(traits.key("feedType"));
	}

	public String getDescription() {
		return wrappedObject.getProperty(traits.key("description"));
	}

	public Long getUpdateInterval() {
		return wrappedObject.getProperty(traits.key("updateInterval"));
	}

	public Date getLastUpdated() {
		return wrappedObject.getProperty(traits.key("lastUpdated"));
	}

	public Long getMaxAge() {
		return wrappedObject.getProperty(traits.key("maxAge"));
	}

	public Integer getMaxItems() {
		return wrappedObject.getProperty(traits.key("maxItems"));
	}

	public Iterable<NodeInterface> getItems() {
		return wrappedObject.getProperty(traits.key("items"));
	}
}
