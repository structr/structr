/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.auth.exception;

import org.structr.core.entity.Principal;

public class TwoFactorAuthenticationRequiredException extends UnauthorizedException {

	private Principal user       = null;
	private String nextStepToken = null;
	private boolean showQrCode   = false;

	public TwoFactorAuthenticationRequiredException(final Principal user, final String token, final boolean showQrCode) {

		super("Two factor authentication - login via OTC required");

		this.nextStepToken = token;
		this.showQrCode    = showQrCode;
		this.user          = user;
	}

	public String getNextStepToken() {
		return nextStepToken;
	}

	public boolean showQrCode() {
		return showQrCode;
	}

	public Principal getUser() {
		return user;
	}

}