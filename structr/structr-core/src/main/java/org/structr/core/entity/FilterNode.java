/*
 *  Copyright (C) 2011 Axel Morgner
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.core.entity;

import javax.servlet.http.HttpServletRequest;
import org.structr.common.RelType;
import org.structr.core.EntityContext;
import org.structr.core.Predicate;
import org.structr.core.node.FilterSourceCollector;
import org.structr.core.node.IterableFilter;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.Set;
import org.structr.common.PropertyView;

//~--- classes ----------------------------------------------------------------

/**
 * The abstract base class for filter nodes.
 *
 * @author Christian Morgner
 */
public abstract class FilterNode extends AbstractNode {

	static {

		EntityContext.registerPropertySet(FilterNode.class,
						  PropertyView.All,
						  Key.values());
	}

	//~--- get methods ----------------------------------------------------

	public abstract Set<Predicate<AbstractNode>> getFilterPredicates();

	// ----- Filterable -----
	@Override
	public Iterable<AbstractNode> getDataNodes(HttpServletRequest request) {

		List<AbstractNode> dataNodes = this.getDirectChildren(RelType.DATA);

		return (new IterableFilter<AbstractNode>(new FilterSourceCollector(request, dataNodes), getFilterPredicates()));
	}

	// ----- AbstractNode -----
	@Override
	public String getIconSrc() {
		return "/images/flag_blue.png";
	}
}
