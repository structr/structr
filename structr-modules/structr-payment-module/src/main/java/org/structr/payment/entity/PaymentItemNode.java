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

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.payment.api.PaymentItem;
import org.structr.payment.entity.relationship.PaymentNodepaymentItemPaymentItem;

/**
 *
 */
public class PaymentItemNode extends AbstractNode implements PaymentItem {

	public static final Property<PaymentNode> paymentProperty = new StartNode<>("payment", PaymentNodepaymentItemPaymentItem.class);
	public static final Property<Integer> amountProperty      = new IntProperty("amount").indexed();
	public static final Property<Integer> quantityProperty    = new IntProperty("quantity").indexed();
	public static final Property<String> descriptionProperty  = new StringProperty("description");
	public static final Property<String> numberProperty       = new StringProperty("number");
	public static final Property<String> urlProperty          = new StringProperty("url");

	public static final View defaultView = new View(PaymentItemNode.class, PropertyView.Public,
		name, amountProperty, quantityProperty, descriptionProperty, numberProperty, urlProperty
	);

	public static final View uiView = new View(PaymentItemNode.class, PropertyView.Ui,
		amountProperty, quantityProperty, descriptionProperty, numberProperty, urlProperty
	);

	@Override
	public boolean isValid(final ErrorBuffer errorBuffer) {

		boolean valid = super.isValid(errorBuffer);

		valid &= ValidationHelper.isValidPropertyNotNull(this, PaymentItemNode.amountProperty, errorBuffer);
		valid &= ValidationHelper.isValidPropertyNotNull(this, PaymentItemNode.quantityProperty, errorBuffer);

		return valid;
	}

	@Override
	public int getAmount() {
		return getProperty(amountProperty);
	}

	@Override
	public int getQuantity() {
		return getProperty(quantityProperty);
	}

	@Override
	public String getDescription() {
		return getProperty(descriptionProperty);
	}

	@Override
	public String getItemNumber() {
		return getProperty(numberProperty);
	}

	@Override
	public String getItemUrl() {
		return getProperty(urlProperty);
	}
}
