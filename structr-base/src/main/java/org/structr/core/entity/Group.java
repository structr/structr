package org.structr.core.entity;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;

public interface Group extends Principal {

	Iterable<Principal> getMembers();
	void addMember(final SecurityContext securityContext, final Principal user) throws FrameworkException;
	void removeMember(final SecurityContext securityContext, final Principal member) throws FrameworkException;
}
