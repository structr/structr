/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.web.function;

import org.structr.docs.Signature;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class ValidateCertificatesFunction extends UiAdvancedFunction {

	public static final String ERROR_MESSAGE_ADD_HEADER    = "Usage: ${ validate_certificates(boolean) }. Example: ${ validate_certificates(false) }";
	public static final String ERROR_MESSAGE_ADD_HEADER_JS = "Usage: ${{ $.validateCertificates(boolean) }}. Example: ${{ $.validateCertificates(false) }}";

	@Override
	public String getName() {
		return "validate_certificates";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("boolean");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) {

		assertArrayHasMinLengthAndTypes(sources, 1, Boolean.class);

		final Boolean validate = (Boolean)sources[0];

        ctx.setValidateCertificates(validate);

        return "";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_ADD_HEADER_JS : ERROR_MESSAGE_ADD_HEADER);
	}

	@Override
	public String getShortDescription() {
		return "Disables or enables strict certificate checking when performing a request in a scripting context. The setting remains for the whole request.";
	}
}
