/*
 * Copyright (C) 2010-2024 Structr GmbH
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

import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.SchemaService;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public interface Mailbox extends NodeInterface {

	class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("Mailbox");
		final JsonObjectType mail = schema.addType("EMailMessage");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/Mailbox"));

		type.addStringProperty("host",                           PropertyView.Ui).setIndexed(true).setRequired(true);
		type.addStringProperty("user",                           PropertyView.Ui).setIndexed(true).setRequired(true);
		type.addStringProperty("overrideMailEntityType",         PropertyView.Ui).setIndexed(true);
		type.addEncryptedProperty("password",                    PropertyView.Ui).setIndexed(true).setRequired(false);
		type.addStringArrayProperty("folders",                   PropertyView.Ui).setIndexed(true);
		type.addEnumProperty("mailProtocol",                     PropertyView.Ui).setEnums("pop3,imaps").setIndexed(true).setRequired(true);
		type.addIntegerProperty("port",                          PropertyView.Ui).setIndexed(true);
		type.addFunctionProperty("availableFoldersOnServer",     PropertyView.Ui).setReadFunction("{return Structr.this.getAvailableFoldersOnServer()}").setIndexed(false);

		type.addPropertyGetter("host",                     String.class);
		type.addPropertyGetter("user",                     String.class);
		type.addPropertyGetter("password",                 String.class);
		type.addPropertyGetter("overrideMailEntityType",   String.class);
		type.addPropertyGetter("mailProtocol",             Object.class);
		type.addPropertyGetter("port",      		       Integer.class);

		type.addMethod("getFolders")
				.setReturnType("List<String>")
				.setSource("return getProperty(foldersProperty);");

		type.addMethod("getAvailableFoldersOnServer")
				.setReturnType("List<String>")
				.setSource("return org.structr.mail.entity.Mailbox.getAvailableFoldersOnServerImpl(this, securityContext);")
				.setDoExport(true);


		type.relate(mail, "CONTAINS_EMAILMESSAGES", Cardinality.OneToMany, "mailbox", "emails").setCascadingDelete(JsonSchema.Cascade.sourceToTarget);

		// view configuration
		type.addViewProperty(PropertyView.Public, "id,type,name");
		type.addViewProperty(PropertyView.Ui,     "id,type,name,host,user,password,mailProtocol,emails,folders,overrideMailEntityType");
	}}

	String getHost();
	String getUser();
	String getPassword();
	String getOverrideMailEntityType();
	List<String> getFolders();
	Object getMailProtocol();
	Integer getPort();

	static List<String> getAvailableFoldersOnServerImpl(final Mailbox mailbox, final SecurityContext securityContext) {
		Iterable<String> result = StructrApp.getInstance(securityContext).command(org.structr.mail.service.FetchFoldersCommand.class).execute(mailbox);
		if (result != null) {

			return StreamSupport.stream(result.spliterator(), false).collect(Collectors.toList());
		} else {

			return new ArrayList<>();
		}
	}

}
