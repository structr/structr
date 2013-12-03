package org.structr.web.entity.relation;

import org.structr.core.entity.ManyToMany;
import org.structr.core.entity.Principal;
import org.structr.web.entity.Group;

/**
 *
 * @author Christian Morgner
 */
public class Groups extends ManyToMany<Group, Principal> {

	@Override
	public Class<Group> getSourceType() {
		return Group.class;
	}

	@Override
	public Class<Principal> getTargetType() {
		return Principal.class;
	}

	@Override
	public String name() {
		return "CONTAINS";
	}
}
