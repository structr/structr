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
package org.structr.payment.entity;

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.payment.api.PaymentItem;
import org.structr.schema.SchemaService;

/**
 *
 */
public class PaymentItemNode extends AbstractNode implements PaymentItem {

	static {

		SchemaService.registerBuiltinTypeOverride("PaymentItemNode", PaymentItemNode.class.getName());
	}

	public static final Property<PaymentNode> payment           = new StartNode<>("payment", PaymentItems.class);
	public static final Property<Integer>     amount            = new IntProperty("amount").indexed();
	public static final Property<Integer>     quantity          = new IntProperty("quantity").indexed();
	public static final Property<String>      description       = new StringProperty("description");
	public static final Property<String>      number            = new StringProperty("number");
	public static final Property<String>      url               = new StringProperty("url");

	public static final View defaultView = new View(PaymentItemNode.class, PropertyView.Public,
		name, amount, quantity, description, number, url
	);

	public static final View uiView = new View(PaymentItemNode.class, PropertyView.Ui,
		name, amount, quantity, description, number, url
	);

	@Override
	public int getAmount() {
		return getProperty(amount);
	}

	@Override
	public int getQuantity() {
		return getProperty(quantity);
	}

	@Override
	public String getDescription() {
		return getProperty(description);
	}

	@Override
	public String getItemNumber() {
		return getProperty(number);
	}

	@Override
	public String getItemUrl() {
		return getProperty(url);
	}
}
