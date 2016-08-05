/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.payment.impl.paypal;

import java.util.LinkedList;
import java.util.List;
import org.structr.payment.api.APIError;
import org.structr.payment.api.APIResponse;
import org.structr.payment.api.CheckoutState;
import urn.ebay.apis.eBLBaseComponents.AbstractResponseType;
import urn.ebay.apis.eBLBaseComponents.AckCodeType;
import static urn.ebay.apis.eBLBaseComponents.AckCodeType.FAILURE;
import static urn.ebay.apis.eBLBaseComponents.AckCodeType.FAILUREWITHWARNING;
import static urn.ebay.apis.eBLBaseComponents.AckCodeType.PARTIALSUCCESS;
import static urn.ebay.apis.eBLBaseComponents.AckCodeType.SUCCESS;
import static urn.ebay.apis.eBLBaseComponents.AckCodeType.SUCCESSWITHWARNING;
import static urn.ebay.apis.eBLBaseComponents.AckCodeType.WARNING;
import urn.ebay.apis.eBLBaseComponents.ErrorType;

/**
 *
 * @author Christian Morgner
 */
public abstract class PayPalResponse implements APIResponse {

	private final List<APIError> errors = new LinkedList<>();
	private CheckoutState checkoutState = null;

	public PayPalResponse(final AbstractResponseType response) {

		this.checkoutState = getCheckoutState(response.getAck());

		for (final ErrorType err : response.getErrors()) {
			this.errors.add(new PayPalError(err.getErrorCode(), err.getShortMessage(), err.getLongMessage()));
		}
	}

	@Override
	public final CheckoutState getCheckoutState() {
		return checkoutState;
	}

	@Override
	public final List<APIError> getErrors() {
		return errors;
	}

	// ----- private methods -----
	private CheckoutState getCheckoutState(final AckCodeType ack) {

		switch (ack) {

			case CUSTOMCODE:
				return CheckoutState.Custom;

			case FAILURE:
				return CheckoutState.Failure;

			case FAILUREWITHWARNING:
				return CheckoutState.FailureWithWarning;

			case PARTIALSUCCESS:
				return CheckoutState.PartialSuccess;

			case SUCCESS:
				return CheckoutState.Success;

			case SUCCESSWITHWARNING:
				return CheckoutState.SuccessWithWarning;

			case WARNING:
				return CheckoutState.Warning;
		}

		return null;
	}

}
