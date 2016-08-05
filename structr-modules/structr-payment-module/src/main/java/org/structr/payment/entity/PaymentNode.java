/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.payment.entity;

import java.util.LinkedList;
import java.util.List;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.schema.SchemaService;
import org.structr.core.Export;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.EndNodes;
import org.structr.core.property.EnumProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.payment.api.APIError;
import org.structr.payment.api.APIResponse;
import org.structr.payment.api.Payment;
import org.structr.payment.api.PaymentItem;
import org.structr.payment.api.PaymentProvider;
import org.structr.payment.api.PaymentState;
import org.structr.payment.api.BeginCheckoutResponse;
import org.structr.payment.api.CheckoutState;
import org.structr.payment.api.ConfirmCheckoutResponse;
import org.structr.payment.impl.paypal.PayPalErrorToken;
import org.structr.payment.impl.paypal.PayPalPaymentProvider;
import org.structr.payment.impl.stripe.StripePaymentProvider;

/**
 *
 * @author Christian Morgner
 */
public class PaymentNode extends AbstractNode implements Payment {

	static {

		SchemaService.registerBuiltinTypeOverride("PaymentNode", PaymentNode.class.getName());
	}

	public static final Property<List<PaymentItemNode>> items                   = new EndNodes<>("items", PaymentItems.class);
	public static final Property<PaymentState>          stateProperty           = new EnumProperty("state", PaymentState.class).indexed();
	public static final Property<String>                descriptionProperty     = new StringProperty("description").indexed();
	public static final Property<String>                currencyProperty        = new StringProperty("currency").indexed();
	public static final Property<String>                tokenProperty           = new StringProperty("token").indexed();
	public static final Property<String>                billingAgreementId      = new StringProperty("billingAgreementId");
	public static final Property<String>                note                    = new StringProperty("note");
	public static final Property<String>                billingAddressName      = new StringProperty("billingAddressName");
	public static final Property<String>                billingAddressStreet1   = new StringProperty("billingAddressStreet1");
	public static final Property<String>                billingAddressStreet2   = new StringProperty("billingAddressStreet2");
	public static final Property<String>                billingAddressZip       = new StringProperty("billingAddressZip");
	public static final Property<String>                billingAddressCity      = new StringProperty("billingAddressCity");
	public static final Property<String>                billingAddressCountry   = new StringProperty("billingAddressCountry");
	public static final Property<String>                invoiceId               = new StringProperty("invoiceId");
	public static final Property<String>                payerAddressName        = new StringProperty("payerAddressName");
	public static final Property<String>                payerAddressStreet1     = new StringProperty("payerAddressStreet1");
	public static final Property<String>                payerAddressStreet2     = new StringProperty("payerAddressStreet2");
	public static final Property<String>                payerAddressZip         = new StringProperty("payerAddressZip");
	public static final Property<String>                payerAddressCity        = new StringProperty("payerAddressCity");
	public static final Property<String>                payerAddressCountry     = new StringProperty("payerAddressCountry");
	public static final Property<String>                payer                   = new StringProperty("payer");
	public static final Property<String>                payerBusiness           = new StringProperty("payerBusiness");

	public static final View publicView = new View(PaymentNode.class, "public",
		descriptionProperty, items, currencyProperty, tokenProperty, stateProperty, billingAgreementId, note, billingAddressName,
		billingAddressStreet1, billingAddressStreet2, billingAddressZip, billingAddressCity, billingAddressCountry, invoiceId,
		payerAddressName, payerAddressStreet1, payerAddressStreet2, payerAddressZip, payerAddressCity, payerAddressCountry, payer, payerBusiness
	);

	public static final View uiView = new View(PaymentNode.class, "ui",
		descriptionProperty, items, currencyProperty, tokenProperty, stateProperty, billingAgreementId, note, billingAddressName,
		billingAddressStreet1, billingAddressStreet2, billingAddressZip, billingAddressCity, billingAddressCountry, invoiceId,
		payerAddressName, payerAddressStreet1, payerAddressStreet2, payerAddressZip, payerAddressCity, payerAddressCountry, payer, payerBusiness
	);

	@Export
	public GraphObject beginCheckout(final String providerName, final String successUrl, final String cancelUrl) throws FrameworkException {

		final PaymentProvider provider = getPaymentProvider(providerName);
		if (provider != null) {

			final BeginCheckoutResponse response = provider.beginCheckout(this, successUrl, cancelUrl);
			if (CheckoutState.Success.equals(response.getCheckoutState())) {

				final GraphObjectMap data  = new GraphObjectMap();

				data.put(tokenProperty, response.getToken());

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

		final PaymentProvider provider = getPaymentProvider(providerName);
		if (provider != null) {

			provider.cancelCheckout(this);

		} else {

			throw new FrameworkException(422, "Payment provider " + providerName + " not found.");
		}
	}

	@Export
	public GraphObject confirmCheckout(final String providerName, final String notifyUrl, final String token, final String payerId) throws FrameworkException {

		final PaymentProvider provider = getPaymentProvider(providerName);
		if (provider != null) {

			final ConfirmCheckoutResponse response = provider.confirmCheckout(this, notifyUrl, token, payerId);
			if (CheckoutState.Success.equals(response.getCheckoutState())) {

				// no return value neccessary, will result in code 200
				return null;

			} else {

				throwErrors("Unable to confirm checkout.", response);
			}

		} else {

			throw new FrameworkException(422, "Payment provider " + providerName + " not found.");
		}

		return null;
	}

	// ----- interface Payment -----
	@Override
	public List<PaymentItem> getItems() {
		return new LinkedList<>(getProperty(items));
	}

	@Override
	public int getTotal() {

		int total = 0;

		for (final PaymentItem item : getItems()) {
			total += item.getAmount() * item.getQuantity();
		}

		return total;
	}

	@Override
	public String getCurrencyCode() {
		return getProperty(currencyProperty);
	}

	@Override
	public String getToken() {
		return getProperty(tokenProperty);
	}

	@Override
	public void setToken(String token) throws FrameworkException {
		setProperty(tokenProperty, token);
	}

	// ----- private methods -----
	private PaymentProvider getPaymentProvider(final String providerName) {

		switch (providerName) {

			case "paypal":
				return new PayPalPaymentProvider();

			case "stripe":
				return new StripePaymentProvider();
		}

		return null;
	}

	private void throwErrors(final String cause, final APIResponse response) throws FrameworkException {

		final ErrorBuffer errorBuffer = new ErrorBuffer();

		for (final APIError error : response.getErrors()) {

			errorBuffer.add(new PayPalErrorToken(PaymentNode.class.getSimpleName(), AbstractNode.base, error.getErrorCode(), error.getLongMessage()));
		}

		throw new FrameworkException(422, cause, errorBuffer);
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
	public PaymentState getPaymentState() {
		return getProperty(stateProperty);
	}

	@Override
	public void setPaymentState(final PaymentState state) throws FrameworkException {
		setProperty(stateProperty, state);
	}

	@Override
	public String getBillingAddressName() {
		return getProperty(billingAddressName);
	}

	@Override
	public void setBillingAddressName(final String billingAddressName) throws FrameworkException {
		setProperty(PaymentNode.billingAddressName, billingAddressName);
	}

	@Override
	public String getBillingAddressStreet1() {
		return getProperty(PaymentNode.billingAddressStreet1);
	}

	@Override
	public void setBillingAddressStreet1(final String billingAddressStreet1) throws FrameworkException {
		setProperty(PaymentNode.billingAddressStreet1, billingAddressStreet1);
	}

	@Override
	public String getBillingAddressStreet2() {
		return getProperty(PaymentNode.billingAddressStreet2);
	}

	@Override
	public void setBillingAddressStreet2(final String billingAddressStreet2) throws FrameworkException {
			setProperty(PaymentNode.billingAddressStreet2, billingAddressStreet2);
	}

	@Override
	public String getBillingAddressZip() {
		return getProperty(PaymentNode.billingAddressZip);
	}

	@Override
	public void setBillingAddressZip(final String billingAddressZip) throws FrameworkException {
		setProperty(PaymentNode.billingAddressZip, billingAddressZip);
	}

	@Override
	public String getBillingAddressCity() {
		return getProperty(PaymentNode.billingAddressCity);
	}

	@Override
	public void setBillingAddressCity(final String billingAddressCity) throws FrameworkException {
		setProperty(PaymentNode.billingAddressCity, billingAddressCity);
	}

	@Override
	public String getBillingAddressCountry() {
		return getProperty(PaymentNode.billingAddressCountry);
	}

	@Override
	public void setBillingAddressCountry(final String billingAddressCountry) throws FrameworkException {
		setProperty(PaymentNode.billingAddressCountry, billingAddressCountry);
	}

	@Override
	public String getPayer() {
		return getProperty(PaymentNode.payer);
	}

	@Override
	public void setPayer(final String payer) throws FrameworkException {
			setProperty(PaymentNode.payer, payer);
	}

	@Override
	public String getPayerBusiness() {
		return getProperty(PaymentNode.payerBusiness);
	}

	@Override
	public void setPayerBusiness(final String payerBusiness) throws FrameworkException {
		setProperty(PaymentNode.payerBusiness, payerBusiness);
	}

	@Override
	public String getPayerAddressName() {
		return getProperty(PaymentNode.payerAddressName);
	}

	@Override
	public void setPayerAddressName(final String payerAddressName) throws FrameworkException {
			setProperty(PaymentNode.payerAddressName, payerAddressName);
	}

	@Override
	public String getPayerAddressStreet1() {
		return getProperty(PaymentNode.payerAddressStreet1);
	}

	@Override
	public void setPayerAddressStreet1(final String payerAddressStreet1) throws FrameworkException {
			setProperty(PaymentNode.payerAddressStreet1, payerAddressStreet1);
	}

	@Override
	public String getPayerAddressStreet2() {
		return getProperty(PaymentNode.payerAddressStreet2);
	}

	@Override
	public void setPayerAddressStreet2(final String payerAddressStreet2) throws FrameworkException {
			setProperty(PaymentNode.payerAddressStreet2, payerAddressStreet2);
	}

	@Override
	public String getPayerAddressZip() {
		return getProperty(PaymentNode.payerAddressZip);
	}

	@Override
	public void setPayerAddressZip(final String payerAddressZip) throws FrameworkException {
			setProperty(PaymentNode.payerAddressZip, payerAddressZip);
	}

	@Override
	public String getPayerAddressCity() {
		return getProperty(PaymentNode.payerAddressCity);
	}

	@Override
	public void setPayerAddressCity(final String payerAddressCity) throws FrameworkException {
			setProperty(PaymentNode.payerAddressCity, payerAddressCity);
	}

	@Override
	public String getPayerAddressCountry() {
		return getProperty(PaymentNode.payerAddressCountry);
	}

	@Override
	public void setPayerAddressCountry(final String payerAddressCountry) throws FrameworkException {
		setProperty(PaymentNode.payerAddressCountry, payerAddressCountry);
	}

	@Override
	public String getBillingAgreementId() {
		return getProperty(PaymentNode.billingAgreementId);
	}

	@Override
	public void setBillingAgreementId(final String billingAgreementId) throws FrameworkException {
		setProperty(PaymentNode.billingAgreementId, billingAgreementId);
	}

	@Override
	public String getNote() {
		return getProperty(PaymentNode.note);
	}

	@Override
	public void setNote(final String note) throws FrameworkException {
		setProperty(PaymentNode.note, note);
	}

	@Override
	public String getInvoiceId() {
		return getProperty(PaymentNode.invoiceId);
	}

	@Override
	public void setInvoiceId(final String invoiceId) throws FrameworkException {
		setProperty(PaymentNode.invoiceId, invoiceId);
	}
}
