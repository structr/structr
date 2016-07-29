package org.structr.payment.impl.paypal;

import org.structr.payment.api.BeginCheckoutResponse;
import urn.ebay.apis.eBLBaseComponents.AbstractResponseType;

/**
 *
 */
public class PayPalBeginCheckoutResponse extends PayPalResponse implements BeginCheckoutResponse {

	private String token = null;

	public PayPalBeginCheckoutResponse(final AbstractResponseType response, final String token) {

		super(response);

		this.token = token;
	}

	@Override
	public String getToken() {
		return token;
	}
}
