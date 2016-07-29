package org.structr.payment.impl.paypal;

import org.structr.payment.api.APIError;

/**
 *
 */
public class PayPalError implements APIError {

	private String shortMessage = null;
	private String longMessage  = null;
	private String code         = null;

	public PayPalError(final String errorCode, final String shortMessage, final String longMessage) {

		this.code         = errorCode;
		this.shortMessage = shortMessage;
		this.longMessage  = longMessage;
	}

	@Override
	public String getErrorCode() {
		return code;
	}

	@Override
	public String getShortMessage() {
		return shortMessage;
	}

	@Override
	public String getLongMessage() {
		return longMessage;
	}
}
