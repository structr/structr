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

import org.structr.payment.api.APIError;
import org.structr.payment.api.APIResponse;
import org.structr.payment.api.CheckoutState;
import urn.ebay.apis.eBLBaseComponents.AbstractResponseType;
import urn.ebay.apis.eBLBaseComponents.AckCodeType;
import urn.ebay.apis.eBLBaseComponents.ErrorType;

import java.util.LinkedList;
import java.util.List;


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
