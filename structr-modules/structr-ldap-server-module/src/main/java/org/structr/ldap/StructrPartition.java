/**
 * Copyright (C) 2010-2016 Structr GmbH
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
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.directory.server.core.LdapPrincipal;
import org.apache.directory.server.core.filtering.BaseEntryFilteringCursor;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.EntryOperationContext;
import org.apache.directory.server.core.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.interceptor.context.UnbindOperationContext;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.cursor.ListCursor;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.model.filter.ExprNode;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.structr.common.SecurityContext;
import org.structr.core.app.StructrApp;
import org.structr.ldap.api.LDAPNode;
import org.structr.ldap.entity.LDAPNodeImpl;

/**
 *
 */
class StructrPartition implements Partition {

	private static final Logger logger = Logger.getLogger(StructrPartition.class.getName());

	private Class<? extends LDAPNode> rootType = null;
	private SchemaManager schemaManager        = null;
	private boolean initialized                = false;
	private String id                          = null;
	private Dn suffixDn                        = null;

	public StructrPartition(final SchemaManager schemaManager, final String partitionId, final Dn suffixDn) {

		this.id            = partitionId;
		this.suffixDn      = suffixDn;
		this.schemaManager = schemaManager;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public SchemaManager getSchemaManager() {
		return schemaManager;
	}

	@Override
	public void setSchemaManager(SchemaManager schemaManager) {
		this.schemaManager = schemaManager;
	}

	@Override
	public void initialize() throws LdapException {
		this.initialized = true;
	}

	@Override
	public Dn getSuffixDn() {
		return suffixDn;
	}

	@Override
	public void setSuffixDn(Dn suffixDn) throws LdapInvalidDnException {
		this.suffixDn = suffixDn;
	}

	@Override
	public void destroy() throws Exception {
	}

	@Override
	public boolean isInitialized() {
		return initialized;
	}

	@Override
	public void sync() throws Exception {
	}

	@Override
	public void delete(DeleteOperationContext deleteContext) throws LdapException {

		final LdapPrincipal principal = deleteContext.getEffectivePrincipal();
		final Dn dn                   = deleteContext.getDn();

		getWrapper(principal).delete(dn);
	}

	@Override
	public void add(AddOperationContext addContext) throws LdapException {

		final LdapPrincipal principal = addContext.getEffectivePrincipal();
		final Entry entry             = addContext.getEntry();

		getWrapper(principal).add(entry);
	}

	@Override
	public void modify(ModifyOperationContext modifyContext) throws LdapException {
	}

	@Override
	public EntryFilteringCursor list(ListOperationContext listContext) throws LdapException {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public EntryFilteringCursor search(SearchOperationContext searchContext) throws LdapException {

		Logger.getLogger(StructrPartition.class.getName()).log(Level.INFO, "{0}", searchContext);

		final LdapPrincipal principal = searchContext.getEffectivePrincipal();
		final Dn dn                   = searchContext.getDn();
		final ExprNode filter         = searchContext.getFilter();
		final SearchScope scope       = searchContext.getScope();
		final List<Entry> list        = getWrapper(principal).filter(dn, filter, scope);
		final Cursor<Entry> cursor    = new ListCursor<>(list);

		return new BaseEntryFilteringCursor(cursor, searchContext);
	}

	@Override
	public Entry lookup(LookupOperationContext lookupContext) throws LdapException {

		final Dn dn = lookupContext.getDn();

		if (lookupContext.getSession() != null) {

			return getWrapper(lookupContext.getEffectivePrincipal()).get(dn);

		} else {

			return getWrapper(null).get(dn);
		}
	}

	@Override
	public boolean hasEntry(EntryOperationContext hasEntryContext) throws LdapException {

		final LdapPrincipal principal = hasEntryContext.getEffectivePrincipal();
		final Dn dn                   = hasEntryContext.getDn();

		return getWrapper(principal).get(dn) != null;
	}

	@Override
	public void rename(RenameOperationContext renameContext) throws LdapException {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void move(MoveOperationContext moveContext) throws LdapException {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void moveAndRename(MoveAndRenameOperationContext moveAndRenameContext) throws LdapException {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void bind(BindOperationContext bindContext) throws LdapException {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void unbind(UnbindOperationContext unbindContext) throws LdapException {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void dumpIndex(OutputStream stream, String name) throws IOException {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	// ----- private methods -----
	private StructrLDAPWrapper getWrapper(final LdapPrincipal principal) {

		final SecurityContext securityContext = SecurityContext.getSuperUserInstance();

		return new StructrLDAPWrapper(securityContext, schemaManager, id, getPartitionRootType());
	}

	private Class<? extends LDAPNode> getPartitionRootType() {

		if (rootType == null) {

			// try to identify root type from structr.conf
			final StringBuilder keyBuilder = new StringBuilder(LDAPServerService.LDAP_PARTITION_ROOT_TYPE_PREFIX);
			keyBuilder.append(this.id);
			keyBuilder.append(LDAPServerService.LDAP_PARTITION_ROOT_TYPE_SUFFIX);

			final String rootTypeName = StructrApp.getConfigurationValue(keyBuilder.toString());
			if (rootTypeName == null) {

				logger.log(Level.INFO, "No LDAP root node type specified for partition {0}, using default (LDAPNodeImpl). This default can be changed by setting a value for {1} in structr.conf",
					new Object[] {
						id,
						keyBuilder.toString()
					}
				);

				rootType = LDAPNodeImpl.class;

			} else {

				try {

					this.rootType = (Class<? extends LDAPNode>)Class.forName(rootTypeName);

				} catch (ClassNotFoundException ex) {
					logger.log(Level.WARNING, "Unable to instantiate LDAP root node class {0}, falling back to default. {1}", new Object[] { rootTypeName, ex.getMessage() });
				}

				if (rootType == null) {
					rootType = LDAPNodeImpl.class;
				}
			}
		}

		return rootType;
	}
}
