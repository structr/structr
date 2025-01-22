/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.ldap;

import com.google.gson.GsonBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.CursorLdapReferralException;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.service.*;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Group;
import org.structr.core.traits.definitions.GroupTraitDefinition;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.schema.SchemaService;

import java.io.IOException;
import java.util.*;

/**
 * The LDAP synchronization service. This is a system service that requires
 * superuser permissions.
 */
@ServiceDependency(SchemaService.class)
@StopServiceForMaintenanceMode
public class LDAPService extends Thread implements SingletonService {

	private static final Logger logger   = LoggerFactory.getLogger(LDAPService.class.getName());
	private final long connectionTimeout = 1000;

	public LDAPService() {

		super("Structr LDAP Service");
	}

	// ----- public methods -----
	public void synchronizeGroup(final LDAPGroup group)  throws IOException, LdapException, CursorException, FrameworkException {

		final String scope                           = getScope();
		final String secret                          = getSecret();
		final String bindDn                          = getBindDN();
		final String host                            = getHost();
		final int port                               = getPort();
		final boolean useSsl                         = getUseSSL();
		final LdapConnection connection              = new LdapNetworkConnection(host, port, useSsl);
		final String groupDn                         = group.getDistinguishedName();
		final String groupFilter                     = group.getFilter();
		final String groupScope                      = group.getScope();
		final String groupPath                       = group.getPath();
		final String groupName                       = group.getName();
		final boolean useDistinguishedName           = StringUtils.isNotBlank(groupDn);
		final boolean useFilterAndScope              = StringUtils.isNotBlank(groupPath) && StringUtils.isNotBlank(groupFilter) && StringUtils.isNotBlank(groupScope);

		if (!useDistinguishedName && !useFilterAndScope) {

			logger.warn("Unable to update LDAP group {}: set distinguishedName or [path, filter and scope] to sync this group.", groupName);

			return;
		}

		connection.setTimeOut(connectionTimeout);

		try {

			if (connection.connect()) {

				if (StringUtils.isNotBlank(bindDn) && StringUtils.isNotBlank(secret)) {

					connection.bind(bindDn, secret);

				} else if (StringUtils.isNotBlank(bindDn)) {

					connection.bind(bindDn);
				}

				// decide which method to use for synchronization
				if (useDistinguishedName) {

					logger.debug("Updating LDAPGroup {} ({}) with DN {} on LDAP server {}:{}..", groupName, group.getUuid(), groupDn, host, port);

					// use dn
					updateWithGroupDn(group, connection, groupDn, scope);

				} else if (useFilterAndScope) {

					// use filter + scope
					logger.debug("Updating LDAPGroup {} ({}) with path {}, filter {} and scope {} on LDAP server {}:{}..", groupName, group.getUuid(), groupPath, groupFilter, groupScope, host, port);

					updateWithFilterAndScope(group, connection, groupPath, groupFilter, groupScope);
				}

				connection.unBind();
			}

		} catch (Throwable t) {

			logger.warn("Unable to sync group {}: {}", group.getName(), t.getMessage());

		} finally {

			connection.close();
		}

		// check if we need to delete users
		checkAndUpdateUsers();
	}

	public boolean canSuccessfullyBind(final String dn, final String secret) {

		final String host               = getHost();
		final int port                  = getPort();
		final boolean useSsl            = getUseSSL();
		final LdapConnection connection = new LdapNetworkConnection(host, port, useSsl);

		connection.setTimeOut(connectionTimeout);

		try {
			if (connection.connect()) {

				connection.bind(dn, secret);
				connection.unBind();
			}

			connection.close();

			return true;

		} catch (LdapException | IOException ex) {
			logger.warn("Cannot bind {} on LDAP server {}: {}", dn, (host + ":" + port), ex.getMessage());
		}

		return false;
	}

	public Map<String, String> getPropertyMapping() {
		return new GsonBuilder().create().fromJson(Settings.LDAPPropertyMapping.getValue("{ sn: name, email: eMail }"), Map.class);
	}

	public Map<String, String> getGroupMapping() {
		return new GsonBuilder().create().fromJson(Settings.LDAPGroupNames.getValue("{ group: member, groupOfNames: member, groupOfUniqueNames: uniqueMember }"), Map.class);
	}

	public String getHost() {
		return Settings.LDAPHost.getValue("localhost");
	}

	public int getPort() {
		return Settings.LDAPPort.getValue(389);
	}

	public String getBindDN() {
		return Settings.LDAPBindDN.getValue("");
	}

	public String getSecret() {
		return Settings.LDAPSecret.getValue("");
	}

	public String getScope() {
		return Settings.LDAPScope.getValue("SUBTREE");
	}

	public boolean getUseSSL() {
		return Settings.LDAPUseSSL.getValue(false);
	}

	public void checkAndUpdateUsers() {

		new Thread(() -> {

			// wait for the enclosing transaction to finish
			try { Thread.sleep(1000); } catch (Throwable t) {}

			final SecurityContext securityContext = SecurityContext.getSuperUserInstance();
			final App app                         = StructrApp.getInstance(securityContext);

			try (final Tx tx = app.tx()) {

				for (final LDAPUser member : app.nodeQuery("LDAPUser").getResultStream()) {

					boolean hasLDAPGroups = false;

					for (final Group group : member.getGroups()) {

						if (group instanceof LDAPGroup) {

							hasLDAPGroups = true;
							break;
						}
					}

					if (!hasLDAPGroups) {

						logger.warn("LDAPUser {} with UUID {} is not associated with an LDAPGroup, removing.", member.getName(), member.getUuid());
						app.delete(member);
					}
				}

				tx.success();

			} catch (FrameworkException fex) {
				logger.warn("Unable to update LDAP users: {}", fex.getMessage());
			}

		}).start();
	}

	// ----- private methods -----
	private LDAPUser getOrCreateUser(final Entry userEntry) throws FrameworkException {

		final App app                = StructrApp.getInstance();
		final PropertyMap attributes = new PropertyMap();
		final String originId        = getOriginId(userEntry);

		if (originId != null) {

			attributes.put(StructrApp.key(LDAPUser.class, "originId"), originId);

			LDAPUser user = app.nodeQuery("LDAPUser").and(attributes).getFirst();
			if (user == null) {

				logger.debug("Creating new user for originId {}", originId);

				user = app.create("LDAPUser", attributes);
				if (user != null) {

					logger.debug("User created: {}", user.getUuid());
				}

			} else {

				logger.debug("Existing user {} found for originId {}", user.getUuid(), originId);
			}

			// update user
			user.initializeFrom(userEntry);

			return user;

		} else {

			logger.warn("No origin ID from user entry: {}", userEntry);
		}

		return null;
	}

	// ----- private methods -----
	private void updateWithGroupDn(final LDAPGroup group, final LdapConnection connection, final String groupDn, final String scope) throws IOException, LdapException, CursorException, FrameworkException {

		final Map<String, String> possibleGroupNames = getGroupMapping();
		final List<LDAPUser> members                 = new LinkedList<>();
		final Set<String> memberDNs                  = new LinkedHashSet<>();

		// fetch DNs of all group members
		try (final EntryCursor cursor = connection.search(groupDn, "(objectclass=*)", SearchScope.valueOf(scope))) {

			while (cursor.next()) {

				final Entry entry           = cursor.get();
				final Attribute objectClass = entry.get("objectclass");

				for (final java.util.Map.Entry<String, String> groupEntry : possibleGroupNames.entrySet()) {

					final String possibleGroupName  = groupEntry.getKey();
					final String possibleMemberName = groupEntry.getValue();

					if (objectClass.contains(possibleGroupName)) {

						// add group members
						final Attribute groupMembers = entry.get(possibleMemberName);
						if (groupMembers != null) {

							for (final Value value : groupMembers) {

								memberDNs.add(value.getString());
							}
						}
					}
				}
			}
		}

		// resolve users
		for (final String memberDN : memberDNs) {

			try (final EntryCursor cursor = connection.search(memberDN, "(objectclass=*)", SearchScope.valueOf(scope))) {

				while (cursor.next()) {

					final Entry entry   = cursor.get();
					final LDAPUser user = getOrCreateUser(entry);

					members.add(user);
				}
			}
		}

		logger.info("{} users updated: {}", members.size(), members);

		// update members of group to new state (will remove all members that are not part of the group, as expected)
		group.setProperty(StructrApp.key(GroupTraitDefinition.class, "members"), members);
	}

	private void updateWithFilterAndScope(final LDAPGroup group, final LdapConnection connection, final String path, final String groupFilter, final String groupScope) throws IOException, LdapException, CursorException, FrameworkException {

		final Map<String, String> possibleGroupNames = getGroupMapping();
		final List<LDAPUser> members                 = new LinkedList<>();
		final Set<String> memberDNs                  = new LinkedHashSet<>();

		// fetch DNs of all group members
		try (final EntryCursor cursor = connection.search(path, groupFilter, SearchScope.valueOf(groupScope))) {

			while (cursor.next()) {

				try {
					final Entry entry           = cursor.get();
					final Attribute objectClass = entry.get("objectclass");

					for (final java.util.Map.Entry<String, String> groupEntry : possibleGroupNames.entrySet()) {

						final String possibleGroupName  = groupEntry.getKey();
						final String possibleMemberName = groupEntry.getValue();

						if (objectClass.contains(possibleGroupName)) {

							// add group members
							final Attribute groupMembers = entry.get(possibleMemberName);
							if (groupMembers != null) {

								for (final Value value : groupMembers) {

									memberDNs.add(value.getString());
								}
							}
						}
					}

				} catch (CursorLdapReferralException e) {

					// ignore, cannot be handled here yet
					//logger.info("CursorLdapReferralException caught, info: {}, remaining DN: {}, resolved object: {}, result code: {}", e.getReferralInfo(), e.getRemainingDn(), e.getResolvedObject(), e.getResultCode());
				}
			}
		}

		// resolve users
		for (final String memberDN : memberDNs) {

			try (final EntryCursor cursor = connection.search(memberDN, "(objectclass=*)", SearchScope.OBJECT)) {

				while (cursor.next()) {

					final Entry entry   = cursor.get();
					final LDAPUser user = getOrCreateUser(entry);

					members.add(user);
				}
			}
		}

		logger.info("{} users updated: {}", members.size(), members);

		// update members of group to new state (will remove all members that are not part of the group, as expected)
		group.setProperty(StructrApp.key(GroupTraitDefinition.class, "members"), members);
	}

	// ----- interface SingletonService -----
	@Override
	public boolean isRunning() {
		return true;
	}

	@Override
	public void injectArguments(final Command command) {
	}

	@Override
	public ServiceResult initialize(final StructrServices services, String serviceName) throws ReflectiveOperationException {

		logger.info("host:    {}", Settings.LDAPHost.getValue("localhost"));
		logger.info("port:    {}", Settings.LDAPPort.getValue(389));
		logger.info("use SSL: {}", Settings.LDAPUseSSL.getValue(false));
		logger.info("bind DN: {}", Settings.LDAPBindDN.getValue(""));
		logger.info("scope:   {}", Settings.LDAPScope.getValue("SUBTREE"));
		logger.info("mapping: {}", Settings.LDAPPropertyMapping.getValue("{ sn: name, email: eMail }"));
		logger.info("groups:  {}", Settings.LDAPGroupNames.getValue("{ group: member, groupOfNames: member, groupOfUniqueNames: uniqueMember }"));

		return new ServiceResult(true);
	}

	@Override
	public void shutdown() {
	}

	@Override
	public void initialized() {
	}

	@Override
	public boolean isVital() {
		return false;
	}

	@Override
	public boolean waitAndRetry() {
		return false;
	}

	// ----- interface Feature -----
	@Override
	public String getModuleName() {
		return "ldap-client";
	}

	// ----- private methods -----
	private String getOriginId(final Entry userEntry) {

		final String originIdName = Settings.LDAPPrimaryKey.getValue("dn");
		if (StringUtils.isNotBlank(originIdName)) {

			if ("dn".equalsIgnoreCase(originIdName) || "distinguishedName".equalsIgnoreCase(originIdName)) {

				return userEntry.getDn().getNormName();

			} else {

				final Attribute originAttr = userEntry.get(originIdName);
				if (originAttr != null) {

					if (originAttr.isHumanReadable()) {

						try {

							return originAttr.getString();

						} catch (LdapInvalidAttributeValueException lex) {
							logger.warn("Invalid LDAP value, expected string: {}", originAttr);
						}

					} else {

						try {

							final byte[] bytes = originAttr.getBytes();
							return Base64.getEncoder().encodeToString(bytes);

						} catch (LdapInvalidAttributeValueException lex) {
							logger.warn("Invalid LDAP value, expected binary: {}", originAttr);
						}

					}

				} else {

					logger.warn("Cannot find attribute {} in user entry {} but configured to be used as origin ID in structr.conf.", originIdName, userEntry.toString());
				}
			}

		} else {

			logger.warn("Invalid (empty) origin ID attribute configured in structr.conf.");
		}

		return null;
	}
}

