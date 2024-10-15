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

import org.structr.common.*;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.relationship.GroupCONTAINSPrincipal;
import org.structr.core.entity.relationship.PrincipalFAVORITEFavoritable;
import org.structr.core.entity.relationship.PrincipalOwnsNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;

import java.util.Date;

public interface PrincipalInterface extends NodeInterface, AccessControllable {

	Object HIDDEN                            = "****** HIDDEN ******";
	String SUPERUSER_ID                      = "00000000000000000000000000000000";
	String ANONYMOUS                         = "anonymous";
	String ANYONE                            = "anyone";

	Property<Iterable<Favoritable>> favoritesProperty = new EndNodes<>("favorites", PrincipalFAVORITEFavoritable.class);
	Property<Iterable<Group>> groupsProperty          = new StartNodes<>("groups", GroupCONTAINSPrincipal.class);

	Property<Iterable<NodeInterface>> ownedNodes   = new EndNodes<>("ownedNodes", PrincipalOwnsNode.class).partOfBuiltInSchema();
	Property<Iterable<NodeInterface>> grantedNodes = new EndNodes<>("grantedNodes", Security.class).partOfBuiltInSchema();

	Property<Boolean> isAdminProperty                           = new BooleanProperty("isAdmin").indexed().readOnly();
	Property<Boolean> blockedProperty                           = new BooleanProperty("blocked");
	Property<String> sessionIdsProperty                         = new ArrayProperty("sessionIds", String.class).indexed();
	Property<String> refreshTokensProperty                      = new ArrayProperty("refreshTokens", String.class).indexed();
	Property<String> sessionDataProperty                        = new StringProperty("sessionData");
	Property<String> eMailProperty                              = new StringProperty("eMail").indexed().unique().transformators(LowercaseTransformator.class.getName(), TrimTransformator.class.getName());
	Property<String> passwordProperty                           = new PasswordProperty("password");
	Property<Date> passwordChangeDateProperty                   = new DateProperty("passwordChangeDate");
	Property<Integer> passwordAttemptsProperty                  = new IntProperty("passwordAttempts");
	Property<Date> lastLoginDateProperty                        = new DateProperty("lastLoginDate");
	Property<String> twoFactorSecretProperty                    = new StringProperty("twoFactorSecret");
	Property<String> twoFactorTokenProperty                     = new StringProperty("twoFactorToken").indexed();
	Property<Boolean> isTwoFactorUserProperty                   = new BooleanProperty("isTwoFactorUser");
	Property<Boolean> twoFactorConfirmedProperty                = new BooleanProperty("twoFactorConfirmed");
	Property<String> saltProperty                               = new StringProperty("salt");
	Property<String> localeProperty                             = new StringProperty("locale");
	Property<String> publicKeyProperty                          = new StringProperty("publicKey");
	Property<String> proxyUrlProperty                           = new StringProperty("proxyUrl");
	Property<String> proxyUsernameProperty                      = new StringProperty("proxyUsername");
	Property<String> proxyPasswordProperty                      = new StringProperty("proxyPassword");
	Property<String> publicKeysProperty                         = new ArrayProperty("publicKeys", String.class);
	Property<String> customPermissionQueryReadProperty          = new StringProperty("customPermissionQueryRead");
	Property<String> customPermissionQueryWriteProperty         = new StringProperty("customPermissionQueryWrite");
	Property<String> customPermissionQueryDeleteProperty        = new StringProperty("customPermissionQueryDelete");
	Property<String> customPermissionQueryAccessControlProperty = new StringProperty("customPermissionQueryAccessControl");

	View uiView = new View(PrincipalInterface.class, PropertyView.Ui,
		blockedProperty
	);

	Iterable<Favoritable> getFavorites();
	Iterable<Group> getGroups();
	String getSessionData();
	String getEMail();
	void setSessionData(final String sessionData) throws FrameworkException;
	boolean isAdmin();
	boolean isBlocked();
	void setFavorites(final Iterable<Favoritable> favorites) throws FrameworkException;
	void setIsAdmin(final boolean isAdmin) throws FrameworkException;
	void setPassword(final String password) throws FrameworkException;
	void setEMail(final String eMail) throws FrameworkException;
	void setSalt(final String salt) throws FrameworkException;
	String getLocale();
	boolean shouldSkipSecurityRelationships();
	Iterable<PrincipalInterface> getParents();
	Iterable<PrincipalInterface> getParentsPrivileged();
	boolean addSessionId(final String sessionId);
	boolean addRefreshToken(final String refreshToken);
	void removeSessionId(final String sessionId);
	void removeRefreshToken(final String refreshToken);
	void clearTokens();
	boolean isValidPassword(final String password);
	String getEncryptedPassword();
	String getSalt();
	String getTwoFactorSecret();
	String getTwoFactorUrl();

	default void onAuthenticate() {}
}
