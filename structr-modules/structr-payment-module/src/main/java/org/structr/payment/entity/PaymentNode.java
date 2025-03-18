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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.payment.entity;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.graph.NodeInterface;
import org.structr.payment.api.Payment;
import org.structr.payment.api.PaymentProvider;
import org.structr.payment.impl.paypal.PayPalPaymentProvider;
import org.structr.payment.impl.stripe.StripePaymentProvider;
import org.structr.payment.impl.test.TestPaymentProvider;

/**
 *
 */
public interface PaymentNode extends NodeInterface, Payment {

	GraphObject beginCheckout(final String providerName, final String successUrl, final String cancelUrl) throws FrameworkException;
	void cancelCheckout(final String providerName, final String token) throws FrameworkException;
	GraphObject confirmCheckout(final String providerName, final String notifyUrl, final String token, final String payerId) throws FrameworkException;

	public static PaymentProvider getPaymentProvider(final String providerName) {

		switch (providerName) {

			case "paypal":
				return new PayPalPaymentProvider();

			case "stripe":
				return new StripePaymentProvider();

			case "test":
				return new TestPaymentProvider();
		}

		return null;
	}
}
