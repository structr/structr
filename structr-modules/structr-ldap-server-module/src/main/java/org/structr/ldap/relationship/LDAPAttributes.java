/**
 * Copyright (C) 2010-2016 Structr GmbH
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
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.ldap.relationship;

import org.structr.core.entity.OneToMany;
import org.structr.core.entity.Relation;
import org.structr.ldap.entity.LDAPAttributeImpl;
import org.structr.ldap.entity.LDAPNodeImpl;

/**
 *
 */
public class LDAPAttributes extends OneToMany<LDAPNodeImpl, LDAPAttributeImpl> {

	@Override
	public Class<LDAPNodeImpl> getSourceType() {
		return LDAPNodeImpl.class;
	}

	@Override
	public Class<LDAPAttributeImpl> getTargetType() {
		return LDAPAttributeImpl.class;
	}

	@Override
	public String name() {
		return "LDAP_ATTR";
	}

	@Override
	public int getCascadingDeleteFlag() {
		return Relation.SOURCE_TO_TARGET;
	}
}
