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

import java.net.URI;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Relation.Cardinality;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.SchemaService;
import org.structr.schema.json.JsonObjectType;
import org.structr.schema.json.JsonSchema;


public interface LDAPAttribute extends NodeInterface {

	static class Impl { static {

		final JsonSchema schema    = SchemaService.getDynamicSchema();
		final JsonObjectType type  = schema.addType("LDAPAttribute");
		final JsonObjectType value = schema.addType("LDAPValue");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/LDAPAttribute"));

		type.addStringProperty("oid", PropertyView.Public);

		type.addPropertyGetter("values", Iterable.class);

		type.overrideMethod("getUserProvidedId", false, "return getProperty(name);");
		type.overrideMethod("getOid",            false, "return " + LDAPAttribute.class.getName() + ".getOid(this);");
		type.overrideMethod("addValue",          false, "return " + LDAPAttribute.class.getName() + ".addValue(this, arg0);");

		type.relate(value, "LDAP_VALUE", Cardinality.OneToMany, "parent", "values");

	}}

	String getOid();
	String getUserProvidedId();

	Iterable<LDAPValue> getValues();
	LDAPValue addValue(final String value) throws FrameworkException;

	static String getOid(final LDAPAttribute thisAttribute) {

		final String oid = thisAttribute.getProperty(StructrApp.key(LDAPAttribute.class, "oid"));
		if (oid == null) {

			return thisAttribute.getProperty(AbstractNode.name);
		}

		return oid;
	}

	static LDAPValue addValue(final LDAPAttribute thisAttribute, final String value) throws FrameworkException {

		final Class type = StructrApp.getConfiguration().getNodeEntityClass("LDAPValue");

		return StructrApp.getInstance(thisAttribute.getSecurityContext()).create(type,
			new NodeAttribute<>(StructrApp.key(LDAPValue.class, "parent"),                      thisAttribute),
			new NodeAttribute<>(StructrApp.key(LDAPValue.class, "value"),                       value),
			new NodeAttribute<>(StructrApp.key(LDAPValue.class, "visibleToPublicUsers"),        true),
			new NodeAttribute<>(StructrApp.key(LDAPValue.class, "visibleToAuthenticatedUsers"), true)
		);
	}
}
