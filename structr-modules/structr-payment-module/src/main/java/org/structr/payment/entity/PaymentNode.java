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

package org.structr.payment.entity;

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.Export;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.EndNodes;
import org.structr.core.property.EnumProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.payment.api.*;
import org.structr.payment.entity.relationship.PaymentNodepaymentItemPaymentItem;
import org.structr.payment.impl.paypal.PayPalErrorToken;
import org.structr.payment.impl.paypal.PayPalPaymentProvider;
import org.structr.payment.impl.stripe.StripePaymentProvider;
import org.structr.payment.impl.test.TestPaymentProvider;

/**
 *
 */
public class PaymentNode extends AbstractNode implements Payment {

	public static final Property<Iterable<PaymentItemNode>> itemsProperty = new EndNodes<>("items", PaymentNodepaymentItemPaymentItem.class);

	public static final Property<PaymentState> stateProperty           = new EnumProperty("state", PaymentState.class);
	public static final Property<String> descriptionProperty           = new StringProperty("description").indexed();
	public static final Property<String> currencyProperty              = new StringProperty("currency").indexed();
	public static final Property<String> tokenProperty                 = new StringProperty("token").indexed();
	public static final Property<String> billingAgreementIdProperty    = new StringProperty("billingAgreementId");
	public static final Property<String> noteProperty                  = new StringProperty("note");
	public static final Property<String> billingAddressNameProperty    = new StringProperty("billingAddressName");
	public static final Property<String> billingAddressStreet1Property = new StringProperty("billingAddressStreet1");
	public static final Property<String> billingAddressStreet2Property = new StringProperty("billingAddressStreet2");
	public static final Property<String> billingAddressZipProperty     = new StringProperty("billingAddressZip");
	public static final Property<String> billingAddressCityProperty    = new StringProperty("billingAddressCity");
	public static final Property<String> billingAddressCountryProperty = new StringProperty("billingAddressCountry");
	public static final Property<String> invoiceIdProperty             = new StringProperty("invoiceId");
	public static final Property<String> payerAddressNameProperty      = new StringProperty("payerAddressName");
	public static final Property<String> payerAddressStreet1Property   = new StringProperty("payerAddressStreet1");
	public static final Property<String> payerAddressStreet2Property   = new StringProperty("payerAddressStreet2");
	public static final Property<String> payerAddressZipProperty       = new StringProperty("payerAddressZip");
	public static final Property<String> payerAddressCityProperty      = new StringProperty("payerAddressCity");
	public static final Property<String> payerAddressCountryProperty   = new StringProperty("payerAddressCountry");
	public static final Property<String> payerProperty                 = new StringProperty("payer");
	public static final Property<String> payerBusinessProperty         = new StringProperty("payerBusiness");


	public static final View defaultView = new View(PaymentNode.class, PropertyView.Public,
		stateProperty, descriptionProperty, currencyProperty, tokenProperty, billingAgreementIdProperty, noteProperty, billingAddressNameProperty, billingAddressStreet1Property,
		billingAddressStreet2Property, billingAddressZipProperty, billingAddressCityProperty, billingAddressCountryProperty, invoiceIdProperty, payerAddressNameProperty,
		payerAddressStreet1Property, payerAddressStreet2Property, payerAddressZipProperty, payerAddressCityProperty, payerAddressCountryProperty, payerProperty, payerBusinessProperty,
		itemsProperty
	);

	public static final View uiView = new View(PaymentNode.class, PropertyView.Ui,
		stateProperty, descriptionProperty, currencyProperty, tokenProperty, billingAgreementIdProperty, noteProperty, billingAddressNameProperty, billingAddressStreet1Property,
		billingAddressStreet2Property, billingAddressZipProperty, billingAddressCityProperty, billingAddressCountryProperty, invoiceIdProperty, payerAddressNameProperty,
		payerAddressStreet1Property, payerAddressStreet2Property, payerAddressZipProperty, payerAddressCityProperty, payerAddressCountryProperty, payerProperty, payerBusinessProperty,
		itemsProperty
	);

	@Override
	public Iterable<PaymentItem> getItems() {
		return (Iterable) getProperty(itemsProperty);
	}

	@Override
	public String getCurrencyCode() {
		return getProperty(currencyProperty);
	}

	@Override
	public String getDescription() {
		return getProperty(descriptionProperty);
	}

	@Override
	public void setDescription(final String description) throws FrameworkException {
		setProperty(descriptionProperty, description);
	}

	@Override
	public String getBillingAddressName() {
		return getProperty(billingAddressNameProperty);
	}

	@Override
	public void setBillingAddressName(String billingAddressName) throws FrameworkException {
		setProperty(billingAddressNameProperty, billingAddressName);
	}

	@Override
	public String getBillingAddressStreet1() {
		return getProperty(billingAddressStreet1Property);
	}

	@Override
	public void setBillingAddressStreet1(String billingAddressStreet1) throws FrameworkException {
		setProperty(billingAddressStreet1Property, billingAddressStreet1);
	}

	@Override
	public String getBillingAddressStreet2() {
		return getProperty(billingAddressStreet2Property);
	}

	@Override
	public void setBillingAddressStreet2(String billingAddressStreet2) throws FrameworkException {
		setProperty(billingAddressStreet2Property, billingAddressStreet2);
	}

	@Override
	public String getBillingAddressZip() {
		return getProperty(billingAddressZipProperty);
	}

	@Override
	public void setBillingAddressZip(String billingAddressZip) throws FrameworkException {
		setProperty(billingAddressZipProperty, billingAddressZip);
	}

	@Override
	public String getBillingAddressCity() {
		return getProperty(billingAddressCityProperty);
	}

	@Override
	public void setBillingAddressCity(String billingAddressCity) throws FrameworkException {
		setProperty(billingAddressCityProperty, billingAddressCity);
	}

	@Override
	public String getBillingAddressCountry() {
		return getProperty(billingAddressCountryProperty);
	}

	@Override
	public void setBillingAddressCountry(String billingAddressCountry) throws FrameworkException {
		setProperty(billingAddressCountryProperty, billingAddressCountry);
	}

	@Override
	public String getPayer() {
		return getProperty(payerProperty);
	}

	@Override
	public void setPayer(String payer) throws FrameworkException {
		setProperty(payerProperty, payer);
	}

	@Override
	public String getPayerBusiness() {
		return getProperty(payerBusinessProperty);
	}

	@Override
	public void setPayerBusiness(String payerBusiness) throws FrameworkException {
		setProperty(payerBusinessProperty, payerBusiness);
	}

	@Override
	public String getPayerAddressName() {
		return getProperty(payerAddressNameProperty);
	}

	@Override
	public void setPayerAddressName(String payerAddressName) throws FrameworkException {
		setProperty(payerAddressNameProperty, payerAddressName);
	}

	@Override
	public String getPayerAddressStreet1() {
		return getProperty(payerAddressStreet1Property);
	}

	@Override
	public void setPayerAddressStreet1(String payerAddressStreet1) throws FrameworkException {
		setProperty(payerAddressStreet1Property, payerAddressStreet1);
	}

	@Override
	public String getPayerAddressStreet2() {
		return getProperty(payerAddressStreet2Property);
	}

	@Override
	public void setPayerAddressStreet2(String payerAddressStreet2) throws FrameworkException {
		setProperty(payerAddressStreet2Property, payerAddressStreet2);
	}

	@Override
	public String getPayerAddressZip() {
		return getProperty(payerAddressZipProperty);
	}

	@Override
	public void setPayerAddressZip(String payerAddressZip) throws FrameworkException {
		setProperty(payerAddressZipProperty, payerAddressZip);
	}

	@Override
	public String getPayerAddressCity() {
		return getProperty(payerAddressCityProperty);
	}

	@Override
	public void setPayerAddressCity(String payerAddressCity) throws FrameworkException {
		setProperty(payerAddressCityProperty, payerAddressCity);
	}

	@Override
	public String getPayerAddressCountry() {
		return getProperty(payerAddressCountryProperty);
	}

	@Override
	public void setPayerAddressCountry(String payerAddressCountry) throws FrameworkException {
		setProperty(payerAddressCountryProperty, payerAddressCountry);
	}

	@Override
	public String getBillingAgreementId() {
		return getProperty(billingAgreementIdProperty);
	}

	@Override
	public void setBillingAgreementId(String billingAgreementId) throws FrameworkException {
		setProperty(billingAgreementIdProperty, billingAgreementId);
	}

	@Override
	public String getNote() {
		return getProperty(noteProperty);
	}

	@Override
	public void setNote(String note) throws FrameworkException {
		setProperty(noteProperty, note);
	}

	@Override
	public String getInvoiceId() {
		return getProperty(invoiceIdProperty);
	}

	@Override
	public void setInvoiceId(final String invoiceId) throws FrameworkException {
		setProperty(invoiceIdProperty, invoiceId);
	}

	@Override
	public PaymentState getPaymentState() {
		return getProperty(stateProperty);
	}

	@Override
	public void setPaymentState(final PaymentState state) throws FrameworkException {
		setProperty(stateProperty, state);
	}

	@Override
	public String getToken() {
		return getProperty(tokenProperty);
	}

	@Override
	public void setToken(final String token) throws FrameworkException {
		setProperty(tokenProperty, token);
	}

	@Export
	public GraphObject beginCheckout(final String providerName, final String successUrl, final String cancelUrl) throws FrameworkException {

		final PaymentProvider provider = PaymentNode.getPaymentProvider(providerName);
		if (provider != null) {

			final BeginCheckoutResponse response = provider.beginCheckout(this, successUrl, cancelUrl);
			if (CheckoutState.Success.equals(response.getCheckoutState())) {

				final GraphObjectMap data = new GraphObjectMap();

				data.put(StructrApp.key(PaymentNode.class, "token"), response.getToken());

				return data;

			} else {

				throwErrors("Unable to begin checkout.", response);
			}

		} else {

			throw new FrameworkException(422, "Payment provider " + providerName + " not found.");
		}

		return null;
	}

	@Export
	public void cancelCheckout(final String providerName, final String token) throws FrameworkException {

		final PaymentProvider provider = PaymentNode.getPaymentProvider(providerName);
		if (provider != null) {

			provider.cancelCheckout(this);

		} else {

			throw new FrameworkException(422, "Payment provider " + providerName + " not found");
		}
	}

	@Export
	public GraphObject confirmCheckout(final String providerName, final String notifyUrl, final String token, final String payerId) throws FrameworkException {

		final PaymentProvider provider = PaymentNode.getPaymentProvider(providerName);
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

			errorBuffer.add(new PayPalErrorToken(PaymentNode.class.getSimpleName(), AbstractNode.base.jsonName(), error.getErrorCode(), error.getLongMessage()));
		}

		throw new FrameworkException(422, cause, errorBuffer);
	}
}
