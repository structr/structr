/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
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

import org.neo4j.graphdb.Direction;

import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.entity.RelationClass.Cardinality;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author amorgner
 *
 */
public class Group extends AbstractNode implements Principal {

	static {

		EntityContext.registerPropertySet(Group.class, PropertyView.All, Key.values());
		//EntityContext.registerPropertyRelation(Group.class, Key.users,	Principal.class, RelType.HAS_CHILD, Direction.OUTGOING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Group.class, Principal.class, RelType.MEMBER_OF, Direction.INCOMING, Cardinality.ManyToMany);
	}

        @Override
        public void block() throws FrameworkException {
                throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getEncryptedPassword() {
                throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Object getPropertyForIndexing(String key) {
                throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getPassword() {
                throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getRealName() {
                throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getConfirmationKey() {
                throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Boolean getBlocked() {
                throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getSessionId() {
                throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Boolean isBlocked() {
                throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isBackendUser() {
                throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isFrontendUser() {
                throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setPassword(String passwordValue) throws FrameworkException {
                throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setRealName(String realName) throws FrameworkException {
                throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setBlocked(Boolean blocked) throws FrameworkException {
                throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setConfirmationKey(String value) throws FrameworkException {
                throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setFrontendUser(boolean isFrontendUser) throws FrameworkException {
                throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setBackendUser(boolean isBackendUser) throws FrameworkException {
                throw new UnsupportedOperationException("Not supported yet.");
        }

	//~--- constant enums -------------------------------------------------

	public enum Key implements PropertyKey{ users }

	//~--- get methods ----------------------------------------------------

	@Override
	public String getIconSrc() {
		return "/images/group.png";
	}
}
