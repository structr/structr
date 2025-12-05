/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.mail.function;

import org.structr.common.error.FrameworkException;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.mail.AdvancedMailModule;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class MailSelectConfigFunction extends AdvancedMailModuleFunction {

	public MailSelectConfigFunction(final AdvancedMailModule parent) {
		super(parent);
	}

	@Override
	public String getName() {
		return "mail_select_config";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("name");
	}

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		try {

			if (sources.length == 0 || sources[0] == null) {

				ctx.getAdvancedMailContainer().setConfigurationPrefix(null);

			} else {

				ctx.getAdvancedMailContainer().setConfigurationPrefix(sources[0].toString());
			}

			return "";

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${mail_select_config(name)}"),
			Usage.javaScript("Usage: ${{ $.mailSelectConfig(name) }}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Selects a configuration prefix for the SMTP configuration.";
	}

	@Override
	public String getLongDescription() {
		return """
				Allows selecting a different SMTP configuration (as configured in structr.conf) for the current outgoing mail.
				
				The six SMTP settings can be overridden **individually** by adding a prefixed configuration entry. If no entry is found the default (non-prefixed) value is used.
				""";
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
				Parameter.mandatory("name", "name prefix to use for lookup in configuration")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
				"A selected configuration can be removed by calling `mail_select_config()` without parameters or with `null` or `\"\"` as parameter.",
				"A manual configuration (see `mail_set_manual_config()`) overrides a selected configuration which overrides the default configuration."
		);
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("""
						${mail_select_config('myDifferentConfig')}
						
						**Example structr.conf**
						[...]
						smtp.host = <server>
						smtp.port = <port>
						smtp.user = <user>
						smtp.password = <password>
						smtp.tls.enabled = true
						smtp.tls.required = true
						myDifferentConfig.smtp.host = <server>
						myDifferentConfig.smtp.port = <port>
						myDifferentConfig.smtp.user = <user>
						myDifferentConfig.smtp.password = <password>
						myDifferentConfig.smtp.tls.enabled = true
						myDifferentConfig.smtp.tls.required = true
						[...]
						""")
		);
	}
}