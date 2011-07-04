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

package org.structr.core.entity.filter;

import java.util.LinkedHashSet;
import java.util.Set;
import org.structr.common.PropertyKey;
import org.structr.core.Predicate;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.FilterNode;

/**
 * A filter node that applies a regular expression to the content
 * of a given property.
 *
 * @author Christian Morgner
 */
public class PropertyRegexFilter extends FilterNode {

	public enum Key implements PropertyKey {

		propertyName, filterExpression
	}

	@Override
	public Set<Predicate<AbstractNode>> getFilterPredicates() {

		Set<Predicate<AbstractNode>> set = new LinkedHashSet<Predicate<AbstractNode>>();
		final String filterExpression = getStringProperty(Key.filterExpression);
		final String propertyName = getStringProperty(Key.propertyName);

		set.add(new Predicate<AbstractNode>() {

			@Override
			public boolean evaluate(AbstractNode obj) {

				if(propertyName != null && filterExpression != null) {

					Object value = obj.getProperty(propertyName);
					if(value != null) {

						String stringValue = value.toString();
						return(stringValue.matches(filterExpression));
					}
				}

				return(true);
			}
		});

		return(set);
	}
}
