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
 * @author Christian Morgner
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
