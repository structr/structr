package org.structr.payment.impl.paypal;

import org.structr.payment.api.ConfirmCheckoutResponse;
import urn.ebay.apis.eBLBaseComponents.AbstractResponseType;

/**
 *
 */
public class PayPalConfirmCheckoutResponse extends PayPalResponse implements ConfirmCheckoutResponse {

	public PayPalConfirmCheckoutResponse(final AbstractResponseType response) {
		super(response);
	}
}
