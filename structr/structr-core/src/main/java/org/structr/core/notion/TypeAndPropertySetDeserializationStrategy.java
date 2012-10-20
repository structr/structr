/*
 *  Copyright (C) 2010-2012 Axel Morgner
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

import java.util.*;
import java.util.Map.Entry;
import org.structr.common.Property;
import org.structr.common.PropertyKey;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchNodeCommand;

//~--- JDK imports ------------------------------------------------------------

import org.structr.common.error.FrameworkException;
import org.structr.common.error.PropertiesNotFoundToken;
import org.structr.common.error.TypeToken;
import org.structr.core.PropertySet;
import org.structr.core.Result;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class TypeAndPropertySetDeserializationStrategy implements DeserializationStrategy {

	protected PropertyKey[] propertyKeys = null;

	//~--- constructors ---------------------------------------------------

	public TypeAndPropertySetDeserializationStrategy(PropertyKey... propertyKeys) {
		this.propertyKeys = propertyKeys;
	}

	//~--- methods --------------------------------------------------------

	@Override
	public GraphObject deserialize(SecurityContext securityContext, Class<? extends GraphObject> type, Object source) throws FrameworkException {

		if (source instanceof PropertySet) {

			Map<String, Object> attributes  = new LinkedHashMap<String, Object>();
			List<SearchAttribute> attrs     = new LinkedList<SearchAttribute>();
			GraphObject typeInstance        = null;
			
			// try to determine type 
			try { typeInstance = type.newInstance(); } catch(Throwable t) {}
			
			for (Entry<String, Object> entry : ((PropertySet) source).getAttributes().entrySet()) {

				String value    = (String) entry.getValue();
				PropertyKey key = typeInstance != null ?
							typeInstance.getPropertyKeyForName(entry.getKey())
						  :
							new Property(entry.getKey());

				attrs.add(Search.andExactProperty(key, value));
				attributes.put(key.name(), value);
			}

			// just check for existance
			Result result = (Result) Services.command(securityContext, SearchNodeCommand.class).execute(null, false, false, attrs);
			if (result.size() == 1) {
				
				GraphObject obj = result.get(0);
				
				if(!type.isAssignableFrom(obj.getClass())) {
					throw new FrameworkException(type.getSimpleName(), new TypeToken(AbstractNode.base, type.getSimpleName()));
				}
				return result.get(0);
			}

			throw new FrameworkException(type.getSimpleName(), new PropertiesNotFoundToken(AbstractNode.base, attributes));
		}

		return null;
	}
}
