/*
 * Copyright (C) 2010-2023 Structr GmbH
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

import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.PropertyView;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.SchemaService;

import java.net.URI;

/**
 */
public interface Person extends NodeInterface {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("Person");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/Person"));
		type.setCategory("core");

		type.addStringProperty("salutation",          PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("firstName",           PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("middleNameOrInitial", PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("lastName",            PropertyView.Public, PropertyView.Ui);

		type.addStringProperty("eMail",               PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("eMail2",              PropertyView.Ui);

		type.addStringProperty("phoneNumber1",        PropertyView.Ui);
		type.addStringProperty("phoneNumber2",        PropertyView.Ui);
		type.addStringProperty("faxNumber1",          PropertyView.Ui);
		type.addStringProperty("faxNumber2",          PropertyView.Ui);

		type.addStringProperty("country",             PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("street",              PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("zipCode",             PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("city",                PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("state",               PropertyView.Public, PropertyView.Ui);

		type.addDateProperty("birthday",              PropertyView.Ui);
		type.addStringProperty("gender",              PropertyView.Ui);
		type.addBooleanProperty("newsletter",         PropertyView.Ui);

		// view configuration
		type.addViewProperty(PropertyView.Public, "name");
	}}
}
