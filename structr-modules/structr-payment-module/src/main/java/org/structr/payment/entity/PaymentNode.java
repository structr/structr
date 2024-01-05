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

import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeInterface;
import org.structr.payment.api.*;
import org.structr.payment.impl.paypal.PayPalErrorToken;
import org.structr.payment.impl.paypal.PayPalPaymentProvider;
import org.structr.payment.impl.stripe.StripePaymentProvider;
import org.structr.payment.impl.test.TestPaymentProvider;
import org.structr.schema.SchemaService;

import java.net.URI;

/**
 *
 */
public interface PaymentNode extends NodeInterface, Payment {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("PaymentNode");
		final JsonObjectType item = schema.addType("PaymentItemNode");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/PaymentNode"));

		type.addEnumProperty("state",                    PropertyView.Public, PropertyView.Ui).setEnumType(PaymentState.class);
		type.addStringProperty("description",            PropertyView.Public, PropertyView.Ui).setIndexed(true);
		type.addStringProperty("currency",               PropertyView.Public, PropertyView.Ui).setIndexed(true);
		type.addStringProperty("token",                  PropertyView.Public, PropertyView.Ui).setIndexed(true);
		type.addStringProperty("billingAgreementId",     PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("note",                   PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("billingAddressName",     PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("billingAddressStreet1",  PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("billingAddressStreet2",  PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("billingAddressZip",      PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("billingAddressCity",     PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("billingAddressCountry",  PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("invoiceId",              PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("payerAddressName",       PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("payerAddressStreet1",    PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("payerAddressStreet2",    PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("payerAddressZip",        PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("payerAddressCity",       PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("payerAddressCountry",    PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("payer",                  PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("payerBusiness",          PropertyView.Public, PropertyView.Ui);

		type.addPropertyGetter("items",                  Iterable.class);
		type.addPropertyGetter("description",            String.class);
		type.addPropertyGetter("currency",               String.class);
		type.addPropertyGetter("token",                  String.class);
		type.addPropertyGetter("billingAgreementId",     String.class);
		type.addPropertyGetter("note",                   String.class);
		type.addPropertyGetter("billingAddressName",     String.class);
		type.addPropertyGetter("billingAddressStreet1",  String.class);
		type.addPropertyGetter("billingAddressStreet2",  String.class);
		type.addPropertyGetter("billingAddressZip",      String.class);
		type.addPropertyGetter("billingAddressCity",     String.class);
		type.addPropertyGetter("billingAddressCountry",  String.class);
		type.addPropertyGetter("invoiceId",              String.class);
		type.addPropertyGetter("payerAddressName",       String.class);
		type.addPropertyGetter("payerAddressStreet1",    String.class);
		type.addPropertyGetter("payerAddressStreet2",    String.class);
		type.addPropertyGetter("payerAddressZip",        String.class);
		type.addPropertyGetter("payerAddressCity",       String.class);
		type.addPropertyGetter("payerAddressCountry",    String.class);
		type.addPropertyGetter("payer",                  String.class);
		type.addPropertyGetter("payerBusiness",          String.class);

		type.addPropertySetter("description",            String.class);
		type.addPropertySetter("currency",               String.class);
		type.addPropertySetter("token",                  String.class);
		type.addPropertySetter("billingAgreementId",     String.class);
		type.addPropertySetter("note",                   String.class);
		type.addPropertySetter("billingAddressName",     String.class);
		type.addPropertySetter("billingAddressStreet1",  String.class);
		type.addPropertySetter("billingAddressStreet2",  String.class);
		type.addPropertySetter("billingAddressZip",      String.class);
		type.addPropertySetter("billingAddressCity",     String.class);
		type.addPropertySetter("billingAddressCountry",  String.class);
		type.addPropertySetter("invoiceId",              String.class);
		type.addPropertySetter("payerAddressName",       String.class);
		type.addPropertySetter("payerAddressStreet1",    String.class);
		type.addPropertySetter("payerAddressStreet2",    String.class);
		type.addPropertySetter("payerAddressZip",        String.class);
		type.addPropertySetter("payerAddressCity",       String.class);
		type.addPropertySetter("payerAddressCountry",    String.class);
		type.addPropertySetter("payer",                  String.class);
		type.addPropertySetter("payerBusiness",          String.class);

		type.addMethod("beginCheckout")
			.setReturnType(GraphObject.class.getName())
			.addParameter("ctx", SecurityContext.class.getName())
			.addParameter("arg0", String.class.getName())
			.addParameter("arg1", String.class.getName())
			.addParameter("arg2", String.class.getName())
			.setSource("return " + PaymentNode.class.getName() + ".beginCheckout(this, arg0, arg1, arg2);")
			.addException(FrameworkException.class.getName())
			.setDoExport(true);

		type.addMethod("cancelCheckout")
			.addParameter("ctx", SecurityContext.class.getName())
			.addParameter("arg0", String.class.getName())
			.addParameter("arg1", String.class.getName())
			.setSource(PaymentNode.class.getName() + ".cancelCheckout(this, arg0, arg1);")
			.addException(FrameworkException.class.getName())
			.setDoExport(true);

		type.addMethod("confirmCheckout")
			.setReturnType(GraphObject.class.getName())
			.addParameter("ctx", SecurityContext.class.getName())
			.addParameter("arg0", String.class.getName())
			.addParameter("arg1", String.class.getName())
			.addParameter("arg2", String.class.getName())
			.addParameter("arg3", String.class.getName())
			.setSource("return " + PaymentNode.class.getName() + ".confirmCheckout(this, arg0, arg1, arg2, arg3);")
			.addException(FrameworkException.class.getName())
			.setDoExport(true);

		type.addMethod("getTotal").setSource("return " + PaymentNode.class.getName() + ".getTotal(this);").setReturnType("int");
		type.addMethod("getCurrencyCode").setSource("return getProperty(currencyProperty);").setReturnType(String.class.getName());
		type.addMethod("getPaymentState").setSource("return getProperty(stateProperty);").setReturnType(PaymentState.class.getName());

		type.addMethod("setPaymentState")
			.addParameter("state", PaymentState.class.getName())
			.setSource("setProperty(stateProperty, state);")
			.addException(FrameworkException.class.getName());

		type.relate(item, "paymentItem", Cardinality.OneToMany, "payment", "items");

		type.addViewProperty(PropertyView.Public, "items");
		type.addViewProperty(PropertyView.Ui, "items");
	}}

	public static GraphObject beginCheckout(final PaymentNode thisNode, final String providerName, final String successUrl, final String cancelUrl) throws FrameworkException {

		final PaymentProvider provider = PaymentNode.getPaymentProvider(providerName);
		if (provider != null) {

			final BeginCheckoutResponse response = provider.beginCheckout(thisNode, successUrl, cancelUrl);
			if (CheckoutState.Success.equals(response.getCheckoutState())) {

				final GraphObjectMap data  = new GraphObjectMap();

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

	public static void cancelCheckout(final PaymentNode thisNode, final String providerName, final String token) throws FrameworkException {

		final PaymentProvider provider = PaymentNode.getPaymentProvider(providerName);
		if (provider != null) {

			provider.cancelCheckout(thisNode);

		} else {

			throw new FrameworkException(422, "Payment provider " + providerName + " not found");
		}
	}

	public static GraphObject confirmCheckout(final PaymentNode thisNode, final String providerName, final String notifyUrl, final String token, final String payerId) throws FrameworkException {

		final PaymentProvider provider = PaymentNode.getPaymentProvider(providerName);
		if (provider != null) {

			final ConfirmCheckoutResponse response = provider.confirmCheckout(thisNode, notifyUrl, token, payerId);
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
	public static int getTotal(final PaymentNode thisNode) {

		int total = 0;

		for (final PaymentItem item : thisNode.getItems()) {
			total += item.getAmount() * item.getQuantity();
		}

		return total;
	}

	static PaymentProvider getPaymentProvider(final String providerName) {

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

	static void throwErrors(final String cause, final APIResponse response) throws FrameworkException {

		final ErrorBuffer errorBuffer = new ErrorBuffer();

		for (final APIError error : response.getErrors()) {

			errorBuffer.add(new PayPalErrorToken(PaymentNode.class.getSimpleName(), AbstractNode.base, error.getErrorCode(), error.getLongMessage()));
		}

		throw new FrameworkException(422, cause, errorBuffer);
	}
}
