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
package org.structr.payment.impl.test;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.payment.api.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public class TestPaymentProvider implements PaymentProvider {

	private static final Logger logger = LoggerFactory.getLogger(TestPaymentProvider.class.getName());

	@Override
	public BeginCheckoutResponse beginCheckout(final Payment payment, final String successUrl, final String cancelUrl) throws FrameworkException {

		final String token = RandomStringUtils.randomAlphabetic(32);

		payment.setPaymentState(PaymentState.open);
		payment.setToken(token);

		return new BeginCheckoutResponse() {
			@Override
			public String getToken() {
				return token;
			}

			@Override
			public CheckoutState getCheckoutState() {
				return CheckoutState.Success;
			}

			@Override
			public List<APIError> getErrors() {
				return Collections.emptyList();
			}
		};
	}

	@Override
	public ConfirmCheckoutResponse confirmCheckout(final Payment payment, final String notifyUrl, final String token, final String payerId) throws FrameworkException {

		if (payment.getToken().equals(token)) {

			payment.setPaymentState(PaymentState.completed);
			payment.setPayer(payerId);
			payment.setToken(null);

			return new ConfirmCheckoutResponse() {
				@Override
				public CheckoutState getCheckoutState() {
					return CheckoutState.Success;
				}

				@Override
				public List<APIError> getErrors() {
					return Collections.emptyList();
				}
			};

		} else {

			payment.setPaymentState(PaymentState.error);
			payment.setToken(null);

			return new ConfirmCheckoutResponse() {
				@Override
				public CheckoutState getCheckoutState() {
					return CheckoutState.Failure;
				}

				@Override
				public List<APIError> getErrors() {

					return Arrays.asList(new APIError() {

						@Override
						public String getShortMessage() {
							return "Invalid token";
						}

						@Override
						public String getLongMessage() {
							return "The provided token does not match the payment's token.";
						}

						@Override
						public String getErrorCode() {
							return "001-test";
						}
					});
				}
			};
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

}
