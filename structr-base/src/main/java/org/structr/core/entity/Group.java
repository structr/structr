package org.structr.core.entity;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.traits.NodeTrait;

public interface Group extends NodeTrait {

	Iterable<Principal> getMembers();
	void addMember(final SecurityContext securityContext, final Principal user) throws FrameworkException;
	void removeMember(final SecurityContext securityContext, final Principal member) throws FrameworkException;
}
