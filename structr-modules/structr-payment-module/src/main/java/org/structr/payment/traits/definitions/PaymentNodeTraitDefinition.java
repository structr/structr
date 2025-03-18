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
import org.structr.core.traits.StructrTraits;
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

	public static final String ITEMS_PROPERTY                   = "items";
	public static final String STATE_PROPERTY                   = "state";
	public static final String DESCRIPTION_PROPERTY             = "description";
	public static final String CURRENCY_PROPERTY                = "currency";
	public static final String TOKEN_PROPERTY                   = "token";
	public static final String BILLING_AGREEMENT_ID_PROPERTY    = "billingAgreementId";
	public static final String NOTE_PROPERTY                    = "note";
	public static final String BILLING_ADDRESS_NAME_PROPERTY    = "billingAddressName";
	public static final String BILLING_ADDRESS_STREET1_PROPERTY = "billingAddressStreet1";
	public static final String BILLING_ADDRESS_STREET2_PROPERTY = "billingAddressStreet2";
	public static final String BILLING_ADDRESS_ZIP_PROPERTY     = "billingAddressZip";
	public static final String BILLING_ADDRESS_CITY_PROPERTY    = "billingAddressCity";
	public static final String BILLING_ADDRESS_COUNTRY_PROPERTY = "billingAddressCountry";
	public static final String INVOICE_ID_PROPERTY              = "invoiceId";
	public static final String PAYER_ADDRESS_NAME_PROPERTY      = "payerAddressName";
	public static final String PAYER_ADDRESS_STREET1_PROPERTY   = "payerAddressStreet1";
	public static final String PAYER_ADDRESS_STREET2_PROPERTY   = "payerAddressStreet2";
	public static final String PAYER_ADDRESS_ZIP_PROPERTY       = "payerAddressZip";
	public static final String PAYER_ADDRESS_CITY_PROPERTY      = "payerAddressCity";
	public static final String PAYER_ADDRESS_COUNTRY_PROPERTY   = "payerAddressCountry";
	public static final String PAYER_PROPERTY                   = "payer";
	public static final String PAYER_BUSINESS_PROPERTY          = "payerBusiness";

	public PaymentNodeTraitDefinition() {
		super(StructrTraits.PAYMENT_NODE);
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

		final Property<Iterable<NodeInterface>> itemsProperty = new EndNodes(ITEMS_PROPERTY, StructrTraits.PAYMENT_NODE_PAYMENT_ITEM_PAYMENT_ITEM);
		final Property<String> stateProperty                  = new EnumProperty(STATE_PROPERTY, PaymentState.class);
		final Property<String> descriptionProperty            = new StringProperty(DESCRIPTION_PROPERTY).indexed();
		final Property<String> currencyProperty               = new StringProperty(CURRENCY_PROPERTY).indexed();
		final Property<String> tokenProperty                  = new StringProperty(TOKEN_PROPERTY).indexed();
		final Property<String> billingAgreementIdProperty     = new StringProperty(BILLING_AGREEMENT_ID_PROPERTY);
		final Property<String> noteProperty                   = new StringProperty(NOTE_PROPERTY);
		final Property<String> billingAddressNameProperty     = new StringProperty(BILLING_ADDRESS_NAME_PROPERTY);
		final Property<String> billingAddressStreet1Property  = new StringProperty(BILLING_ADDRESS_STREET1_PROPERTY);
		final Property<String> billingAddressStreet2Property  = new StringProperty(BILLING_ADDRESS_STREET2_PROPERTY);
		final Property<String> billingAddressZipProperty      = new StringProperty(BILLING_ADDRESS_ZIP_PROPERTY);
		final Property<String> billingAddressCityProperty     = new StringProperty(BILLING_ADDRESS_CITY_PROPERTY);
		final Property<String> billingAddressCountryProperty  = new StringProperty(BILLING_ADDRESS_COUNTRY_PROPERTY);
		final Property<String> invoiceIdProperty              = new StringProperty(INVOICE_ID_PROPERTY);
		final Property<String> payerAddressNameProperty       = new StringProperty(PAYER_ADDRESS_NAME_PROPERTY);
		final Property<String> payerAddressStreet1Property    = new StringProperty(PAYER_ADDRESS_STREET1_PROPERTY);
		final Property<String> payerAddressStreet2Property    = new StringProperty(PAYER_ADDRESS_STREET2_PROPERTY);
		final Property<String> payerAddressZipProperty        = new StringProperty(PAYER_ADDRESS_ZIP_PROPERTY);
		final Property<String> payerAddressCityProperty       = new StringProperty(PAYER_ADDRESS_CITY_PROPERTY);
		final Property<String> payerAddressCountryProperty    = new StringProperty(PAYER_ADDRESS_COUNTRY_PROPERTY);
		final Property<String> payerProperty                  = new StringProperty(PAYER_PROPERTY);
		final Property<String> payerBusinessProperty          = new StringProperty(PAYER_BUSINESS_PROPERTY);

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
					STATE_PROPERTY, DESCRIPTION_PROPERTY, CURRENCY_PROPERTY, TOKEN_PROPERTY, BILLING_AGREEMENT_ID_PROPERTY,
					NOTE_PROPERTY, BILLING_ADDRESS_NAME_PROPERTY, BILLING_ADDRESS_STREET1_PROPERTY, BILLING_ADDRESS_STREET2_PROPERTY,
					BILLING_ADDRESS_ZIP_PROPERTY, BILLING_ADDRESS_CITY_PROPERTY, BILLING_ADDRESS_COUNTRY_PROPERTY, INVOICE_ID_PROPERTY,
					PAYER_ADDRESS_NAME_PROPERTY, PAYER_ADDRESS_STREET1_PROPERTY, PAYER_ADDRESS_STREET2_PROPERTY, PAYER_ADDRESS_ZIP_PROPERTY,
					PAYER_ADDRESS_CITY_PROPERTY, PAYER_ADDRESS_COUNTRY_PROPERTY, PAYER_PROPERTY, PAYER_BUSINESS_PROPERTY, ITEMS_PROPERTY
			),
			PropertyView.Ui,
			newSet(
					STATE_PROPERTY, DESCRIPTION_PROPERTY, CURRENCY_PROPERTY, TOKEN_PROPERTY, BILLING_AGREEMENT_ID_PROPERTY,
					NOTE_PROPERTY, BILLING_ADDRESS_NAME_PROPERTY, BILLING_ADDRESS_STREET1_PROPERTY, BILLING_ADDRESS_STREET2_PROPERTY,
					BILLING_ADDRESS_ZIP_PROPERTY, BILLING_ADDRESS_CITY_PROPERTY, BILLING_ADDRESS_COUNTRY_PROPERTY, INVOICE_ID_PROPERTY,
					PAYER_ADDRESS_NAME_PROPERTY, PAYER_ADDRESS_STREET1_PROPERTY, PAYER_ADDRESS_STREET2_PROPERTY, PAYER_ADDRESS_ZIP_PROPERTY,
					PAYER_ADDRESS_CITY_PROPERTY, PAYER_ADDRESS_COUNTRY_PROPERTY, PAYER_PROPERTY, PAYER_BUSINESS_PROPERTY, ITEMS_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
