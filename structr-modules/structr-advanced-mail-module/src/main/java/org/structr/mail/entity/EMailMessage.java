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
import org.structr.api.graph.PropagationDirection;
import org.structr.api.graph.PropagationMode;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonReferenceType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.PropertyView;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.SchemaService;

import java.net.URI;
import java.util.Date;

public interface EMailMessage extends NodeInterface {

	class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("EMailMessage");
		final JsonObjectType file = schema.addType("File");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/EMailMessage"));

		type.addStringProperty("subject",          PropertyView.Ui).setIndexed(true);
		type.addStringProperty("from",             PropertyView.Ui).setIndexed(true);
		type.addStringProperty("fromMail",         PropertyView.Ui).setIndexed(true);
		type.addStringProperty("to",               PropertyView.Ui).setIndexed(true);
		type.addStringProperty("cc",               PropertyView.Ui).setIndexed(true);
		type.addStringProperty("bcc",              PropertyView.Ui).setIndexed(true);
		type.addStringProperty("replyTo",          PropertyView.Ui).setIndexed(true);
		type.addStringProperty("content",          PropertyView.Ui).setIndexed(false);
		type.addStringProperty("htmlContent",      PropertyView.Ui).setIndexed(false);
		type.addStringProperty("folder",           PropertyView.Ui).setIndexed(true);
		type.addStringProperty("header",           PropertyView.Ui).setIndexed(false);
		type.addStringProperty("messageId",        PropertyView.Ui).setIndexed(true);
		type.addStringProperty("inReplyTo",        PropertyView.Ui).setIndexed(true);

		type.addDateProperty("receivedDate",       PropertyView.Ui).setIndexed(true);
		type.addDateProperty("sentDate",           PropertyView.Ui).setIndexed(true);

		type.addPropertyGetter("subject",           String.class);
		type.addPropertyGetter("from",              String.class);
		type.addPropertyGetter("fromMail",          String.class);
		type.addPropertyGetter("to",                String.class);
		type.addPropertyGetter("content",           String.class);
		type.addPropertyGetter("folder",            String.class);
		type.addPropertyGetter("header",            String.class);
		type.addPropertyGetter("messageId",         String.class);
		type.addPropertyGetter("inReplyTo",         String.class);
		type.addPropertyGetter("receivedDate",      Date.class);
		type.addPropertyGetter("sentDate",          Date.class);

		JsonReferenceType rel = type.relate(file, "HAS_ATTACHMENT", Cardinality.OneToMany, "attachedMail", "attachedFiles");
		rel.setCascadingDelete(JsonSchema.Cascade.sourceToTarget);
		rel.setReadPermissionPropagation(PropagationMode.Add);
		rel.setPermissionPropagation(PropagationDirection.Out);

		// view configuration
		type.addViewProperty(PropertyView.Public, "id,type");
		type.addViewProperty(PropertyView.Ui,     "id,type,subject,from,fromMail,to,content,htmlContent,folder,receivedDate,sentDate,mailbox,header,messageId,inReplyTo,attachedFiles");
	}}

}
