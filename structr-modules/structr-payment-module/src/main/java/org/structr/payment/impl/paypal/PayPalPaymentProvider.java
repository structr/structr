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
package org.structr.payment.impl.paypal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.payment.api.*;
import urn.ebay.api.PayPalAPI.DoExpressCheckoutPaymentResponseType;
import urn.ebay.api.PayPalAPI.GetExpressCheckoutDetailsResponseType;
import urn.ebay.api.PayPalAPI.SetExpressCheckoutResponseType;
import urn.ebay.apis.eBLBaseComponents.*;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class PayPalPaymentProvider implements PaymentProvider {

	private static final Logger logger = LoggerFactory.getLogger(PayPalPaymentProvider.class.getName());

	@Override
	public BeginCheckoutResponse beginCheckout(final Payment payment, final String successUrl, final String cancelUrl) throws FrameworkException {

		final List<PaymentDetailsType> paymentDetailList = new ArrayList<>();
		final List<PaymentDetailsItemType> lineItems     = new ArrayList<>();
		final PaymentDetailsType paymentDetails          = new PaymentDetailsType();

		for (final PaymentItem item : payment.getItems()) {

			// create payment item
			final PaymentDetailsItemType paymentDetailsItem = new PaymentDetailsItemType();

			paymentDetailsItem.setAmount(PayPalHelper.getAmountForCurrency(payment.getCurrencyCode(), item.getAmount()));
			paymentDetailsItem.setQuantity(item.getQuantity());

			final String name = item.getName();
			if (name != null) {

				paymentDetailsItem.setName(name);
			}

			final String description = item.getDescription();
			if (description != null) {

				paymentDetailsItem.setDescription(description);
			}

			final String itemUrl = item.getItemUrl();
			if (itemUrl != null) {

				paymentDetailsItem.setItemURL(itemUrl);
			}

			final String itemNumber = item.getItemNumber();
			if (itemNumber != null) {

				paymentDetailsItem.setNumber(itemNumber);
			}

			lineItems.add(paymentDetailsItem);
		}

		paymentDetails.setPaymentAction(PaymentActionCodeType.SALE);
		paymentDetails.setPaymentDetailsItem(lineItems);
		paymentDetails.setOrderTotal(PayPalHelper.getAmountForCurrency(payment.getCurrencyCode(), payment.getTotal()));

		paymentDetailList.add(paymentDetails);

		try {
			final SetExpressCheckoutResponseType response = PayPalHelper.getExpressCheckoutToken(paymentDetailList, successUrl, cancelUrl);
			if (AckCodeType.SUCCESS.equals(response.getAck())) {

				payment.setToken(response.getToken());
				payment.setPaymentState(PaymentState.open);
				return new PayPalBeginCheckoutResponse(response, response.getToken());
			}

		} catch (Throwable t) {

			throw new FrameworkException(422, t.getMessage());
		}

		throw new FrameworkException(422, "Unknown error.");
	}

	@Override
	public void cancelCheckout(final Payment payment) throws FrameworkException {

		// this is a no-op in the PayPal API, we only have to set the payment state
		try {

			payment.setToken(null);
			payment.setPaymentState(PaymentState.cancelled);

		} catch (FrameworkException fex) {
			logger.warn("", fex);
		}
	}

	@Override
	public ConfirmCheckoutResponse confirmCheckout(final Payment payment, final String notifyUrl, final String token, final String payerId) throws FrameworkException {

		try {

			final GetExpressCheckoutDetailsResponseType response = PayPalHelper.getExpressCheckoutResponse(token);

			if (AckCodeType.SUCCESS.equals(response.getAck())) {

				// TODO: change currency code
				final GetExpressCheckoutDetailsResponseDetailsType details      = response.getGetExpressCheckoutDetailsResponseDetails();
				final CurrencyCodeType currencyCode                             = CurrencyCodeType.fromValue(payment.getCurrencyCode());

					final DoExpressCheckoutPaymentResponseType confirmationResponse = PayPalHelper.commitExpressCheckout(
						notifyUrl,
						currencyCode,
						payment.getTotal(),
						token,
						payerId
					);


					if (AckCodeType.SUCCESS.equals(confirmationResponse.getAck())) {

						final PayPalConfirmCheckoutResponse checkoutResponse           = new PayPalConfirmCheckoutResponse(confirmationResponse);
						final DoExpressCheckoutPaymentResponseDetailsType confirmation = confirmationResponse.getDoExpressCheckoutPaymentResponseDetails();
						final String billingAgreementId                                = confirmation.getBillingAgreementID();
						final String note                                              = confirmation.getNote();

						// billing address
						final AddressType billingAddress = details.getBillingAddress();
						if (billingAddress != null) {

							final String billingAddressName    = billingAddress.getName();
							final String billingAddressStreet1 = billingAddress.getStreet1();
							final String billingAddressStreet2 = billingAddress.getStreet2();
							final String billingAddressZip     = billingAddress.getPostalCode();
							final String billingAddressCity    = billingAddress.getCityName();
							final String billingAddressCountry = billingAddress.getCountryName();

							payment.setBillingAddressName(billingAddressName);
							payment.setBillingAddressStreet1(billingAddressStreet1);
							payment.setBillingAddressStreet2(billingAddressStreet2);
							payment.setBillingAddressZip(billingAddressZip);
							payment.setBillingAddressCity(billingAddressCity);
							payment.setBillingAddressCountry(billingAddressCountry);
						}

						// payer info
						final PayerInfoType payerInfo = details.getPayerInfo();
						if (payerInfo != null) {

							payment.setPayer(payerInfo.getPayer());
							payment.setPayerBusiness(payerInfo.getPayerBusiness());

							final AddressType payerAddress = payerInfo.getAddress();
							if (payerAddress != null) {

								payment.setPayerAddressName(payerAddress.getName());
								payment.setPayerAddressStreet1(payerAddress.getStreet1());
								payment.setPayerAddressStreet2(payerAddress.getStreet1());
								payment.setPayerAddressZip(payerAddress.getPostalCode());
								payment.setPayerAddressCity(payerAddress.getCityName());
								payment.setPayerAddressCountry(payerAddress.getCountryName());
							}
						}

						payment.setBillingAgreementId(billingAgreementId);
						payment.setNote(note);
						payment.setInvoiceId(details.getInvoiceID());
						payment.setPaymentState(PaymentState.completed);
						payment.setToken(null);

						// success
						return checkoutResponse;
					}
			}

		} catch (Throwable t) {
			throw new FrameworkException(422, t.getMessage());
		}

		throw new FrameworkException(422, "Unknown error.");
	}
}
