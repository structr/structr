/**
 * Copyright (C) 2010-2020 Structr GmbH
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
package org.structr.payment.impl.paypal;

import java.util.ArrayList;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.structr.api.config.Settings;
import urn.ebay.api.PayPalAPI.DoExpressCheckoutPaymentReq;
import urn.ebay.api.PayPalAPI.DoExpressCheckoutPaymentRequestType;
import urn.ebay.api.PayPalAPI.DoExpressCheckoutPaymentResponseType;
import urn.ebay.api.PayPalAPI.GetExpressCheckoutDetailsReq;
import urn.ebay.api.PayPalAPI.GetExpressCheckoutDetailsRequestType;
import urn.ebay.api.PayPalAPI.GetExpressCheckoutDetailsResponseType;
import urn.ebay.api.PayPalAPI.PayPalAPIInterfaceServiceService;
import urn.ebay.api.PayPalAPI.SetExpressCheckoutReq;
import urn.ebay.api.PayPalAPI.SetExpressCheckoutRequestType;
import urn.ebay.api.PayPalAPI.SetExpressCheckoutResponseType;
import urn.ebay.apis.CoreComponentTypes.BasicAmountType;
import urn.ebay.apis.eBLBaseComponents.CurrencyCodeType;
import urn.ebay.apis.eBLBaseComponents.DoExpressCheckoutPaymentRequestDetailsType;
import urn.ebay.apis.eBLBaseComponents.PaymentActionCodeType;
import urn.ebay.apis.eBLBaseComponents.PaymentDetailsType;
import urn.ebay.apis.eBLBaseComponents.SetExpressCheckoutRequestDetailsType;

/**
 *
 */
public class PayPalHelper {

	public static SetExpressCheckoutResponseType getExpressCheckoutToken(final List<PaymentDetailsType> paymentDetails, final String successUrl, final String cancelUrl) throws Throwable {

		final SetExpressCheckoutRequestDetailsType setExpressCheckoutRequestDetails = new SetExpressCheckoutRequestDetailsType();

		setExpressCheckoutRequestDetails.setReturnURL(successUrl);
		setExpressCheckoutRequestDetails.setCancelURL(cancelUrl);

		setExpressCheckoutRequestDetails.setPaymentDetails(paymentDetails);

		final SetExpressCheckoutRequestType setExpressCheckoutRequest = new SetExpressCheckoutRequestType(setExpressCheckoutRequestDetails);
		setExpressCheckoutRequest.setVersion("104.0");

		final SetExpressCheckoutReq setExpressCheckoutReq = new SetExpressCheckoutReq();
		setExpressCheckoutReq.setSetExpressCheckoutRequest(setExpressCheckoutRequest);

		final PayPalAPIInterfaceServiceService service = new PayPalAPIInterfaceServiceService(getPayPalConfig());

		return service.setExpressCheckout(setExpressCheckoutReq);
	}

	public static GetExpressCheckoutDetailsResponseType getExpressCheckoutResponse(final String token) throws Throwable {

		final GetExpressCheckoutDetailsRequestType getExpressCheckoutDetailsRequest = new GetExpressCheckoutDetailsRequestType(token);
		getExpressCheckoutDetailsRequest.setVersion("104.0");

		final GetExpressCheckoutDetailsReq getExpressCheckoutDetailsReq = new GetExpressCheckoutDetailsReq();
		getExpressCheckoutDetailsReq.setGetExpressCheckoutDetailsRequest(getExpressCheckoutDetailsRequest);

		final PayPalAPIInterfaceServiceService service = new PayPalAPIInterfaceServiceService(getPayPalConfig());

		return service.getExpressCheckoutDetails(getExpressCheckoutDetailsReq);
	}

	public static DoExpressCheckoutPaymentResponseType commitExpressCheckout(final String notifyUrl, final CurrencyCodeType currencyCode, final int amountInCents, final String token, final String payerId) throws Throwable {

		final PaymentDetailsType paymentDetail = new PaymentDetailsType();
		paymentDetail.setNotifyURL(notifyUrl);
		paymentDetail.setOrderTotal(getAmountForCurrency(currencyCode.name(), amountInCents));
		paymentDetail.setPaymentAction(PaymentActionCodeType.SALE);

		final List<PaymentDetailsType> paymentDetails = new ArrayList<>();
		paymentDetails.add(paymentDetail);

		final DoExpressCheckoutPaymentRequestDetailsType doExpressCheckoutPaymentRequestDetails = new DoExpressCheckoutPaymentRequestDetailsType();
		doExpressCheckoutPaymentRequestDetails.setToken(token);
		doExpressCheckoutPaymentRequestDetails.setPayerID(payerId);
		doExpressCheckoutPaymentRequestDetails.setPaymentDetails(paymentDetails);

		final DoExpressCheckoutPaymentRequestType doExpressCheckoutPaymentRequest = new DoExpressCheckoutPaymentRequestType(doExpressCheckoutPaymentRequestDetails);
		doExpressCheckoutPaymentRequest.setVersion("104.0");

		final DoExpressCheckoutPaymentReq doExpressCheckoutPaymentReq = new DoExpressCheckoutPaymentReq();
		doExpressCheckoutPaymentReq.setDoExpressCheckoutPaymentRequest(doExpressCheckoutPaymentRequest);

		final PayPalAPIInterfaceServiceService service = new PayPalAPIInterfaceServiceService(getPayPalConfig());
		return service.doExpressCheckoutPayment(doExpressCheckoutPaymentReq);
	}

	// ----- private methods -----
	public static BasicAmountType getAmountForCurrency(final String currencyCodeName, final int amountInCents) {

		// We use the smallest existing currency unit for each currency,
		// so we need to calculate a divisor that can be used to transform
		// the value into the correct base used by PayPal..

		final Currency currency                          = Currency.getInstance(currencyCodeName);
		final CurrencyCodeType currencyCode              = CurrencyCodeType.fromValue(currency.getCurrencyCode());
		final double divisor                             = Math.pow(10, currency.getDefaultFractionDigits());
		final double calculatedAmount                    = ((double)amountInCents / divisor);

		return new BasicAmountType(currencyCode, Double.toString(calculatedAmount));

	}

	private static Map<String, String> getPayPalConfig() {

		final Map<String, String> config = new LinkedHashMap<>();

		config.put("mode",            Settings.getOrCreateStringSetting("paypal", "mode").getValue());
		config.put("acct1.UserName",  Settings.getOrCreateStringSetting("paypal", "username").getValue());
		config.put("acct1.Password",  Settings.getOrCreateStringSetting("paypal", "password").getValue());
		config.put("acct1.Signature", Settings.getOrCreateStringSetting("paypal", "signature").getValue());

		return config;

	}
}
