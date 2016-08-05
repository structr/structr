/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.ldap.entity;

import org.structr.ldap.api.LDAPValue;
import org.structr.ldap.api.LDAPAttribute;
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
import org.structr.ldap.relationship.LDAPAttributes;
import org.structr.ldap.relationship.LDAPValues;

/**
 *
 * @author Christian Morgner
 */
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
