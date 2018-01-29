/**
 * Copyright (C) 2010-2018 Structr GmbH
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

import java.net.URI;
import java.util.List;
import org.structr.common.ConstantBooleanTrue;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.property.PropertyKey;
import org.structr.schema.SchemaService;
import org.structr.schema.json.JsonObjectType;
import org.structr.schema.json.JsonSchema;

/**
 */
public interface Group extends Principal {

	static class Impl { static {

		final JsonSchema schema        = SchemaService.getDynamicSchema();
		final JsonObjectType group     = schema.addType("Group");
		final JsonObjectType principal = schema.addType("Principal");

		group.setImplements(URI.create("https://structr.org/v1.1/definitions/Group"));
		group.setExtends(URI.create("#/definitions/Principal"));

		group.addBooleanProperty("isGroup", PropertyView.Public).setReadOnly(true).addTransformer(ConstantBooleanTrue.class.getName());

		group.addMethod("addMember").setSource("org.structr.core.entity.Group.addMember(this, member);").addParameter("member", Principal.class.getName());
		group.addMethod("removeMember").setSource("org.structr.core.entity.Group.removeMember(this, member);").addParameter("member", Principal.class.getName());

		// create relationship
		group.relate(principal, "CONTAINS", Relation.Cardinality.ManyToMany, "groups", "members");

		// make name visible in public view
		group.addViewProperty(PropertyView.Public, "members");
		group.addViewProperty(PropertyView.Public, "name");
	}}

	void addMember(final Principal member);
	void removeMember(final Principal member);


	public static void addMember(final Group group, final Principal user) {

		try {
			final PropertyKey<List<Principal>> key = StructrApp.key(group.getClass(), "members");
			final List<Principal> _users           = group.getProperty(key);

			_users.add(user);

			group.setProperty(key, _users);

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}
	}

	public static void removeMember(final Group group, final Principal member) {

		try {

			final PropertyKey<List<Principal>> key = StructrApp.key(group.getClass(), "members");
			final List<Principal> _users           = group.getProperty(key);

			_users.remove(member);

			group.setProperty(key, _users);

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}
	}
}
