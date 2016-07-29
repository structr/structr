/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.ldap.relationship;

import org.structr.core.entity.OneToMany;
import org.structr.core.entity.Relation;
import org.structr.ldap.entity.LDAPAttributeImpl;
import org.structr.ldap.entity.LDAPValueImpl;

/**
 *
 * @author Christian Morgner
 */
public class LDAPValues extends OneToMany<LDAPAttributeImpl, LDAPValueImpl> {

	@Override
	public Class<LDAPAttributeImpl> getSourceType() {
		return LDAPAttributeImpl.class;
	}

	@Override
	public Class<LDAPValueImpl> getTargetType() {
		return LDAPValueImpl.class;
	}

	@Override
	public String name() {
		return "LDAP_VALUE";
	}

	@Override
	public int getCascadingDeleteFlag() {
		return Relation.SOURCE_TO_TARGET;
	}
}
