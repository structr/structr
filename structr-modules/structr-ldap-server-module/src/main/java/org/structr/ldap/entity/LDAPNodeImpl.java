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
package org.structr.ldap.entity;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.apache.directory.api.ldap.model.entry.Value;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.ldap.api.LDAPAttribute;
import org.structr.ldap.api.LDAPNode;
import org.structr.ldap.relationship.LDAPAttributes;
import org.structr.ldap.relationship.LDAPChildren;


public class LDAPNodeImpl extends AbstractNode implements LDAPNode {

	public static final Property<List<LDAPAttributeImpl>> attributes = new EndNodes<>("attributes", LDAPAttributes.class);
	public static final Property<List<LDAPNodeImpl>> children        = new EndNodes<>("children", LDAPChildren.class);
	public static final Property<LDAPNodeImpl> parent                = new StartNode<>("parent", LDAPChildren.class);
	public static final Property<Boolean> isRoot                     = new BooleanProperty("isRoot").indexed();
	public static final Property<String> rdn                         = new StringProperty("rdn").indexed();

	public static final View publicView = new View(LDAPNodeImpl.class, PropertyView.Public,
		name, rdn, attributes, children
	);

	public static final View uiView = new View(LDAPNodeImpl.class, PropertyView.Ui,
		name, rdn, attributes, children
	);

	@Override
	public LDAPNode getChild(final String normalizedName) throws FrameworkException {

		// find child
		final List<LDAPNodeImpl> children = getProperty(LDAPNodeImpl.children);
		for (final LDAPNode child : children) {

			final String childRdn = child.getRdn();
			if (normalizedName.equals(childRdn)) {

				return child;
			}
		}

		return null;
	}

	@Override
	public LDAPNode createChild(final String normalizedName, final String userProvidedName, final String structuralObjectClass, final Set<String> objectClasses) throws FrameworkException {

		return StructrApp.getInstance(securityContext).create(LDAPNodeImpl.class,
			new NodeAttribute<>(LDAPNodeImpl.parent, this),
			new NodeAttribute<>(LDAPNodeImpl.name, userProvidedName),
			new NodeAttribute<>(LDAPNodeImpl.rdn, normalizedName),
			new NodeAttribute<>(AbstractNode.visibleToPublicUsers, true),
			new NodeAttribute<>(AbstractNode.visibleToAuthenticatedUsers, true)
		);
	}

	@Override
	public String getUserProvidedName() {
		return getProperty(AbstractNode.name);
	}

	@Override
	public String getRdn() {
		return getProperty(LDAPNodeImpl.rdn);
	}

	@Override
	public LDAPNode getParent() {
		return getProperty(LDAPNodeImpl.parent);
	}

	@Override
	public List<LDAPNode> getChildren() {

		final List<LDAPNode> nodes = new LinkedList<>();
		for (final LDAPNode child : getProperty(LDAPNodeImpl.children)) {

			nodes.add(child);
		}

		return nodes;
	}

	@Override
	public LDAPAttribute createAttribute(final String oid, final String userProvidedId, final Iterable<Value<?>> values) throws FrameworkException {

		final App app = StructrApp.getInstance(securityContext);

		final LDAPAttribute newAttribute = app.create(LDAPAttributeImpl.class,
			new NodeAttribute<>(LDAPAttributeImpl.parent, this),
			new NodeAttribute<>(AbstractNode.name, userProvidedId),
			new NodeAttribute<>(LDAPAttributeImpl.oid, oid),
			new NodeAttribute<>(AbstractNode.visibleToPublicUsers, true),
			new NodeAttribute<>(AbstractNode.visibleToAuthenticatedUsers, true)
		);

		for (final Value<?> value : values) {
			newAttribute.addValue(value.getString());
		}

		return newAttribute;
	}

	@Override
	public List<LDAPAttribute> getAttributes() {

		final List<LDAPAttribute> attrs = new LinkedList<>();
		for (final LDAPAttribute attr : getProperty(LDAPNodeImpl.attributes)) {

			attrs.add(attr);
		}

		return attrs;
	}

	@Override
	public void delete() throws FrameworkException {
		StructrApp.getInstance(securityContext).delete(this);
	}
}
