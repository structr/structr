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



package org.structr.core.notion;

import org.structr.common.ErrorBuffer;
import org.structr.common.PropertyKey;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchNodeCommand;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedList;
import java.util.List;
import org.structr.core.node.NodeAttribute;
import org.structr.rest.wrapper.PropertySet;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class TypeAndPropertySetDeserializationStrategy implements DeserializationStrategy {

	private PropertyKey[] propertyKeys = null;

	//~--- constructors ---------------------------------------------------

	public TypeAndPropertySetDeserializationStrategy(PropertyKey... propertyKeys) {
		this.propertyKeys = propertyKeys;
	}

	//~--- methods --------------------------------------------------------

	@Override
	public GraphObject deserialize(SecurityContext securityContext, Class type, Object source) {

		StringBuilder msgBuffer = new StringBuilder();

		if (source instanceof PropertySet) {

			List<SearchAttribute> attrs          = new LinkedList<SearchAttribute>();

			for (NodeAttribute attr : ((PropertySet) source).getAttributes()) {

				String key = attr.getKey();
				String value = (String) attr.getValue();

				attrs.add(Search.andExactProperty(key, value));
				msgBuffer.append(key).append(" = '").append(value).append("', ");

			}

			// just check for existance
			List<AbstractNode> nodes = (List<AbstractNode>) Services.command(securityContext, SearchNodeCommand.class).execute(null, false, false, attrs);
			ErrorBuffer buf          = new ErrorBuffer();
			int size                 = nodes.size();

			switch (size) {

				case 0 :
					buf.add(type.getSimpleName(), " with ", msgBuffer, " not found.");

					break;

				case 1 :
					return nodes.get(0);

				default :
					buf.add(type.getSimpleName(), " with ", msgBuffer, " is ambiguous.");

			}

			throw new IllegalArgumentException(buf.toString());

		}

		return null;
	}
}
