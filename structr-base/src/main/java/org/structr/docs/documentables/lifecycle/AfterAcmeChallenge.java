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
package org.structr.docs.documentables.lifecycle;

import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.ontology.ConceptType;

import java.util.List;

public class AfterAcmeChallenge extends LifecycleBase {

	public AfterAcmeChallenge() {
		super("afterAcmeChallenge");
	}

	@Override
	public String getShortDescription() {
		return "Called after the ACME challenge authorization flow has completed.";
	}

	@Override
	public String getLongDescription() {
		return """
		The `afterAcmeChallenge()` lifecycle method is called after the entire ACME challenge authorization flow has finished, regardless of whether the certificate was successfully obtained or the process failed.

		To receive this callback, you must create a **global schema method** (user-defined function) called `afterAcmeChallenge`. Instance methods or static methods will not be called.

		This method is typically used to clean up DNS TXT records that were created during the challenge phase via `onAcmeChallenge()`, or to send notifications about the outcome of the certificate retrieval.

		The method receives the result of the certificate retrieval process, including a success flag and any error messages, so you can react accordingly.
		""";
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
			Parameter.mandatory("success", "`true` if the certificate was successfully obtained, `false` otherwise."),
			Parameter.mandatory("errors", "A list of error messages that occurred during the process. Empty if successful.")
		);
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
			Example.javaScript("""
			{
				if ($.args.success) {

					$.log('Certificate successfully obtained!');

					// Clean up DNS TXT records that were created during the challenge
					$.DELETE('https://dns.provider.example/api/records/acme-challenge');

				} else {

					$.log('Certificate retrieval failed: ' + $.args.errors);

					// Send notification about the failure
					$.sendHtmlMail(
						'admin@example.com',
						'noreply@example.com',
						'Certificate renewal failed',
						'<p>ACME certificate retrieval failed with errors: ' + $.args.errors + '</p>'
					);
				}
			}
			""", "Clean up DNS records after successful retrieval, send alert on failure")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
			"This method is called regardless of whether the certificate retrieval succeeded or failed.",
			"The method runs in the context of the superuser.",
			"See also: `onAcmeChallenge()`, the `letsencrypt` maintenance command."
		);
	}

	@Override
	public List<Link> getLinkedConcepts() {

		final List<Link> links = super.getLinkedConcepts();

		links.add(Link.to("isexecutedby", ConceptReference.of(ConceptType.Topic, "Letsencrypt")));

		return links;
	}
}
