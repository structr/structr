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
package org.structr.payment.traits.wrappers;

import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.payment.entity.PaymentItemNode;
import org.structr.payment.traits.definitions.PaymentItemNodeTraitDefinition;

/**
 *
 */
public class PaymentItemNodeTraitWrapper extends AbstractNodeTraitWrapper implements PaymentItemNode {

	public PaymentItemNodeTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public int getAmount() {
		return wrappedObject.getProperty(traits.key(PaymentItemNodeTraitDefinition.AMOUNT_PROPERTY));
	}

	@Override
	public int getQuantity() {
		return wrappedObject.getProperty(traits.key(PaymentItemNodeTraitDefinition.QUANTITY_PROPERTY));
	}

	@Override
	public String getDescription() {
		return wrappedObject.getProperty(traits.key(PaymentItemNodeTraitDefinition.DESCRIPTION_PROPERTY));
	}

	@Override
	public String getItemNumber() {
		return wrappedObject.getProperty(traits.key(PaymentItemNodeTraitDefinition.NUMBER_PROPERTY));
	}

	@Override
	public String getItemUrl() {
		return wrappedObject.getProperty(traits.key(PaymentItemNodeTraitDefinition.URL_PROPERTY));
	}
}
