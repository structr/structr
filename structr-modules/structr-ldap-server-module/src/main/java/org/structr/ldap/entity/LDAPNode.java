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
import java.util.List;
import java.util.Set;
import org.apache.directory.api.ldap.model.entry.Value;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Relation.Cardinality;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.SchemaService;
import org.structr.schema.json.JsonObjectType;
import org.structr.schema.json.JsonSchema;
import org.structr.schema.json.JsonSchema.Cascade;


public interface LDAPNode extends NodeInterface {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("LDAPNode");
		final JsonObjectType attr = schema.addType("LDAPAttribute");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/LDAPNode"));

		type.addBooleanProperty("isRoot", PropertyView.Public).setIndexed(true);
		type.addStringProperty("rdn",     PropertyView.Public).setIndexed(true);

		type.addPropertyGetter("rdn",        String.class);
		type.addPropertyGetter("parent",     LDAPNode.class);
		type.addPropertyGetter("children",   Iterable.class);
		type.addPropertyGetter("attributes", Iterable.class);

		type.relate(type, "LDAP_CHILD", Cardinality.OneToMany, "parent", "children").setCascadingDelete(Cascade.sourceToTarget);
		type.relate(attr, "LDAP_ATTR",  Cardinality.OneToMany, "parent", "attributes").setCascadingDelete(Cascade.sourceToTarget);

		type.overrideMethod("getChild",            false, "return " + LDAPNode.class.getName() + ".getChild(this, arg0);");
		type.overrideMethod("createChild",         false, "return " + LDAPNode.class.getName() + ".createChild(this, arg0, arg1, arg2, arg3);");
		type.overrideMethod("getUserProvidedName", false, "return " + LDAPNode.class.getName() + ".getUserProvidedName(this);");
		type.overrideMethod("createAttribute",     false, "return " + LDAPNode.class.getName() + ".createAttribute(this, arg0, arg1, arg2);");
		type.overrideMethod("delete",              false, LDAPNode.class.getName() + ".delete(this);");

	}}

	String getUserProvidedName();
	String getRdn();

	LDAPNode getChild(final String normalizedName) throws FrameworkException;
	LDAPNode createChild(final String normalizedName, final String userProvidedName, final String structuralObjectClass, final Set<String> objectClasses) throws FrameworkException;

	Iterable<LDAPNode> getChildren();
	LDAPNode getParent();

	Iterable<LDAPAttribute> getAttributes();
	LDAPAttribute createAttribute(final String oid, final String userProvidedId, final Iterable<Value<?>> values) throws FrameworkException;

	void delete() throws FrameworkException;

	/*


	public static final Property<List<LDAPAttribute>> attributes = new EndNodes<>("attributes", LDAPAttributes.class);
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
	*/

	static LDAPNode getChild(final LDAPNode thisNode, final String normalizedName) throws FrameworkException {

		// find child
		for (final LDAPNode child : thisNode.getChildren()) {

			final String childRdn = child.getRdn();
			if (normalizedName.equals(childRdn)) {

				return child;
			}
		}

		return null;
	}

	static LDAPNode createChild(final LDAPNode thisNode, final String normalizedName, final String userProvidedName, final String structuralObjectClass, final Set<String> objectClasses) throws FrameworkException {

		final Class type = StructrApp.getConfiguration().getNodeEntityClass("LDAPNode");

		return StructrApp.getInstance(thisNode.getSecurityContext()).create(type,
			new NodeAttribute<>(StructrApp.key(LDAPNode.class, "parent"),                      thisNode),
			new NodeAttribute<>(StructrApp.key(LDAPNode.class, "name"),                        userProvidedName),
			new NodeAttribute<>(StructrApp.key(LDAPNode.class, "rdn"),                         normalizedName),
			new NodeAttribute<>(StructrApp.key(LDAPNode.class, "visibleToPublicUsers"),        true),
			new NodeAttribute<>(StructrApp.key(LDAPNode.class, "visibleToAuthenticatedUsers"), true)
		);
	}

	static String getUserProvidedName(final LDAPNode thisNode) {
		return thisNode.getProperty(AbstractNode.name);
	}

	static List<LDAPNode> getChildren(final LDAPNode thisNode) {
		return thisNode.getProperty(StructrApp.key(LDAPNode.class, "children"));
	}

	static LDAPAttribute createAttribute(final LDAPNode thisNode, final String oid, final String userProvidedId, final Iterable<Value<?>> values) throws FrameworkException {

		final App app = StructrApp.getInstance(thisNode.getSecurityContext());

		final LDAPAttribute newAttribute = app.create(LDAPAttribute.class,
			new NodeAttribute<>(StructrApp.key(LDAPAttribute.class, "parent"),                      thisNode),
			new NodeAttribute<>(StructrApp.key(LDAPAttribute.class, "name"),                        userProvidedId),
			new NodeAttribute<>(StructrApp.key(LDAPAttribute.class, "oid"),                         oid),
			new NodeAttribute<>(StructrApp.key(LDAPAttribute.class, "visibleToPublicUsers"),        true),
			new NodeAttribute<>(StructrApp.key(LDAPAttribute.class, "visibleToAuthenticatedUsers"), true)
		);

		for (final Value<?> value : values) {
			newAttribute.addValue(value.getString());
		}

		return newAttribute;
	}

	static List<LDAPAttribute> getAttributes(final LDAPNode thisNode) {
		return thisNode.getProperty(StructrApp.key(LDAPNode.class, "attributes"));
	}

	static void delete(final LDAPNode thisNode) throws FrameworkException {
		StructrApp.getInstance(thisNode.getSecurityContext()).delete(thisNode);
	}
}
