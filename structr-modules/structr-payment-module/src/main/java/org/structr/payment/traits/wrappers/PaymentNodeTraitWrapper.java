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

package org.structr.payment.traits.wrappers;

import org.structr.api.util.Iterables;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.payment.api.*;
import org.structr.payment.entity.PaymentNode;
import org.structr.payment.impl.paypal.PayPalErrorToken;
import org.structr.payment.impl.paypal.PayPalPaymentProvider;
import org.structr.payment.impl.stripe.StripePaymentProvider;
import org.structr.payment.impl.test.TestPaymentProvider;
import org.structr.payment.traits.definitions.PaymentNodeTraitDefinition;

/**
 *
 */
public class PaymentNodeTraitWrapper extends AbstractNodeTraitWrapper implements PaymentNode {

	public PaymentNodeTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public Iterable<PaymentItem> getItems() {

		final Iterable<NodeInterface> nodes = getProperty(traits.key(PaymentNodeTraitDefinition.ITEMS_PROPERTY));

		return Iterables.map(n -> n.as(PaymentItem.class), nodes);
	}

	@Override
	public String getCurrencyCode() {
		return getProperty(traits.key(PaymentNodeTraitDefinition.CURRENCY_PROPERTY));
	}

	@Override
	public String getDescription() {
		return getProperty(traits.key(PaymentNodeTraitDefinition.DESCRIPTION_PROPERTY));
	}

	@Override
	public void setDescription(final String description) throws FrameworkException {
		setProperty(traits.key(PaymentNodeTraitDefinition.DESCRIPTION_PROPERTY), description);
	}

	@Override
	public String getBillingAddressName() {
		return getProperty(traits.key(PaymentNodeTraitDefinition.BILLING_ADDRESS_NAME_PROPERTY));
	}

	@Override
	public void setBillingAddressName(String billingAddressName) throws FrameworkException {
		setProperty(traits.key(PaymentNodeTraitDefinition.BILLING_ADDRESS_NAME_PROPERTY), billingAddressName);
	}

	@Override
	public String getBillingAddressStreet1() {
		return getProperty(traits.key(PaymentNodeTraitDefinition.BILLING_ADDRESS_STREET1_PROPERTY));
	}

	@Override
	public void setBillingAddressStreet1(String billingAddressStreet1) throws FrameworkException {
		setProperty(traits.key(PaymentNodeTraitDefinition.BILLING_ADDRESS_STREET1_PROPERTY), billingAddressStreet1);
	}

	@Override
	public String getBillingAddressStreet2() {
		return getProperty(traits.key(PaymentNodeTraitDefinition.BILLING_ADDRESS_STREET2_PROPERTY));
	}

	@Override
	public void setBillingAddressStreet2(String billingAddressStreet2) throws FrameworkException {
		setProperty(traits.key(PaymentNodeTraitDefinition.BILLING_ADDRESS_STREET2_PROPERTY), billingAddressStreet2);
	}

	@Override
	public String getBillingAddressZip() {
		return getProperty(traits.key(PaymentNodeTraitDefinition.BILLING_ADDRESS_ZIP_PROPERTY));
	}

	@Override
	public void setBillingAddressZip(String billingAddressZip) throws FrameworkException {
		setProperty(traits.key(PaymentNodeTraitDefinition.BILLING_ADDRESS_ZIP_PROPERTY), billingAddressZip);
	}

	@Override
	public String getBillingAddressCity() {
		return getProperty(traits.key(PaymentNodeTraitDefinition.BILLING_ADDRESS_CITY_PROPERTY));
	}

	@Override
	public void setBillingAddressCity(String billingAddressCity) throws FrameworkException {
		setProperty(traits.key(PaymentNodeTraitDefinition.BILLING_ADDRESS_CITY_PROPERTY), billingAddressCity);
	}

	@Override
	public String getBillingAddressCountry() {
		return getProperty(traits.key(PaymentNodeTraitDefinition.BILLING_ADDRESS_COUNTRY_PROPERTY));
	}

	@Override
	public void setBillingAddressCountry(String billingAddressCountry) throws FrameworkException {
		setProperty(traits.key(PaymentNodeTraitDefinition.BILLING_ADDRESS_COUNTRY_PROPERTY), billingAddressCountry);
	}

	@Override
	public String getPayer() {
		return getProperty(traits.key(PaymentNodeTraitDefinition.PAYER_PROPERTY));
	}

	@Override
	public void setPayer(String payer) throws FrameworkException {
		setProperty(traits.key(PaymentNodeTraitDefinition.PAYER_PROPERTY), payer);
	}

	@Override
	public String getPayerBusiness() {
		return getProperty(traits.key(PaymentNodeTraitDefinition.PAYER_BUSINESS_PROPERTY));
	}

	@Override
	public void setPayerBusiness(String payerBusiness) throws FrameworkException {
		setProperty(traits.key(PaymentNodeTraitDefinition.PAYER_BUSINESS_PROPERTY), payerBusiness);
	}

	@Override
	public String getPayerAddressName() {
		return getProperty(traits.key(PaymentNodeTraitDefinition.PAYER_ADDRESS_NAME_PROPERTY));
	}

	@Override
	public void setPayerAddressName(String payerAddressName) throws FrameworkException {
		setProperty(traits.key(PaymentNodeTraitDefinition.PAYER_ADDRESS_NAME_PROPERTY), payerAddressName);
	}

	@Override
	public String getPayerAddressStreet1() {
		return getProperty(traits.key(PaymentNodeTraitDefinition.PAYER_ADDRESS_STREET1_PROPERTY));
	}

	@Override
	public void setPayerAddressStreet1(String payerAddressStreet1) throws FrameworkException {
		setProperty(traits.key(PaymentNodeTraitDefinition.PAYER_ADDRESS_STREET1_PROPERTY), payerAddressStreet1);
	}

	@Override
	public String getPayerAddressStreet2() {
		return getProperty(traits.key(PaymentNodeTraitDefinition.PAYER_ADDRESS_STREET2_PROPERTY));
	}

	@Override
	public void setPayerAddressStreet2(String payerAddressStreet2) throws FrameworkException {
		setProperty(traits.key(PaymentNodeTraitDefinition.PAYER_ADDRESS_STREET2_PROPERTY), payerAddressStreet2);
	}

	@Override
	public String getPayerAddressZip() {
		return getProperty(traits.key(PaymentNodeTraitDefinition.PAYER_ADDRESS_ZIP_PROPERTY));
	}

	@Override
	public void setPayerAddressZip(String payerAddressZip) throws FrameworkException {
		setProperty(traits.key(PaymentNodeTraitDefinition.PAYER_ADDRESS_ZIP_PROPERTY), payerAddressZip);
	}

	@Override
	public String getPayerAddressCity() {
		return getProperty(traits.key(PaymentNodeTraitDefinition.PAYER_ADDRESS_CITY_PROPERTY));
	}

	@Override
	public void setPayerAddressCity(String payerAddressCity) throws FrameworkException {
		setProperty(traits.key(PaymentNodeTraitDefinition.PAYER_ADDRESS_CITY_PROPERTY), payerAddressCity);
	}

	@Override
	public String getPayerAddressCountry() {
		return getProperty(traits.key(PaymentNodeTraitDefinition.PAYER_ADDRESS_COUNTRY_PROPERTY));
	}

	@Override
	public void setPayerAddressCountry(String payerAddressCountry) throws FrameworkException {
		setProperty(traits.key(PaymentNodeTraitDefinition.PAYER_ADDRESS_COUNTRY_PROPERTY), payerAddressCountry);
	}

	@Override
	public String getBillingAgreementId() {
		return getProperty(traits.key(PaymentNodeTraitDefinition.BILLING_AGREEMENT_ID_PROPERTY));
	}

	@Override
	public void setBillingAgreementId(String billingAgreementId) throws FrameworkException {
		setProperty(traits.key(PaymentNodeTraitDefinition.BILLING_AGREEMENT_ID_PROPERTY), billingAgreementId);
	}

	@Override
	public String getNote() {
		return getProperty(traits.key(PaymentNodeTraitDefinition.NOTE_PROPERTY));
	}

	@Override
	public void setNote(String note) throws FrameworkException {
		setProperty(traits.key(PaymentNodeTraitDefinition.NOTE_PROPERTY), note);
	}

	@Override
	public String getInvoiceId() {
		return getProperty(traits.key(PaymentNodeTraitDefinition.INVOICE_ID_PROPERTY));
	}

	@Override
	public void setInvoiceId(final String invoiceId) throws FrameworkException {
		setProperty(traits.key(PaymentNodeTraitDefinition.INVOICE_ID_PROPERTY), invoiceId);
	}

	@Override
	public String getPaymentState() {
		return getProperty(traits.key(PaymentNodeTraitDefinition.STATE_PROPERTY));
	}

	@Override
	public void setPaymentState(final String state) throws FrameworkException {
		setProperty(traits.key(PaymentNodeTraitDefinition.STATE_PROPERTY), state);
	}

	@Override
	public String getToken() {
		return getProperty(traits.key(PaymentNodeTraitDefinition.TOKEN_PROPERTY));
	}

	@Override
	public void setToken(final String token) throws FrameworkException {
		setProperty(traits.key(PaymentNodeTraitDefinition.TOKEN_PROPERTY), token);
	}

	@Override
	public GraphObject beginCheckout(final String providerName, final String successUrl, final String cancelUrl) throws FrameworkException {

		final PaymentProvider provider = PaymentNodeTraitWrapper.getPaymentProvider(providerName);
		if (provider != null) {

			final BeginCheckoutResponse response = provider.beginCheckout(this, successUrl, cancelUrl);
			if (CheckoutState.Success.equals(response.getCheckoutState())) {

				final GraphObjectMap data = new GraphObjectMap();

				data.put(traits.key(PaymentNodeTraitDefinition.TOKEN_PROPERTY), response.getToken());

				return data;

			} else {

				throwErrors("Unable to begin checkout.", response);
			}

		} else {

			throw new FrameworkException(422, "Payment provider " + providerName + " not found.");
		}

		return null;
	}

	@Override
	public void cancelCheckout(final String providerName, final String token) throws FrameworkException {

		final PaymentProvider provider = PaymentNodeTraitWrapper.getPaymentProvider(providerName);
		if (provider != null) {

			provider.cancelCheckout(this);

		} else {

			throw new FrameworkException(422, "Payment provider " + providerName + " not found");
		}
	}

	@Override
	public GraphObject confirmCheckout(final String providerName, final String notifyUrl, final String token, final String payerId) throws FrameworkException {

		final PaymentProvider provider = PaymentNodeTraitWrapper.getPaymentProvider(providerName);
		if (provider != null) {

			final ConfirmCheckoutResponse response = provider.confirmCheckout(this, notifyUrl, token, payerId);
			if (CheckoutState.Success.equals(response.getCheckoutState())) {

				// no return value neccessary, will result in code 200
				return null;

			} else {

				// FIXME: checkout error should NOT cause the transaction to be rolled back,
				//        because we're losing information (payment state etc.)
				throwErrors("Unable to confirm checkout", response);
			}

		} else {

			throw new FrameworkException(422, "Payment provider " + providerName + " not found");
		}

		return null;
	}

	// ----- interface Payment -----
	public int getTotal() {

		int total = 0;

		for (final PaymentItem item : this.getItems()) {
			total += item.getAmount() * item.getQuantity();
		}

		return total;
	}

	public static PaymentProvider getPaymentProvider(final String providerName) {

		switch (providerName) {

			case "paypal":
				return new PayPalPaymentProvider();

			case "stripe":
				return new StripePaymentProvider();

			case "test":
				return new TestPaymentProvider();
		}

		return null;
	}

	void throwErrors(final String cause, final APIResponse response) throws FrameworkException {

		final ErrorBuffer errorBuffer = new ErrorBuffer();

		for (final APIError error : response.getErrors()) {

			errorBuffer.add(new PayPalErrorToken(StructrTraits.PAYMENT_NODE, "base", error.getErrorCode(), error.getLongMessage()));
		}

		throw new FrameworkException(422, cause, errorBuffer);
	}
}
