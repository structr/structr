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

import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.api.util.Iterables;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.Export;
import org.structr.core.entity.relationship.GroupCONTAINSPrincipal;
import org.structr.core.property.ConstantBooleanProperty;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.schema.SchemaService;

import java.util.List;

/**
 */
public class Group extends Principal {

	public static final Property<Iterable<PrincipalInterface>> membersProperty = new EndNodes<>("members", GroupCONTAINSPrincipal.class).partOfBuiltInSchema();
	public static final Property<String> jwksReferenceIdProperty      = new StringProperty("jwksReferenceId").indexed().unique().partOfBuiltInSchema();
	public static final Property<String> nameProperty                 = new StringProperty("name").indexed().notNull().unique().partOfBuiltInSchema();
	public static final Property<Boolean> isGroupProperty             = new ConstantBooleanProperty("isGroup", true).partOfBuiltInSchema();

	static {

		final JsonSchema schema = SchemaService.getDynamicSchema();
		final JsonType type     = schema.addType("Group");

		type.setExtends(Group.class);
	}

	public static final View defaultView = new View(Group.class, PropertyView.Public,
		nameProperty, isGroupProperty, membersProperty, blockedProperty
	);

	public static final View uiView = new View(Group.class, PropertyView.Ui,
		isGroupProperty, jwksReferenceIdProperty, membersProperty
	);

	public Iterable<PrincipalInterface> getMembers() {
		return getProperty(membersProperty);
	}

	@Override
	public boolean isValid(final ErrorBuffer errorBuffer) {

		boolean valid = super.isValid(errorBuffer);

		valid &= ValidationHelper.isValidPropertyNotNull(this, Group.nameProperty, errorBuffer);
		valid &= ValidationHelper.isValidUniqueProperty(this, Group.nameProperty, errorBuffer);
		valid &= ValidationHelper.isValidUniqueProperty(this, Group.jwksReferenceIdProperty, errorBuffer);

		return valid;
	}

	@Export
	public void addMember(final SecurityContext securityContext, final PrincipalInterface user) throws FrameworkException {

		if (user == null) {
			throw new FrameworkException(422, "Unable to add user " + user + " to group " + this);
		}

		final List<PrincipalInterface> _users = Iterables.toList(getProperty(membersProperty));

		_users.add(user);

		setProperty(membersProperty, _users);
	}

	@Export
	public void removeMember(final SecurityContext securityContext, final PrincipalInterface member) throws FrameworkException {

		if (member == null) {
			throw new FrameworkException(422, "Unable to remove member " + member + " from group " + this);
		}

		final List<PrincipalInterface> _users = Iterables.toList(getProperty(membersProperty));

		_users.remove(member);

		setProperty(membersProperty, _users);
	}

	@Override
	public boolean shouldSkipSecurityRelationships() {
		return isAdmin();
	}
}
