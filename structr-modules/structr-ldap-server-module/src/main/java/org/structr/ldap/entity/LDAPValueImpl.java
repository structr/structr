/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.ldap.entity;

import org.structr.ldap.api.LDAPValue;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.ldap.relationship.LDAPValues;

/**
 *
 * @author Christian Morgner
 */
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
