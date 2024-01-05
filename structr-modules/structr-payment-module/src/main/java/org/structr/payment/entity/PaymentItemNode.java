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
package org.structr.payment.entity;

import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.PropertyView;
import org.structr.core.graph.NodeInterface;
import org.structr.payment.api.PaymentItem;
import org.structr.schema.SchemaService;

import java.net.URI;

/**
 *
 */
public interface PaymentItemNode extends NodeInterface, PaymentItem {


	static class Impl { static {

		final JsonSchema schema      = SchemaService.getDynamicSchema();
		final JsonObjectType type    = schema.addType("PaymentItemNode");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/PaymentItemNode"));

		type.addIntegerProperty("amount",     PropertyView.Public, PropertyView.Ui).setIndexed(true);
		type.addIntegerProperty("quantity",   PropertyView.Public, PropertyView.Ui).setIndexed(true);
		type.addStringProperty("description", PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("number",      PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("url",         PropertyView.Public, PropertyView.Ui);

		type.addPropertyGetter("amount",      Integer.TYPE);
		type.addPropertyGetter("quantity",    Integer.TYPE);
		type.addPropertyGetter("description", String.class);
		type.addPropertyGetter("number",      String.class);
		type.addPropertyGetter("url",         String.class);

		type.addPropertySetter("amount",      Integer.TYPE);
		type.addPropertySetter("quantity",    Integer.TYPE);
		type.addPropertySetter("description", String.class);
		type.addPropertySetter("number",      String.class);
		type.addPropertySetter("url",         String.class);

		type.addMethod("getItemNumber").setSource("return getProperty(numberProperty);").setReturnType(String.class.getName());
		type.addMethod("getItemUrl").setSource("return getProperty(urlProperty);").setReturnType(String.class.getName());

		// view configuration
		type.addViewProperty(PropertyView.Public, "name");
	}}
}
