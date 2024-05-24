/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.entity;

import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.api.util.Iterables;
import org.structr.common.ConstantBooleanTrue;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.property.PropertyKey;
import org.structr.schema.SchemaService;

import java.net.URI;
import java.util.List;

/**
 */
public interface Group extends Principal {

	static class Impl { static {

		final JsonSchema schema        = SchemaService.getDynamicSchema();
		final JsonObjectType group     = schema.addType("Group");
		final JsonObjectType principal = schema.addType("Principal");

		group.setImplements(URI.create("https://structr.org/v1.1/definitions/Group"));
		group.setExtends(URI.create("#/definitions/Principal"));
		group.setCategory("core");

		group.addStringProperty("appId", PropertyView.Ui)
			.setIndexed(true)
			.setUnique(true);

		group.addStringProperty("name")
			.setIndexed(true)
			.setRequired(true)
			.setUnique(true);

		group.addBooleanProperty("isGroup", PropertyView.Public, PropertyView.Ui).setReadOnly(true).addTransformer(ConstantBooleanTrue.class.getName());
		group.addPropertyGetter("members", Iterable.class);

		group.addMethod("addMember")
				.setSource(Group.class.getName() + ".addMember(this, member, ctx);")
				.addParameter("ctx", SecurityContext.class.getName())
				.addParameter("member", Principal.class.getName())
				.addException(FrameworkException.class.getName())
				.setDoExport(true);

		group.addMethod("removeMember")
				.setSource(Group.class.getName() + ".removeMember(this, member, ctx);")
				.addParameter("ctx", SecurityContext.class.getName())
				.addParameter("member", Principal.class.getName())
				.addException(FrameworkException.class.getName())
				.setDoExport(true);

		// create relationship
		group.relate(principal, "CONTAINS", Cardinality.ManyToMany, "groups", "members");

		// view configuration
		group.addViewProperty(PropertyView.Ui, "members");
		group.addViewProperty(PropertyView.Ui, "customPermissionQueryRead");
		group.addViewProperty(PropertyView.Ui, "customPermissionQueryWrite");
		group.addViewProperty(PropertyView.Ui, "customPermissionQueryDelete");
		group.addViewProperty(PropertyView.Ui, "customPermissionQueryAccessControl");

		group.addViewProperty(PropertyView.Public, "members");
		group.addViewProperty(PropertyView.Public, "blocked");
		group.addViewProperty(PropertyView.Public, "name");
	}}

	void addMember(final SecurityContext ctx, final Principal member) throws FrameworkException;
	void removeMember(final SecurityContext ctx, final Principal member) throws FrameworkException;
	Iterable<Principal> getMembers();


	public static void addMember(final Group group, final Principal user, final SecurityContext ctx) throws FrameworkException {

		if (user == null) {
			throw new FrameworkException(422, "Unable to add user " + user + " to group " + group);
		}

		final PropertyKey<Iterable<Principal>> key = StructrApp.key(group.getClass(), "members");
		final List<Principal> _users               = Iterables.toList(group.getProperty(key));

		_users.add(user);

		group.setProperty(key, _users);
	}

	public static void removeMember(final Group group, final Principal member, final SecurityContext ctx) throws FrameworkException {

		if (member == null) {
			throw new FrameworkException(422, "Unable to remove member " + member + " from group " + group);
		}

		final PropertyKey<Iterable<Principal>> key = StructrApp.key(group.getClass(), "members");
		final List<Principal> _users               = Iterables.toList(group.getProperty(key));

		_users.remove(member);

		group.setProperty(key, _users);
	}
}
