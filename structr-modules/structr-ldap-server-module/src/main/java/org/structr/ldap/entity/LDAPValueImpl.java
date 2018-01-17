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

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.ldap.api.LDAPValue;
import org.structr.ldap.relationship.LDAPValues;


public class LDAPValueImpl extends AbstractNode implements LDAPValue {

	public static final Property<LDAPAttributeImpl> parent = new StartNode<>("parent", LDAPValues.class);
	public static final Property<String> value             = new StringProperty("value");

	public static final View publicView = new View(LDAPValueImpl.class, PropertyView.Public,
		value
	);

	public static final View uiView = new View(LDAPValueImpl.class, PropertyView.Ui,
		value
	);

	@Override
	public String getStringValue() {
		return getProperty(LDAPValueImpl.value);
	}

	@Override
	public void setStringValue(final String value) throws FrameworkException {
		setProperty(LDAPValueImpl.value, value);
	}
}
