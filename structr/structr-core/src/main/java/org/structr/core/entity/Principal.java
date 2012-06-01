/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
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

import org.structr.common.AccessControllable;
import org.structr.common.Permission;
import org.structr.common.PropertyKey;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;

//~--- interfaces -------------------------------------------------------------

/**
 *
 * @author amorgner
 *
 */
public interface Principal extends GraphObject {

	public enum Key implements PropertyKey{ sessionId, groups, blocked }

	//public enum UserIndexKey implements PropertyKey{ name, email; }

	//~--- methods --------------------------------------------------------

	public void block() throws FrameworkException;

	public void grant(final Permission permission, final AccessControllable obj);

	public void revoke(final Permission permission, final AccessControllable obj);

	//~--- get methods ----------------------------------------------------

	public List<Principal> getParents();

	public String getEncryptedPassword();

	public Boolean getBlocked();

	public Boolean isBlocked();

	//~--- set methods ----------------------------------------------------

	public void setBlocked(final Boolean blocked) throws FrameworkException;

}
