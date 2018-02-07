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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.filter.AndNode;
import org.apache.directory.api.ldap.model.filter.AssertionType;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.OrNode;
import org.apache.directory.api.ldap.model.filter.PresenceNode;
import org.apache.directory.api.ldap.model.filter.SimpleNode;
import org.apache.directory.api.ldap.model.filter.SubstringNode;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.registries.ObjectClassRegistry;
import org.apache.directory.server.core.api.entry.ClonedServerEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.ldap.entity.LDAPAttribute;
import org.structr.ldap.entity.LDAPNode;
import org.structr.ldap.entity.LDAPValue;


public class StructrLDAPWrapper {

	private static final Logger logger = LoggerFactory.getLogger(StructrLDAPWrapper.class.getName());
	private SecurityContext securityContext = null;
	private SchemaManager schemaManager     = null;
	private String partitionId              = null;
	private Class<? extends LDAPNode> type  = null;

	public StructrLDAPWrapper(final SecurityContext securityContext, final SchemaManager schemaManager, final String partitionId, final Class<? extends LDAPNode> type) {

		this.securityContext = securityContext;
		this.schemaManager   = schemaManager;
		this.partitionId     = partitionId;
		this.type            = type;
	}

	public void add(final Entry entry) throws LdapException {

		try (final Tx tx = app().tx()) {

			// create while descending
			final Dn dn           = entry.getDn();
			final LDAPNode parent = find(dn.getParent());

			if (parent != null) {

				final Attribute objectClasses = entry.get(schemaManager.getAttributeType(SchemaConstants.OBJECT_CLASS_AT_OID));
				final ObjectClassRegistry reg = schemaManager.getObjectClassRegistry();
				final Set<String> classes     = new LinkedHashSet<>();
				final Rdn rdn                 = dn.getRdn();
				String mainClass              = null;

				// make rdn schema aware
				if (!rdn.isSchemaAware()) {
					rdn.apply(schemaManager);
				}

				if (objectClasses != null) {

					for (final Value<?> value : objectClasses) {

						final String cls            = value.getString();
						final String objectClassOid = reg.getOidByName(cls);

						if (reg.get(objectClassOid).isStructural()) {

							mainClass = cls;

						} else {

							classes.add(cls);
						}
					}

					final LDAPNode newChild = parent.createChild(rdn.getNormName(), rdn.getName(), mainClass, classes);
					if (newChild != null) {

						for (final Attribute attr : entry) {

							AttributeType type = attr.getAttributeType();
							String oid         = null;

							if (type != null) {

								oid = type.getOid();

							} else {

								type = schemaManager.getAttributeType(attr.getUpId());
								oid  = type.getOid();
							}

							newChild.createAttribute(oid, attr.getUpId(), attr);
						}

					} else {

						logger.warn("Unable to add entry {}, could not create new instance", entry);
					}

				} else {


					logger.warn("Unable to add entry {}, could not determine object class(es)", entry);
				}

			} else {

				logger.warn("Unable to add entry {}, parent not found", entry);
			}

			tx.success();

		} catch (FrameworkException fex) {
			handleException(fex);
		}
	}

	public Entry get(final Dn dn) throws LdapException {

		try (final Tx tx = app().tx()) {

			final LDAPNode entry = find(dn);

			tx.success();

			if (entry != null) {
				return getEntry(entry);
			}

		} catch (FrameworkException fex) {
			handleException(fex);
		}
		// not found
		return null;
	}

	public void delete(final Dn dn) throws LdapException {

		final App app = app();
		try (final Tx tx = app.tx()) {

			final LDAPNode entry = find(dn);
			if (entry != null) {

				entry.delete();
			}

			tx.success();

		} catch (FrameworkException fex) {
			handleException(fex);
		}
	}

	public List<Entry> filter(final Dn dn, final ExprNode filter, final SearchScope scope) throws LdapException {

		try (final Tx tx = app().tx()) {

			final LDAPNode entry = find(dn);
			List<Entry> result   = null;

			if (entry != null) {

				result = filter(entry, filter, scope, 0);
			}

			tx.success();

			if (result != null) {
				return result;
			}

		} catch (FrameworkException fex) {
			handleException(fex);
		}

		return Collections.emptyList();
	}

	// ----- private methods -----
	public Rdn getRdn(final LDAPNode node) throws FrameworkException, LdapInvalidDnException {

		String name = node.getUserProvidedName();
		if (name == null) {

			name = node.getRdn();
		}

		return new Rdn(schemaManager, name);
	}

	public Dn getDn(final LDAPNode node) throws FrameworkException, LdapInvalidDnException {

		final LDAPNode _parent = node.getParent();
		if (_parent != null) {

			return new Dn(getRdn(node), getDn(_parent));
		}

		return Dn.EMPTY_DN;
	}

	public Entry getEntry(final LDAPNode node) throws FrameworkException, LdapException {

		final DefaultEntry entry = new DefaultEntry(schemaManager, getDn(node));

		for (final LDAPAttribute attr : node.getAttributes()) {

			entry.add(getAttribute(attr));
		}

		return new ClonedServerEntry(entry);
	}

	public Attribute getAttribute(final LDAPAttribute src) throws LdapInvalidAttributeValueException {

		final AttributeType type  = schemaManager.getAttributeType(src.getOid());
		final Attribute attribute = new DefaultAttribute(type);
		final String name         = src.getUserProvidedId();

		if (name != null) {
			attribute.setUpId(name);
		}

		for (final LDAPValue value : src.getValues()) {

			attribute.add(value.getStringValue());
		}

		return attribute;

	}

	private LDAPNode find(final Dn dn) throws FrameworkException, LdapException, LdapInvalidDnException {

		final List<Rdn> rdns = new LinkedList<>(dn.getRdns());

		Collections.reverse(rdns);

		LDAPNode current = getRoot();

		for (final Rdn rdn : rdns) {

			if (!rdn.isSchemaAware()) {
				rdn.apply(schemaManager);
			}

			current = current.getChild(rdn.getNormName());

			// break early to avoid NPE
			if (current == null) {
				return null;
			}
		}

		return current;
	}

	private List<Entry> filter(final LDAPNode node, final ExprNode filter, final SearchScope scope, final int depth) throws FrameworkException, LdapException {

		final boolean base     = SearchScope.OBJECT.equals(scope);
		final boolean oneLevel = SearchScope.ONELEVEL.equals(scope);
		final boolean subtree  = SearchScope.SUBTREE.equals(scope);
		final List<Entry> list = new LinkedList<>();

		if (base || !(depth == 0 && oneLevel)) {

			if (matches(node, filter)) {
				list.add(getEntry(node));
			}
		}

		if (!base && (subtree || (depth == 0 && oneLevel))) {

			// recurse
			for (final LDAPNode child : node.getChildren()) {
				list.addAll(filter(child, filter, scope, depth + 1));
			}
		}

		return list;
	}

	private boolean matches(final LDAPNode node, final ExprNode filter) throws FrameworkException, LdapInvalidAttributeValueException {

		if (filter instanceof SimpleNode) {

			return evaluateSimpleNode(node, (SimpleNode)filter);

		} else if (filter instanceof SubstringNode) {

			return evaluateSubstringNode(node, (SubstringNode)filter);

		} else if (filter instanceof PresenceNode) {

			final PresenceNode presence = (PresenceNode)filter;
			final Attribute attribute   = new DefaultAttribute(presence.getAttributeType());

			return findAttribute(node, attribute.getId()) != null;

		} else if (filter instanceof OrNode) {

			final OrNode orNode = (OrNode)filter;

			for (final ExprNode child : orNode.getChildren()) {

				if (matches(node, child)) {
					return true;
				}
			}

			return false;

		} else if (filter instanceof AndNode) {

			final AndNode andNode = (AndNode)filter;
			boolean result        = true;

			for (final ExprNode child : andNode.getChildren()) {
				result &= matches(node, child);
			}

			return result;

		} else {

			System.out.println("Unsupported filter type " + filter.getClass());
		}

		return false;

	}

	private boolean evaluateSimpleNode(final LDAPNode node, final SimpleNode simpleNode) throws FrameworkException, LdapInvalidAttributeValueException {

		final AssertionType assertionType = simpleNode.getAssertionType();
		final Attribute attribute         = new DefaultAttribute(simpleNode.getAttributeType(), simpleNode.getValue());

		if (attribute != null) {

			switch (assertionType) {

				case EQUALITY:
					return hasAttributeValue(node, attribute);
			}
		}

		return false;
	}

	private boolean evaluateSubstringNode(final LDAPNode node, final SubstringNode substringNode) throws FrameworkException, LdapInvalidAttributeValueException {

		final Attribute attribute         = new DefaultAttribute(substringNode.getAttributeType());
		final String oid                  = attribute.getId();
		final String initialPart          = substringNode.getInitial();
		final String finalPart            = substringNode.getFinal();
		final List<String> any            = new LinkedList<>();
		final List<String> fromNode       = substringNode.getAny();

		// add fragments from substring node (if present)
		if (fromNode != null) {
			any.addAll(fromNode);
		}

		final Pattern pattern = SubstringNode.getRegex(initialPart, any.toArray(new String[0]), finalPart);

		for (final LDAPAttribute attr : node.getAttributes()) {

			if (oid.equals(attr.getOid())) {

				for (final LDAPValue value : attr.getValues()) {

					final String stringValue = value.getStringValue().toLowerCase();
					final Matcher matcher    = pattern.matcher(stringValue);

					if (matcher.matches()) {

						return true;
					}
				}
			}
		}

		return false;
	}

	private boolean hasAttributeValue(final LDAPNode node, final Attribute value) throws FrameworkException, LdapInvalidAttributeValueException {

		final Attribute attribute = findAttribute(node, value.getId());
		if (attribute != null) {

			return attribute.contains(value.get());
		}

		return false;
	}

	private Attribute findAttribute(final LDAPNode node, final String oid) throws FrameworkException, LdapInvalidAttributeValueException {

		for (final LDAPAttribute attr : node.getAttributes()) {

			if (oid.equals(attr.getOid())) {

				return getAttribute(attr);
			}
		}

		return null;
	}

	private LDAPNode getRoot() throws FrameworkException {

		final Class type = StructrApp.getConfiguration().getNodeEntityClass("LDAPNode");
		final App app    = app();

		LDAPNode root = (LDAPNode)app.nodeQuery(type).andName(partitionId).getFirst();
		if (root == null) {

			root = app.create(type,
				new NodeAttribute<>(StructrApp.key(LDAPNode.class, "name"),   partitionId),
				new NodeAttribute<>(StructrApp.key(LDAPNode.class, "isRoot"), true)
			);
		}

		return root;
	}

	private App app() {
		return StructrApp.getInstance(securityContext);
	}

	private void handleException(final FrameworkException fex) throws LdapException {

		logger.warn("", fex);

	}
}
