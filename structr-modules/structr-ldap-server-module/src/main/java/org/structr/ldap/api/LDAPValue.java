/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.ldap.api;

import org.structr.common.error.FrameworkException;

/**
 *
 * @author Christian Morgner
 */
public interface LDAPValue {

	public String getStringValue();
	public void setStringValue(final String value) throws FrameworkException;
}
