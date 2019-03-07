/**
 * Copyright (C) 2010-2019 Structr GmbH
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

import ch.qos.logback.classic.Level;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.apache.directory.api.ldap.codec.osgi.DefaultLdapCodecService;
import org.apache.directory.api.ldap.codec.standalone.CodecFactoryUtil;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.service.Command;
import org.structr.api.service.ServiceDependency;
import org.structr.api.service.SingletonService;
import org.structr.api.service.StructrServices;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Group;
import org.structr.core.property.PropertyMap;
import org.structr.schema.SchemaService;

/**
 * The LDAP synchronization service. This is a system service that requires
 * superuser permissions.
 */
@ServiceDependency(SchemaService.class)
public class LDAPService extends Thread implements SingletonService {

	private static final Logger logger               = LoggerFactory.getLogger(LDAPService.class.getName());
	private static final Set<String> structuralTypes = new LinkedHashSet<>(Arrays.asList("ou", "dc"));

	private final long connectionTimeout = 1000;

	public LDAPService() {

		super("Structr LDAP Service");

		((ch.qos.logback.classic.Logger)LoggerFactory.getLogger(DefaultLdapCodecService.class)).setLevel(Level.WARN);
		((ch.qos.logback.classic.Logger)LoggerFactory.getLogger(CodecFactoryUtil.class)).setLevel(Level.WARN);
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

		if (connection != null) {

			connection.setTimeOut(connectionTimeout);

			if (connection.connect()) {

				if (StringUtils.isNotBlank(bindDn) && StringUtils.isNotBlank(secret)) {

					connection.bind(bindDn, secret);

				} else if (StringUtils.isNotBlank(bindDn)) {

					connection.bind(bindDn);
				}

				// decide which method to use for synchronization
				if (useDistinguishedName) {

					logger.info("Updating LDAPGroup {} ({}) with DN {} on LDAP server {}:{}..", groupName, group.getUuid(), groupDn, host, port);

					// use filter + scope
					updateWithGroupDn(group, connection, groupDn, scope);

				} else if (useFilterAndScope) {

					// use dn
					logger.info("Updating LDAPGroup {} ({}) with path {}, filter {} and scope {} on LDAP server {}:{}..", groupName, group.getUuid(), groupPath, groupFilter, groupScope, host, port);

					updateWithFilterAndScope(group, connection, groupPath, groupFilter, groupScope);
				}
			}
		}
	}

	public boolean canSuccessfullyBind(final String dn, final String secret) {

		final String host               = getHost();
		final int port                  = getPort();
		final boolean useSsl            = getUseSSL();
		final LdapConnection connection = new LdapNetworkConnection(host, port, useSsl);
		if (connection != null) {

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

	// ----- private methods -----
	private LDAPUser getOrCreateUser(final Entry userEntry) throws FrameworkException {

		final App app                = StructrApp.getInstance();
		final PropertyMap attributes = new PropertyMap();
		final Dn dn                  = userEntry.getDn();
		Dn parent                    = dn;

		// remove all non-structural rdns (cn / sn / uid etc.)
		while (!structuralTypes.contains(parent.getRdn().getNormType())) {

			parent = parent.getParent();
		}

		attributes.put(StructrApp.key(LDAPUser.class, "distinguishedName"), dn.getNormName());

		LDAPUser user = app.nodeQuery(LDAPUser.class).and(attributes).getFirst();
		if (user == null) {

			user = app.create(LDAPUser.class, attributes);
			user.initializeFrom(userEntry);
		}

		return user;
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

					members.add(getOrCreateUser(cursor.get()));
				}
			}
		}

		logger.info("{} users updated", members.size());

		// update members of group to new state (will remove all members that are not part of the group, as expected)
		group.setProperty(StructrApp.key(Group.class, "members"), members);
	}

	private void updateWithFilterAndScope(final LDAPGroup group, final LdapConnection connection, final String path, final String groupFilter, final String groupScope) throws IOException, LdapException, CursorException, FrameworkException {

		final Map<String, String> possibleGroupNames = getGroupMapping();
		final List<LDAPUser> members                 = new LinkedList<>();
		final Set<String> memberDNs                  = new LinkedHashSet<>();

		// fetch DNs of all group members
		try (final EntryCursor cursor = connection.search(path, groupFilter, SearchScope.valueOf(groupScope))) {

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

			try (final EntryCursor cursor = connection.search(memberDN, "(objectclass=*)", SearchScope.OBJECT)) {

				while (cursor.next()) {

					members.add(getOrCreateUser(cursor.get()));
				}
			}
		}

		logger.info("{} users updated", members.size());

		// update members of group to new state (will remove all members that are not part of the group, as expected)
		group.setProperty(StructrApp.key(Group.class, "members"), members);
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
	public boolean initialize(final StructrServices services) throws ClassNotFoundException, InstantiationException, IllegalAccessException {

		logger.info("host:    {}", Settings.LDAPHost.getValue("localhost"));
		logger.info("port:    {}", Settings.LDAPPort.getValue(389));
		logger.info("use SSL: {}", Settings.LDAPUseSSL.getValue(false));
		logger.info("bind DN: {}", Settings.LDAPBindDN.getValue(""));
		logger.info("scope:   {}", Settings.LDAPScope.getValue("SUBTREE"));
		logger.info("mapping: {}", Settings.LDAPPropertyMapping.getValue("{ sn: name, email: eMail }"));
		logger.info("groups:  {}", Settings.LDAPGroupNames.getValue("{ group: member, groupOfNames: member, groupOfUniqueNames: uniqueMember }"));

		return true;
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
}

