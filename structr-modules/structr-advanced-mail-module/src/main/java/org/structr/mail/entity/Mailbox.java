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
package org.structr.mail.entity;

import org.structr.common.PropertyView;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.SchemaService;
import org.structr.schema.json.JsonObjectType;
import org.structr.schema.json.JsonSchema;

import java.net.URI;

public interface Mailbox extends NodeInterface {
	class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("Mailbox");
		final JsonObjectType mail = schema.addType("Mail");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/Mailbox"));

		type.addStringProperty("host",                     PropertyView.Public, PropertyView.Ui).setIndexed(true);
		type.addStringProperty("user",                     PropertyView.Public, PropertyView.Ui).setIndexed(true);
		type.addStringProperty("password",                 PropertyView.Public, PropertyView.Ui).setIndexed(true);
		type.addStringProperty("mailProtocol",             PropertyView.Public, PropertyView.Ui).setIndexed(true);

		type.addPropertyGetter("host",              String.class);
		type.addPropertyGetter("user",              String.class);
		type.addPropertyGetter("password",          String.class);
		type.addPropertyGetter("mailProtocol",      String.class);

		type.relate(mail, "CONTAINS_MAILS", Relation.Cardinality.OneToMany, "mails", "mailbox");

		// view configuration
		type.addViewProperty(PropertyView.Public, "host,user,password,mailProtocol,mails");
		type.addViewProperty(PropertyView.Ui, "host,user,password,mailProtocol,mails");
	}}

	String getHost();
	String getUser();
	String getPassword();
	String getMailProtocol();

}
