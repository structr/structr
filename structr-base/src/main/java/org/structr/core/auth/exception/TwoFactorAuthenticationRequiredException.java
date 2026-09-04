/*
 * Copyright (C) 2010-2026 Structr GmbH
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

import org.structr.api.config.Settings;
import org.structr.core.entity.Principal;
import org.structr.web.function.BarcodeFunction;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

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

	public Map<String, String> getData() {

		final Map<String, String> data = new HashMap<>(Map.of(
				"token", nextStepToken,
				"twoFactorLoginPage", Settings.TwoFactorLoginPage.getValue(),
				"deviceTrustPossible", String.valueOf(user.isDeviceTrustPossible()),
				"deviceTrustDuration", Settings.TwoFactorDeviceTrustDuration.getValue().toString()
		));

		if (showQrCode) {

			final Map<String, Object> hints = Map.of(
					"MARGIN", 0,
					"ERROR_CORRECTION", "M"
			);

			data.put("qrdata", Base64.getUrlEncoder().encodeToString(BarcodeFunction.getQRCode(user.getTwoFactorUrl(), "QR_CODE", 200, 200, hints).getBytes(StandardCharsets.ISO_8859_1)));
		}

		return data;
	}
}