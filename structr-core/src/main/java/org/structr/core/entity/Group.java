/**
 * Copyright (C) 2010-2017 Structr GmbH
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

		group.setImplements(URI.create("https://structr.org/v1.1/definitions/Group"));
		group.setExtends(URI.create("#/definitions/Principal"));

		group.addMethod("void", "addMember",    "org.structr.core.entity.Principal member", "org.structr.core.entity.Group.addMember(this, member);");
		group.addMethod("void", "removeMember", "org.structr.core.entity.Principal member", "org.structr.core.entity.Group.removeMember(this, member);");
	}}

	void addMember(final Principal member);
	void removeMember(final Principal member);


	public static void addMember(final Principal principal, final Principal user) {

		try {
			final PropertyKey<List<Principal>> key = StructrApp.getConfiguration().getPropertyKeyForJSONName(Group.class, "members");
			final List<Principal> _users           = principal.getProperty(key);

			_users.add(user);

			principal.setProperty(key, _users);

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}
	}

	public static void removeMember(final Principal principal, final Principal member) {

		try {

			final PropertyKey<List<Principal>> key = StructrApp.getConfiguration().getPropertyKeyForJSONName(Group.class, "members");
			final List<Principal> _users           = principal.getProperty(key);

			_users.remove(member);

			principal.setProperty(key, _users);

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}
	}
}
