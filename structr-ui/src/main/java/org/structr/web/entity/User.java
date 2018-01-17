/**
 * Copyright (C) 2010-2018 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.entity;

import java.net.URI;
import org.structr.api.config.Settings;
import org.structr.common.ConstantBooleanTrue;
import org.structr.common.KeyAndClass;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.property.EndNode;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StartNode;
import org.structr.schema.SchemaService;
import org.structr.schema.json.JsonObjectType;
import org.structr.schema.json.JsonSchema;
import org.structr.web.entity.relation.UserHomeDir;
import org.structr.web.entity.relation.UserImage;
import org.structr.web.entity.relation.UserWorkDir;
import org.structr.web.property.ImageDataProperty;

public interface User extends Principal {

	static class Impl { static {

		final JsonSchema schema    = SchemaService.getDynamicSchema();
		final JsonObjectType user  = schema.addType("User");

		user.setExtends(schema.getType("Principal"));
		user.setImplements(URI.create("https://structr.org/v1.1/definitions/User"));

		user.addStringProperty("confirmationKey").setIndexed(true);
		user.addStringProperty("twitterName").setIndexed(true);
		user.addStringProperty("localStorage");

		user.addBooleanProperty("backendUser").setIndexed(true);
		user.addBooleanProperty("frontendUser").setIndexed(true);
		user.addBooleanProperty("isUser", PropertyView.Public).setReadOnly(true).addTransformer(ConstantBooleanTrue.class.getName());
		user.addBooleanProperty("skipSecurityRelationships").setDefaultValue("false").setIndexed(true);

		user.addPropertySetter("localStorage", String.class);
		user.addPropertyGetter("localStorage", String.class);

		user.overrideMethod("shouldSkipSecurityRelationships", false, "return getProperty(skipSecurityRelationshipsProperty);");

		user.overrideMethod("onCreation",     true, "org.structr.web.entity.User.checkAndCreateHomeDirectory(this, securityContext);");
		user.overrideMethod("onModification", true, "org.structr.web.entity.User.checkAndCreateHomeDirectory(this, securityContext);");
		user.overrideMethod("onDeletion",     true, "org.structr.web.entity.User.checkAndRemoveHomeDirectory(this, securityContext);");

		user.addMethod("isFrontendUser").setReturnType("boolean").setSource("return getProperty(frontendUserProperty);");
		user.addMethod("isBackendUser").setReturnType("boolean").setSource("return getProperty(backendUserProperty);");

		user.addViewProperty(PropertyView.Public, "name");

	}}

	//public static final Property<String>            confirmationKey           = new StringProperty("confirmationKey").indexed();
	//public static final Property<Boolean>           backendUser               = new BooleanProperty("backendUser").indexed();
	//public static final Property<Boolean>           frontendUser              = new BooleanProperty("frontendUser").indexed();
	public static final Property<Image>             img                       = new StartNode<>("img", UserImage.class);
	public static final ImageDataProperty           imageData                 = new ImageDataProperty("imageData", new KeyAndClass(img, Image.class));
	public static final Property<Folder>            homeDirectory             = new EndNode<>("homeDirectory", UserHomeDir.class);
	public static final Property<Folder>            workingDirectory          = new EndNode<>("workingDirectory", UserWorkDir.class);
	//public static final Property<List<Group>>       groups                    = new StartNodes<>("groups", Groups.class, new UiNotion());
	//public static final Property<Boolean>           isUser                    = new ConstantBooleanProperty("isUser", true);
	//public static final Property<String>            twitterName               = new StringProperty("twitterName").cmis().indexed();
	//public static final Property<String>            localStorage              = new StringProperty("localStorage");
	//public static final Property<List<Favoritable>> favorites                 = new EndNodes<>("favorites", UserFavoriteFavoritable.class);
	//public static final Property<Boolean>           skipSecurityRelationships = new BooleanProperty("skipSecurityRelationships").defaultValue(Boolean.FALSE).indexed().readOnly();

	/*
	@Override
	public boolean isValid(ErrorBuffer errorBuffer) {

		if ( getProperty(skipSecurityRelationships).equals(Boolean.TRUE) && !isAdmin()) {

			errorBuffer.add(new SemanticErrorToken(getClass().getSimpleName(), skipSecurityRelationships, "can_only_be_set_for_admin_accounts"));
			return false;
		}

		return super.isValid(errorBuffer);
	}
	*/

	String getLocalStorage();
	void setLocalStorage(final String localStorage) throws FrameworkException;

	boolean isBackendUser();
	boolean isFrontendUser();

	// ----- public static methods -----
	public static void checkAndCreateHomeDirectory(final User user, final SecurityContext securityContext) throws FrameworkException {

		if (Settings.FilesystemEnabled.getValue()) {

			final PropertyKey<Folder> homeFolderKey = StructrApp.key(AbstractFile.class, "homeFolderOfUser");
			final PropertyKey<Folder> parentKey     = StructrApp.key(AbstractFile.class, "parent");

			// use superuser context here
			final SecurityContext storedContext = user.getSecurityContext();

			try {

				user.setSecurityContext(SecurityContext.getSuperUserInstance());

				Folder homeDir = user.getProperty(User.homeDirectory);
				if (homeDir == null) {

					// create home directory
					final App app     = StructrApp.getInstance();
					Folder homeFolder = app.nodeQuery(Folder.class).and(Folder.name, "home").and(parentKey, null).getFirst();

					if (homeFolder == null) {

						homeFolder = app.create(Folder.class,
							new NodeAttribute(Folder.name, "home"),
							new NodeAttribute(Folder.owner, null),
							new NodeAttribute(Folder.visibleToAuthenticatedUsers, true)
						);
					}

					app.create(Folder.class,
						new NodeAttribute(Folder.name, user.getUuid()),
						new NodeAttribute(Folder.owner, user),
						new NodeAttribute(Folder.visibleToAuthenticatedUsers, true),
						new NodeAttribute(parentKey, homeFolder),
						new NodeAttribute(homeFolderKey, user)
					);
				}

			} catch (Throwable t) {


			} finally {

				// restore previous context
				user.setSecurityContext(storedContext);
			}
		}
	}

	public static void checkAndRemoveHomeDirectory(final User user, final SecurityContext securityContext) throws FrameworkException {

		if (Settings.FilesystemEnabled.getValue()) {

			// use superuser context here
			final SecurityContext storedContext = user.getSecurityContext();

			try {

				user.setSecurityContext(SecurityContext.getSuperUserInstance());

				final Folder homeDir = user.getProperty(User.homeDirectory);
				if (homeDir != null) {

					StructrApp.getInstance().delete(homeDir);
				}

			} catch (Throwable t) {

				t.printStackTrace();

			} finally {

				// restore previous context
				user.setSecurityContext(storedContext);
			}

		}
	}
}
