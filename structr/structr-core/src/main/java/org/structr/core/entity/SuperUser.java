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

/**
 * 
 * @author amorgner
 * 
 */
public class SuperUser extends User {

    public SuperUser() {
    }

    @Override
    public long getId() {
        return -1L;
    }

    @Override
    public String getName() {
        return "superuser";
    }

    @Override
    public void setName(final String name) {
        // not supported
    }

    @Override
    public void setPassword(final String passwordValue) {
        // not supported
    }

    @Override
    public String getRealName() {
        return "Super User";
    }

    @Override
    public void setRealName(final String realName) {
        // not supported
    }

    @Override
    public Boolean getBlocked() {
        return false;
    }

    @Override
    public void setBlocked(final Boolean blocked) {
        // not supported
    }

    @Override
    public Boolean isBlocked() {
        return false;
    }

    @Override
    public void block() {
        // not supported
    }
}
