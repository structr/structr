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
package org.structr.payment.impl.stripe;

import com.stripe.Stripe;
import com.stripe.exception.*;
import com.stripe.model.Charge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.payment.api.*;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class StripePaymentProvider implements PaymentProvider {

	private static final Logger logger = LoggerFactory.getLogger(StripePaymentProvider.class.getName());

	@Override
	public BeginCheckoutResponse beginCheckout(final Payment payment, final String successUrl, final String cancelUrl) throws FrameworkException {
		throw new FrameworkException(422, "Begin checkout not supported by this payment provider. Please use the confirmCheckout endpoint.");
	}

	@Override
	public ConfirmCheckoutResponse confirmCheckout(final Payment payment, final String notifyUrl, final String token, final String payerId) throws FrameworkException {

		Stripe.apiKey = Settings.getOrCreateStringSetting("stripe", "apikey").getValue();

		// Create the charge on Stripe's servers - this will charge the user's card
		try {

			final Map<String, Object> chargeParams = new HashMap<>();

			chargeParams.put("amount", payment.getTotal());
			chargeParams.put("currency", payment.getCurrencyCode());
			chargeParams.put("source", token);
			chargeParams.put("description", payment.getDescription());

			Charge.create(chargeParams);

			payment.setPaymentState(PaymentState.completed);

			return new ConfirmResponse(CheckoutState.Success);

		} catch (APIException ex) {

			payment.setPaymentState(PaymentState.error);

			return new ConfirmResponse(CheckoutState.Failure, "1", "APIException", ex.getMessage());

		} catch (APIConnectionException ex) {

			payment.setPaymentState(PaymentState.error);

			return new ConfirmResponse(CheckoutState.Failure, "1", "APIConnectionException", ex.getMessage());

		} catch (InvalidRequestException ex) {

			payment.setPaymentState(PaymentState.error);

			return new ConfirmResponse(CheckoutState.Failure, "1", "InvalidRequestException", ex.getMessage());

		} catch (AuthenticationException ex) {

			payment.setPaymentState(PaymentState.error);

			return new ConfirmResponse(CheckoutState.Failure, "1", "AuthenticationException", ex.getMessage());

		} catch (CardException e) {

			payment.setPaymentState(PaymentState.error);

			return new ConfirmResponse(CheckoutState.Failure, e.getCode(), e.getCharge(), e.getMessage());
		}
	}

	@Override
	public void cancelCheckout(final Payment payment) throws FrameworkException {

		// we only have to set the payment state
		try {

			payment.setToken(null);
			payment.setPaymentState(PaymentState.cancelled);

		} catch (FrameworkException fex) {
			logger.warn("", fex);
		}
	}

	private static class ConfirmResponse implements ConfirmCheckoutResponse {

		private final List<APIError> errors = new ArrayList<>();
		private CheckoutState state         = null;

		public ConfirmResponse(final CheckoutState state) {
			this(state, null, null, null);
		}

		public ConfirmResponse(final CheckoutState state, final String errorCode, final String shortMessage, final String longMessage) {

			this.state = state;

			if (errorCode != null && shortMessage != null && longMessage != null) {
				this.errors.add(new APIErrorImpl(errorCode, shortMessage, longMessage));
			}
		}

		@Override
		public CheckoutState getCheckoutState() {
			return state;
		}

		@Override
		public List<APIError> getErrors() {
			return errors;
		}
	}

	private static class APIErrorImpl implements APIError {

		private String errorCode    = null;
		private String shortMessage = null;
		private String longMessage  = null;

		public APIErrorImpl(final String errorCode, final String shortMessage, final String longMessage) {

			this.errorCode     = errorCode;
			this.shortMessage  = shortMessage;
			this.longMessage   = longMessage;
		}

		@Override
		public String getShortMessage() {
			return shortMessage;
		}

		@Override
		public String getLongMessage() {
			return longMessage;
		}

		@Override
		public String getErrorCode() {
			return errorCode;
		}
	}
}
