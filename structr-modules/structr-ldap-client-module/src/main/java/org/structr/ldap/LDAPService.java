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
package org.structr.ldap;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.service.Command;
import org.structr.api.service.RunnableService;
import org.structr.api.service.StructrServices;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Group;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;

/**
 * The LDAP synchronization service. This is a system service that requires
 * superuser permissions.
 */
public class LDAPService extends Thread implements RunnableService {

	private static final Logger logger               = LoggerFactory.getLogger(LDAPService.class.getName());
	private static final Set<String> structuralTypes = new LinkedHashSet<>(Arrays.asList("ou", "dc"));

	public static final String CONFIG_KEY_UPDATE_INTERVAL = "ldap.updateInterval";
	public static final String CONFIG_KEY_LDAP_BINDDN     = "ldap.bindDn";
	public static final String CONFIG_KEY_LDAP_SECRET     = "ldap.secret";
	public static final String CONFIG_KEY_LDAP_HOST       = "ldap.host";
	public static final String CONFIG_KEY_LDAP_PORT       = "ldap.port";
	public static final String CONFIG_KEY_LDAP_SSL        = "ldap.useSsl";
	public static final String CONFIG_KEY_LDAP_BASEDN     = "ldap.baseDn";
	public static final String CONFIG_KEY_LDAP_FILTER     = "ldap.filter";
	public static final String CONFIG_KEY_LDAP_SCOPE      = "ldap.scope";

	private long updateInterval = 1800;		// completely arbitrary update interval, set your own in structr.conf!
	private String host         = "localhost";
	private String binddn       = null;
	private String secret       = null;
	private String baseDn       = null;
	private String filter       = null;
	private String scope        = null;
	private boolean useSsl      = true;
	private boolean doRun       = true;
	private int port            = 389;

	public LDAPService() {

		super("Structr LDAP Service");
		this.setDaemon(true);
	}

	// ----- public methods -----
	public String fetchObjectInfo(final String dn) {

		final LdapConnection connection = new LdapNetworkConnection(host, port, useSsl);
		final StringBuilder buf         = new StringBuilder();

		if (connection != null) {

			try {
				if (connection.connect()) {

					if (StringUtils.isNotBlank(binddn) && StringUtils.isNotBlank(secret)) {

						connection.bind(binddn, secret);

					} else if (StringUtils.isNotBlank(binddn)) {

						connection.bind(binddn);
					}

					final EntryCursor cursor = connection.search(dn, "(objectclass=*)", SearchScope.OBJECT);
					while (cursor.next()) {

						buf.append(cursor.get());
						buf.append("\n");
					}

					cursor.close();

					connection.close();
				}

				connection.close();

			} catch (CursorException | LdapException | IOException ex) {
				logger.warn("", ex);
			}
		}

		return buf.toString();
	}

	public boolean canSuccessfullyBind(final String dn, final String secret) {

		final LdapConnection connection = new LdapNetworkConnection(host, port, useSsl);
		if (connection != null) {

			try {
				if (connection.connect()) {

					connection.bind(dn, secret);
					connection.unBind();
				}

				connection.close();

				return true;

			} catch (LdapException | IOException ex) {
				logger.warn("", ex);
			}
		}

		return false;
	}

	public void doUpdate() throws IOException, LdapException, CursorException, FrameworkException {

		final LdapConnection connection                 = new LdapNetworkConnection(host, port, useSsl);
		final Map<Group, Set<String>> unresolvedMembers = new LinkedHashMap<>();
		final Map<String, Object> metadata              = new LinkedHashMap<>();
		final App app                                   = StructrApp.getInstance();

		metadata.put("users",        0);
		metadata.put("groups",       0);
		metadata.put("deletedUsers", 0);

		if (connection != null) {

			// make connection persistent
			connection.setTimeOut(0);

			if (connection.connect()) {

				logger.info("Updating user/group information from LDAP server {}:{}..", host, port);

				if (StringUtils.isNotBlank(binddn) && StringUtils.isNotBlank(secret)) {

					connection.bind(binddn, secret);

				} else if (StringUtils.isNotBlank(binddn)) {

					connection.bind(binddn);
				}

				// step 1: fetch / update all users from LDAP server
				final EntryCursor cursor = connection.search(baseDn, filter, SearchScope.valueOf(scope));
				while (cursor.next()) {

					try (final Tx tx = app.tx()) {

						synchronizeEntry(cursor.get(), unresolvedMembers, metadata);
						tx.success();

					} catch (FrameworkException fex) {
						fex.printStackTrace();
					}
				}

				// step 2: resolve group members and create connections
				try (final Tx tx = app.tx()) {

					for (final Map.Entry<Group, Set<String>> entry : unresolvedMembers.entrySet()) {

						final Group group         = entry.getKey();
						final Set<String> members = entry.getValue();

						for (final String member : members) {

							final LDAPUser resolvedUser = resolveUser(new Dn(member.split(",")));
							if (resolvedUser != null) {

								group.addMember(resolvedUser);
							}
						}
					}

					tx.success();

				} catch (FrameworkException fex) {
					fex.printStackTrace();
				}

				// step 3: examine local users and refresh / remove
				try (final Tx tx = app.tx()) {

					for (final LDAPUser user : app.nodeQuery(LDAPUser.class).getAsList()) {

						final String dn = user.getDistinguishedName();
						if (dn != null) {

							final Entry userEntry = connection.lookup(dn);
							if (userEntry != null) {

								// update user information
								user.initializeFrom(userEntry);

							} else {

								logger.info("User {} doesn't exist in LDAP directory, deleting.", user);
								app.delete(user);

								count(metadata, "deletedUser");
							}

						} else {

							logger.warn("User {} doesn't have an LDAP distinguished name, ignoring.", user);
						}
					}

					tx.success();

				} catch (FrameworkException fex) {
					fex.printStackTrace();
				}

				cursor.close();
				connection.close();

				logger.info("Synchronized {} users and {} groups, deleted {} users", metadata.get("users"), metadata.get("groups"), metadata.get("deletedUsers"));

			} else {

				logger.info("Connection to LDAP server {} failed", host);
			}
		}
	}

	// ----- private methods -----
	private void synchronizeEntry(final Entry entry, final Map<Group, Set<String>> unresolvedMembers, final Map<String, Object> metadata) throws FrameworkException {

		// entry can be either user or group and must be handled accordingly
		final Attribute objectClass = entry.get("objectclass");
		if (objectClass.contains("groupOfUniqueNames")) {

			synchronizeGroup(entry, unresolvedMembers);
			count(metadata, "groups");

		} else if (objectClass.contains("organizationalPerson")) {

			synchronizeUser(entry);
			count(metadata, "users");
		}
	}

	private void synchronizeGroup(final Entry groupEntry, final Map<Group, Set<String>> unresolvedMembers) throws FrameworkException {

		final Set<String> members = new LinkedHashSet<>();
		final Group group         = getOrCreateGroupHierarchy(groupEntry.getDn());

		// add group members
		final Attribute uniqueMembers = groupEntry.get("uniqueMember");
		if (uniqueMembers != null) {

			for (final Value value : uniqueMembers) {

				members.add(value.getString());
			}

			unresolvedMembers.put(group, members);
		}
	}

	private void synchronizeUser(final Entry userEntry) throws FrameworkException {

		final Dn dn                  = userEntry.getDn();
		Dn parent                    = dn;

		// remove all non-structural rdns (cn / sn / uid etc.)
		while (!structuralTypes.contains(parent.getRdn().getNormType())) {

			parent = parent.getParent();
		}

		final Group parentGroup = getOrCreateGroupHierarchy(parent);
		final App app           = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			final PropertyMap attributes = new PropertyMap();

			attributes.put(StructrApp.key(LDAPUser.class, "distinguishedName"), dn.getNormName());
			attributes.put(StructrApp.key(LDAPUser.class, "name"),              dn.getRdn().getNormName());
			attributes.put(StructrApp.key(LDAPUser.class, "groups"),            Arrays.asList(parentGroup));

			LDAPUser user = app.nodeQuery(LDAPUser.class).and(attributes).getFirst();
			if (user == null) {

				user = app.create(LDAPUser.class, attributes);
				user.initializeFrom(userEntry);
			}

			tx.success();

		} catch (FrameworkException | LdapInvalidAttributeValueException fex) {
			logger.warn("Unable to update LDAP information", fex);
		}
	}

	private Group getOrCreateGroupHierarchy(final Dn dn) throws FrameworkException {

		final List<Rdn> rdns = new LinkedList<>(dn.getRdns());
		Group currentGroup   = null;

		Collections.reverse(rdns);

		for (final Rdn rdn : rdns) {

			currentGroup = getOrCreateGroup(currentGroup, rdn.getNormName());
		}

		return currentGroup;
	}

	private Group getOrCreateGroup(final Group parent, final String name) throws FrameworkException {

		final PropertyMap attributes = new PropertyMap();
		final List<Group> parents    = new LinkedList<>();

		if (parent != null) {
			parents.add(parent);
		}

		attributes.put(StructrApp.key(Group.class, "name"),   name);
		attributes.put(StructrApp.key(Group.class, "groups"), parents);

		final Group group = StructrApp.getInstance().nodeQuery(Group.class).and(attributes).getFirst();
		if (group != null) {

			return group;
		}

		// not found => create
		return StructrApp.getInstance().create(Group.class, attributes);
	}

	private LDAPUser resolveUser(final Dn dn) throws FrameworkException {

		final Class<Group> groupType             = StructrApp.getConfiguration().getNodeEntityClass("Group");
		final PropertyKey<List<Group>> groupsKey = StructrApp.getConfiguration().getPropertyKeyForJSONName(groupType, "groups");
		final List<Rdn> rdns                     = new LinkedList<>(dn.getRdns());
		final PropertyMap attrs                  = new PropertyMap();
		Group currentGroup                       = null;

		Collections.reverse(rdns);

		for (final Rdn rdn : rdns) {

			if (structuralTypes.contains(rdn.getNormType())) {

				currentGroup = getOrCreateGroup(currentGroup, rdn.getNormName());

			} else {

				final PropertyKey<String> key = StructrApp.key(LDAPUser.class, rdn.getNormType());
				if (key != null) {

					attrs.put(key, rdn.getValue());

				} else {

					logger.warn("No property key found for Rdn component {}, ignoring", rdn.getNormName());
				}
			}
		}

		// try to find a user with the given group and attribute set
		return StructrApp.getInstance()
			.nodeQuery(LDAPUser.class)
			.and(attrs)
			.and(groupsKey, Arrays.asList(currentGroup), false)
			.getFirst();
	}

	private void count(final Map<String, Object> data, final String key) {

		final Integer value = (Integer)data.get(key);
		if (value != null) {

			data.put(key, value + 1);

		} else {

			data.put(key, 1);
		}
	}

	// ----- class Thread -----
	@Override
	public void run() {

		// wait for service layer to be fully initialized
		while (!Services.getInstance().isInitialized()) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException itex) {
			}
		}

		while (doRun) {

			try {

				doUpdate();

			} catch (Throwable t) {
				t.printStackTrace();
				logger.warn("Unable to update LDAP information: {}", t.getMessage());
			}

			// sleep until next update
			try { Thread.sleep(updateInterval * 1000); } catch (InterruptedException itex) { }
		}
	}

	// ----- interface RunnableService -----
	@Override
	public void startService() throws Exception {

		logger.info("Starting LDAPService, update interval {} s", updateInterval);
		this.start();
	}

	@Override
	public void stopService() {
		doRun = false;
	}

	@Override
	public boolean runOnStartup() {
		return true;
	}

	@Override
	public boolean isRunning() {
		return doRun;
	}

	@Override
	public void injectArguments(final Command command) {
	}

	@Override
	public boolean initialize(final StructrServices services) throws ClassNotFoundException, InstantiationException, IllegalAccessException {

		this.updateInterval = Settings.getOrCreateIntegerSetting(CONFIG_KEY_UPDATE_INTERVAL).getValue(1800);

		this.binddn         = Settings.getOrCreateStringSetting(CONFIG_KEY_LDAP_BINDDN).getValue("");
		this.secret         = Settings.getOrCreateStringSetting(CONFIG_KEY_LDAP_SECRET).getValue("");

		this.host           = Settings.getOrCreateStringSetting(CONFIG_KEY_LDAP_HOST).getValue("localhost");
		this.baseDn         = Settings.getOrCreateStringSetting(CONFIG_KEY_LDAP_BASEDN).getValue("");
		this.filter         = Settings.getOrCreateStringSetting(CONFIG_KEY_LDAP_FILTER).getValue("(objectclass=*)");
		this.scope          = Settings.getOrCreateStringSetting(CONFIG_KEY_LDAP_SCOPE).getValue("SUBTREE");

		this.port           = Settings.getOrCreateIntegerSetting(CONFIG_KEY_LDAP_PORT).getValue(389);
		this.useSsl         = Settings.getOrCreateBooleanSetting(CONFIG_KEY_LDAP_SSL).getValue(false);

		return true;
	}

	@Override
	public void shutdown() {
		doRun = false;
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

