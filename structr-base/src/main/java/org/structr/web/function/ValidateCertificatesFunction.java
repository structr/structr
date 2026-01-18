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
package org.structr.web.function;

import org.structr.docs.Example;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class ValidateCertificatesFunction extends UiAdvancedFunction {

	@Override
	public String getName() {
		return "validateCertificates";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("boolean");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) {

		assertArrayHasMinLengthAndTypes(sources, 1, Boolean.class);

		final Boolean validate = (Boolean)sources[0];

        ctx.setValidateCertificates(validate);

        return "";
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${ validateCertificates(boolean) }. Example: ${ validateCertificates(false) }"),
			Usage.javaScript("Usage: ${{ $.validateCertificates(boolean) }}. Example: ${{ $.validateCertificates(false) }}")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
				"By default, certificate validation is always enabled - only in rare cases would/should it be necessary to change this behaviour"
		);
	}

	@Override
	public String getShortDescription() {
		return "Disables or enables strict certificate checking when performing a request in a scripting context. The setting remains for the whole request.";
	}

	@Override
	public String getLongDescription() {
		return "Disables or enables certificate validation for outgoing requests. All subsequent `GET()`, `HEAD()`, `POST()`, `PUT()` or `DELETE()` calls in the same request (meaning the request from the client to Structr) will use the setting configured here.";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.javaScript("""
						${{
						    $.validateCertificates(false);
						    let result = $.GET('https://www.domain-with-invalid-certificate.com/resource.json');
						}}
						""")
		);
	}
}
