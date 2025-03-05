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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.payment.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.api.JavaMethod;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.payment.api.PaymentState;
import org.structr.payment.entity.PaymentNode;
import org.structr.payment.traits.wrappers.PaymentNodeTraitWrapper;
import org.structr.schema.action.EvaluationHints;

import java.util.Map;
import java.util.Set;

/**
 *
 */
public class PaymentNodeTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String DESCRIPTION_PROPERTY         = "description";

	public PaymentNodeTraitDefinition() {
		super("PaymentNode");
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {

		return newSet(

			new JavaMethod("beginCheckout", false, false) {

				@Override
				public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {

					final String providerName = (String)arguments.get("providerName");
					final String successUrl   = (String)arguments.get("successUrl");
					final String cancelUrl    = (String)arguments.get("cancelUrl");

					return entity.as(PaymentNode.class).beginCheckout(providerName, successUrl, cancelUrl);
				}
			},

			new JavaMethod("cancelCheckout", false, false) {

				@Override
				public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {

					final String providerName = (String)arguments.get("providerName");
					final String token        = (String)arguments.get("token");

					entity.as(PaymentNode.class).cancelCheckout(providerName, token);

					return null;
				}
			},

			new JavaMethod("confirmCheckout", false, false) {

				@Override
				public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {

					final String providerName = (String)arguments.get("providerName");
					final String notifyUrl    = (String)arguments.get("notifyUrl");
					final String token        = (String)arguments.get("token");
					final String payerId      = (String)arguments.get("payerId");

					return entity.as(PaymentNode.class).confirmCheckout(providerName, notifyUrl, token, payerId);
				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			PaymentNode.class, (traits, node) -> new PaymentNodeTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<Iterable<NodeInterface>> itemsProperty = new EndNodes("items", "PaymentNodepaymentItemPaymentItem");
		final Property<String> stateProperty                  = new EnumProperty("state", PaymentState.class);
		final Property<String> descriptionProperty            = new StringProperty(DESCRIPTION_PROPERTY).indexed();
		final Property<String> currencyProperty               = new StringProperty("currency").indexed();
		final Property<String> tokenProperty                  = new StringProperty("token").indexed();
		final Property<String> billingAgreementIdProperty     = new StringProperty("billingAgreementId");
		final Property<String> noteProperty                   = new StringProperty("note");
		final Property<String> billingAddressNameProperty     = new StringProperty("billingAddressName");
		final Property<String> billingAddressStreet1Property  = new StringProperty("billingAddressStreet1");
		final Property<String> billingAddressStreet2Property  = new StringProperty("billingAddressStreet2");
		final Property<String> billingAddressZipProperty      = new StringProperty("billingAddressZip");
		final Property<String> billingAddressCityProperty     = new StringProperty("billingAddressCity");
		final Property<String> billingAddressCountryProperty  = new StringProperty("billingAddressCountry");
		final Property<String> invoiceIdProperty              = new StringProperty("invoiceId");
		final Property<String> payerAddressNameProperty       = new StringProperty("payerAddressName");
		final Property<String> payerAddressStreet1Property    = new StringProperty("payerAddressStreet1");
		final Property<String> payerAddressStreet2Property    = new StringProperty("payerAddressStreet2");
		final Property<String> payerAddressZipProperty        = new StringProperty("payerAddressZip");
		final Property<String> payerAddressCityProperty       = new StringProperty("payerAddressCity");
		final Property<String> payerAddressCountryProperty    = new StringProperty("payerAddressCountry");
		final Property<String> payerProperty                  = new StringProperty("payer");
		final Property<String> payerBusinessProperty          = new StringProperty("payerBusiness");

		return newSet(
			itemsProperty,
			stateProperty,
			descriptionProperty,
			currencyProperty,
			tokenProperty,
			billingAgreementIdProperty,
			noteProperty,
			billingAddressNameProperty,
			billingAddressStreet1Property,
			billingAddressStreet2Property,
			billingAddressZipProperty,
			billingAddressCityProperty,
			billingAddressCountryProperty,
			invoiceIdProperty,
			payerAddressNameProperty,
			payerAddressStreet1Property,
			payerAddressStreet2Property,
			payerAddressZipProperty,
			payerAddressCityProperty,
			payerAddressCountryProperty,
			payerProperty,
			payerBusinessProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
				"state", DESCRIPTION_PROPERTY, "currency", "token", "billingAgreementId", "note", "billingAddressName", "billingAddressStreet1",
				"billingAddressStreet2", "billingAddressZip", "billingAddressCity", "billingAddressCountry", "invoiceId", "payerAddressName",
				"payerAddressStreet1", "payerAddressStreet2", "payerAddressZip", "payerAddressCity", "payerAddressCountry", "payer", "payerBusiness",
				"items"
			),
			PropertyView.Ui,
			newSet(
				"state", DESCRIPTION_PROPERTY, "currency", "token", "billingAgreementId", "note", "billingAddressName", "billingAddressStreet1",
				"billingAddressStreet2", "billingAddressZip", "billingAddressCity", "billingAddressCountry", "invoiceId", "payerAddressName",
				"payerAddressStreet1", "payerAddressStreet2", "payerAddressZip", "payerAddressCity", "payerAddressCountry", "payer", "payerBusiness",
				"items"
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
