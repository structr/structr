/**
 * Copyright (C) 2010-2017 Structr GmbH
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
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.ldap.api.LDAPAttribute;
import org.structr.ldap.api.LDAPValue;
import org.structr.ldap.relationship.LDAPAttributes;
import org.structr.ldap.relationship.LDAPValues;


public class LDAPAttributeImpl extends AbstractNode implements LDAPAttribute {

	public static final Property<List<LDAPValueImpl>> values = new EndNodes<>("values", LDAPValues.class);
	public static final Property<LDAPNodeImpl> parent        = new StartNode<>("parent", LDAPAttributes.class);
	public static final Property<String> oid                 = new StringProperty("oid").indexed();

	public static final View publicView = new View(LDAPAttributeImpl.class, PropertyView.Public,
		name, oid, values
	);

	public static final View uiView = new View(LDAPAttributeImpl.class, PropertyView.Ui,
		name, oid, values
	);

	@Override
	public String getUserProvidedId() {
		return getProperty(AbstractNode.name);
	}

	@Override
	public String getOid() {

		final String oid = getProperty(LDAPAttributeImpl.oid);
		if (oid == null) {

			return getProperty(AbstractNode.name);
		}

		return oid;
	}


	@Override
	public List<LDAPValue> getValues() {

		final List<LDAPValue> values = new LinkedList<>();

		for (final LDAPValueImpl impl : getProperty(LDAPAttributeImpl.values)) {
			values.add(impl);
		}

		return values;
	}
	@Override
	public LDAPValue addValue(String value) throws FrameworkException {

		return StructrApp.getInstance(securityContext).create(LDAPValueImpl.class,
			new NodeAttribute<>(LDAPValueImpl.parent, this),
			new NodeAttribute<>(LDAPValueImpl.value, value),
			new NodeAttribute<>(AbstractNode.visibleToPublicUsers, true),
			new NodeAttribute<>(AbstractNode.visibleToAuthenticatedUsers, true)
		);
	}
}
