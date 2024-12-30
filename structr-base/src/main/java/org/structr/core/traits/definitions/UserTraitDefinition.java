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
package org.structr.core.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.wrappers.UserTraitWrapper;
import org.structr.web.entity.User;

import java.util.Map;
import java.util.Set;

public final class UserTraitDefinition extends AbstractTraitDefinition {

	public UserTraitDefinition() {
		super("User");
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			User.class, (traits, node) -> new UserTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<NodeInterface> homeDirectoryProperty       = new EndNode("homeDirectory", "UserHOME_DIRFolder").partOfBuiltInSchema();
		final Property<NodeInterface> workingDirectoryProperty    = new EndNode("workingDirectory", "UserWORKING_DIRFolder").partOfBuiltInSchema();
		final Property<NodeInterface> imgProperty                 = new StartNode("img", "ImagePICTURE_OFUser");
		final Property<String> confirmationKeyProperty            = new StringProperty("confirmationKey").indexed().partOfBuiltInSchema();
		final Property<String> localStorageProperty               = new StringProperty("localStorage").partOfBuiltInSchema();
		final Property<Boolean> skipSecurityRelationshipsProperty = new BooleanProperty("skipSecurityRelationships").defaultValue(false).indexed().partOfBuiltInSchema();
		final Property<Boolean> isUserProperty                    = new ConstantBooleanProperty("isUser", true).partOfBuiltInSchema();

		return Set.of(
			homeDirectoryProperty,
			workingDirectoryProperty,
			imgProperty,
			confirmationKeyProperty,
			localStorageProperty,
			skipSecurityRelationshipsProperty,
			isUserProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(

			PropertyView.Public,
			Set.of("isUser", "name"),

			PropertyView.Ui,
			Set.of(
				"isUser", "confirmationKey", "eMail", "favorites", "groups", "homeDirectory", "isAdmin", "locale", "password", "proxyPassword",
				"proxyUrl", "proxyUsername", "publicKey", "sessionIds", "refreshTokens", "workingDirectory", "twoFactorToken", "isTwoFactorUser",
				"twoFactorConfirmed", "passwordAttempts", "passwordChangeDate", "lastLoginDate", "skipSecurityRelationships", "img"
			)
		);
	}
}
