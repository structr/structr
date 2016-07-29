/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.ldap.api;

import java.util.List;
import org.structr.common.error.FrameworkException;

/**
 *
 * @author Christian Morgner
 */
public interface LDAPAttribute {

	String getOid();
	String getUserProvidedId();

	List<LDAPValue> getValues();
	LDAPValue addValue(final String value) throws FrameworkException;

}
