/**
 * Copyright (C) 2010-2017 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.entity;

import java.net.URI;
import org.structr.common.PropertyView;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.SchemaService;
import org.structr.schema.json.JsonObjectType;
import org.structr.schema.json.JsonSchema;

/**
 */
public interface Person extends NodeInterface {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("Person");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/Person"));

		type.addStringProperty("salutation",          PropertyView.Public);
		type.addStringProperty("firstName",           PropertyView.Public);
		type.addStringProperty("middleNameOrInitial", PropertyView.Public);
		type.addStringProperty("lastName",            PropertyView.Public);

		type.addStringProperty("twitterName",         PropertyView.Public);
		type.addStringProperty("eMail",               PropertyView.Public);
		type.addStringProperty("eMail2",              PropertyView.Public);

		type.addStringProperty("phoneNumber1",        PropertyView.Public);
		type.addStringProperty("phoneNumber2",        PropertyView.Public);
		type.addStringProperty("faxNumber1",          PropertyView.Public);
		type.addStringProperty("faxNumber2",          PropertyView.Public);

		type.addStringProperty("street",              PropertyView.Public);
		type.addStringProperty("zipCode",             PropertyView.Public);
		type.addStringProperty("city",                PropertyView.Public);
		type.addStringProperty("state",               PropertyView.Public);

		type.addDateProperty("birthday",              PropertyView.Public);
		type.addStringProperty("gender",              PropertyView.Public);
		type.addBooleanProperty("newsletter",         PropertyView.Public);
	}}
}
