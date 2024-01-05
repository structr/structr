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
