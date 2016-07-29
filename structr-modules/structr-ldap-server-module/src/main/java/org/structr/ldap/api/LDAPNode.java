/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.ldap.api;

import java.util.List;
import java.util.Set;
import org.apache.directory.shared.ldap.model.entry.Value;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;

/**
 *
 * @author Christian Morgner
 */
public interface LDAPNode extends NodeInterface {

	String getUserProvidedName();
	String getRdn();

	LDAPNode getChild(final String normalizedName) throws FrameworkException;
	LDAPNode createChild(final String normalizedName, final String userProvidedName, final String structuralObjectClass, final Set<String> objectClasses) throws FrameworkException;

	List<LDAPNode> getChildren();
	LDAPNode getParent();

	List<LDAPAttribute> getAttributes();
	LDAPAttribute createAttribute(final String oid, final String userProvidedId, final Iterable<Value<?>> values) throws FrameworkException;

	void delete() throws FrameworkException;
}
