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

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import org.apache.commons.lang.StringUtils;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.schema.loader.SchemaEntityFactory;
import org.apache.directory.api.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InstanceLayout;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.HasEntryOperationContext;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.schema.SchemaPartition;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.service.Command;
import org.structr.api.service.SingletonService;
import org.structr.api.service.StructrServices;

public class LDAPServerService implements SingletonService {

	public static final String LDAP_PARTITION_ROOT_TYPE_PREFIX = "ldap.partition.";
	public static final String LDAP_PARTITION_ROOT_TYPE_SUFFIX = ".rootType";

	private static final Logger logger = LoggerFactory.getLogger(LDAPServerService.class.getName());

	private DirectoryService ds = null;
	private LdapServer server   = null;

	@Override
	public boolean isRunning() {
		return server.isStarted();
	}

	@Override
	public void injectArguments(Command command) {
	}

	@Override
	public boolean initialize(final StructrServices services) throws ClassNotFoundException, InstantiationException, IllegalAccessException {

		logger.info("Initializing directory service");

		try {

			ds = new DefaultDirectoryService();

			final SchemaManager schemaManager = new DefaultSchemaManager();
			final SchemaPartition schemaPartition = new SchemaPartition(schemaManager);

			final StructrPartition structrSchemaPartition = new StructrPartition(schemaManager, "schema", new Dn("ou=system"));
			schemaPartition.setWrappedPartition(structrSchemaPartition);

			ds.setInstanceLayout(new InstanceLayout(new File("/tmp/ldap-test")));
			ds.setSchemaPartition(schemaPartition);
			ds.setSchemaManager(schemaManager);
			ds.setSystemPartition(new StructrPartition(schemaManager, "system", new Dn("ou=system")));
			ds.startup();

			logger.info("Importing schema..");

			initSchema(schemaManager, ds.getAdminSession(), structrSchemaPartition);

			server = new LdapServer();
			int serverPort = 10389;
			server.setTransports(new TcpTransport(serverPort));
			server.setDirectoryService(ds);

			server.start();

		} catch (Throwable t) {

			t.printStackTrace();

			logger.warn("Unable to start LDAP server: {}", t.getMessage());

			return false;
		}

		return true;
	}

	@Override
	public void shutdown() {
		server.stop();
	}

	@Override
	public void initialized() {

		try {

			// TEST
			final DirectoryService service    = server.getDirectoryService();
			final SchemaManager schemaManager = service.getSchemaManager();
			final Dn structrDn                = new Dn(schemaManager, "dc=org");
			final Partition apachePartition   = new StructrPartition(schemaManager, "structr", structrDn);

			apachePartition.initialize();
			service.addPartition(apachePartition);

			try {
				service.getAdminSession().lookup(structrDn);

			} catch (LdapException lnnfe) {

				Entry structrEntry = service.newEntry(structrDn);

				structrEntry.add("objectClass", "top", "domain", "extensibleObject");
				structrEntry.add("dc", "structr");
				service.getAdminSession().add(structrEntry);
			}

			try {
				System.out.println("######: " + service.getAdminSession().lookup(structrDn));
				System.out.flush();

			} catch (LdapException lnnfe) {
				logger.warn("", lnnfe);
			}

		} catch (Throwable t) {

			logger.warn("", t);
		}
	}

	@Override
	public String getName() {
		return "LDAP Server";
	}

	@Override
	public boolean isVital() {
		return false;
	}

	// ----- interface Feature -----
	@Override
	public String getModuleName() {
		return "ldap-server";
	}

	// ----- private methods -----
	private void initSchema(final SchemaManager schemaManager, final CoreSession adminSession, final StructrPartition partition) throws Exception {

		final URL url            = SchemaEntityFactory.class.getProtectionDomain().getCodeSource().getLocation();
		final JarFile jarFile    = new JarFile(new File(url.toURI()));
		final List<String> names = new LinkedList<>();

		for (final Enumeration<JarEntry> e = jarFile.entries(); e.hasMoreElements();) {

			final ZipEntry zipEntry = e.nextElement();
			final String name = zipEntry.getName();

			if (name.startsWith("schema/") && name.endsWith(".ldif")) {

				names.add(name);
			}
		}

		Collections.sort(names, new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {

				// first sort key: number of slashes (level)
				Integer s1 = StringUtils.countMatches(o1, "/");
				Integer s2 = StringUtils.countMatches(o2, "/");

				if (s1.equals(s2)) {

					// secondary sort key: string length
					Integer l1 = o1.length();
					Integer l2 = o2.length();

					if (l1.equals(l2)) {

						// tertiary sort key: the string values
						return o1.compareTo(o2);
					}

					return l1.compareTo(l2);
				}

				return s1.compareTo(s2);
			}

		});

		for (final String name : names) {

			try (final LdifReader reader = new LdifReader(jarFile.getInputStream(jarFile.getEntry(name)))) {

				for (final LdifEntry entry : reader) {

					final Entry schemaEntry  = new DefaultEntry(schemaManager, entry.getEntry());
					final Dn dn              = schemaEntry.getDn();

					if (!partition.hasEntry(new HasEntryOperationContext(adminSession, dn))) {

						logger.info("Importing {}...", name);
						partition.add(new AddOperationContext(adminSession, schemaEntry));
					}
				}
			}
		}
	}
}
