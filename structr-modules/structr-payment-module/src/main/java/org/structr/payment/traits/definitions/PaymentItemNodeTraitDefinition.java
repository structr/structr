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
package org.structr.payment.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.payment.entity.PaymentItemNode;
import org.structr.payment.traits.wrappers.PaymentItemNodeTraitWrapper;

import java.util.Map;
import java.util.Set;

/**
 *
 */
public class PaymentItemNodeTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String PAYMENT_PROPERTY     = "payment";
	public static final String AMOUNT_PROPERTY      = "amount";
	public static final String QUANTITY_PROPERTY    = "quantity";
	public static final String DESCRIPTION_PROPERTY = "description";
	public static final String NUMBER_PROPERTY      = "number";
	public static final String URL_PROPERTY         = "url";

	public PaymentItemNodeTraitDefinition() {
		super(StructrTraits.PAYMENT_ITEM_NODE);
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(
			IsValid.class,
			new IsValid() {
				@Override
				public Boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer) {

					final Traits traits = obj.getTraits();
					boolean valid       = true;

					valid &= ValidationHelper.isValidPropertyNotNull(obj, traits.key(AMOUNT_PROPERTY), errorBuffer);
					valid &= ValidationHelper.isValidPropertyNotNull(obj, traits.key(QUANTITY_PROPERTY), errorBuffer);

					return valid;
				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			PaymentItemNode.class, (traits, node) -> new PaymentItemNodeTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<NodeInterface> paymentProperty = new StartNode(PAYMENT_PROPERTY, StructrTraits.PAYMENT_NODE_PAYMENT_ITEM_PAYMENT_ITEM);
		final Property<Integer> amountProperty        = new IntProperty(AMOUNT_PROPERTY).indexed();
		final Property<Integer> quantityProperty      = new IntProperty(QUANTITY_PROPERTY).indexed();
		final Property<String> descriptionProperty    = new StringProperty(DESCRIPTION_PROPERTY);
		final Property<String> numberProperty         = new StringProperty(NUMBER_PROPERTY);
		final Property<String> urlProperty            = new StringProperty(URL_PROPERTY);

		return newSet(
			paymentProperty,
			amountProperty,
			quantityProperty,
			descriptionProperty,
			numberProperty,
			urlProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
					NodeInterfaceTraitDefinition.NAME_PROPERTY, AMOUNT_PROPERTY, QUANTITY_PROPERTY, DESCRIPTION_PROPERTY, NUMBER_PROPERTY, URL_PROPERTY
			),
			PropertyView.Ui,
			newSet(
					AMOUNT_PROPERTY, QUANTITY_PROPERTY, DESCRIPTION_PROPERTY, NUMBER_PROPERTY, URL_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
