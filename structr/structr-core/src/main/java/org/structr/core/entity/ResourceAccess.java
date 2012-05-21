/*
 *  Copyright (C) 2012 Axel Morgner
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

import java.util.logging.Logger;
import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.node.NodeService;

/**
 * The database representation of a REST resource.
 * 
 * This class exists because we need to be able to control access to
 * specific REST resources in the database, i.e. grant access to
 * Principals (Users or Groups etc.).
 * 
 * ResourceAccess objects are created automatically for every
 * REST resource on access. The
 * 
 * @author Christian Morgner
 */
public class ResourceAccess extends AbstractNode {

	private static final Logger logger = Logger.getLogger(ResourceAccess.class.getName());

	private String cachedUriPart = null;
	private Long cachedFlags     = null;
	
	//~--- static initializers --------------------------------------------

	static {

		EntityContext.registerPropertySet(ResourceAccess.class, PropertyView.All, Key.values());
		EntityContext.registerPropertySet(ResourceAccess.class, PropertyView.Ui, Key.values());

		EntityContext.registerSearchablePropertySet(ResourceAccess.class, NodeService.NodeIndex.fulltext.name(), Key.values());
		EntityContext.registerSearchablePropertySet(ResourceAccess.class, NodeService.NodeIndex.keyword.name(),  Key.values());
		
		// uri and type must be read-only
		EntityContext.registerWriteOnceProperty(ResourceAccess.class, AbstractNode.Key.type.name());
		EntityContext.registerWriteOnceProperty(ResourceAccess.class, Key.uri.name());

	}

	//~--- constant enums -------------------------------------------------

	public enum Key implements PropertyKey {

		uri, flags

	}

	//~--- methods --------------------------------------------------------

	@Override
	public String toString() {
		
		StringBuilder buf = new StringBuilder();
		
		buf.append("('").append(getUriPart()).append("', flags: ").append(getFlags()).append(")");
		
		return buf.toString();
	}
	
	public boolean hasFlag(long flag) {
		return (getFlags() & flag) == flag;
	}
	
	public void setFlag(long flag) throws FrameworkException {
		
		// reset cached field
		cachedFlags = null;

		// set modified property
		setProperty(Key.flags, getFlags() | flag);
	}
	
	public void clearFlag(long flag) throws FrameworkException {
		
		// reset cached field
		cachedFlags = null;

		// set modified property
		setProperty(Key.flags, getFlags() & ~flag);
	}
	
	public long getFlags() {
		
		if(cachedFlags == null) {
			cachedFlags = getLongProperty(Key.flags);
		}
		
		if(cachedFlags != null) {
			return cachedFlags.longValue();
		}
		
		return 0;
	}
	
	public String getUriPart() {
		
		if(cachedUriPart == null) {
			cachedUriPart = getStringProperty(Key.uri);
		}
		
		return cachedUriPart;
	}
}
