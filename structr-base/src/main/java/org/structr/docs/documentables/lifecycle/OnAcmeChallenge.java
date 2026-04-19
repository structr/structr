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

public class OnAcmeChallenge extends LifecycleBase {

	public OnAcmeChallenge() {
		super("onAcmeChallenge");
	}

	@Override
	public String getShortDescription() {
		return "Called for each ACME challenge during Let's Encrypt certificate retrieval.";
	}

	@Override
	public String getLongDescription() {
		return """
		The `onAcmeChallenge()` lifecycle method is called for each ACME challenge that needs to be fulfilled during automated TLS certificate retrieval via Let's Encrypt. This is typically used with **DNS challenges** to automate the creation of DNS TXT records required for domain validation.

		To receive this callback, you must create a **global schema method** (user-defined function) called `onAcmeChallenge`. Instance methods or static methods will not be called.

		When the `letsencrypt` maintenance command runs with a DNS challenge, it calls this method for each domain that needs to be authorized. The method receives the challenge details as parameters, allowing you to automate DNS record creation via your DNS provider's API.

		If this method is not defined, Structr will log the required DNS record details and wait for manual creation within the configured timeout period.

		If this method is defined and returns a non-null value, Structr assumes that the DNS record has been created programmatically and proceeds with the challenge verification.
		""";
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
			Parameter.mandatory("type", "The challenge type, currently always `dns`."),
			Parameter.mandatory("domain", "The domain name being authorized (e.g. `example.com`)."),
			Parameter.mandatory("record", "The full DNS record name to create (e.g. `_acme-challenge.example.com.`)."),
			Parameter.mandatory("digest", "The digest value to use as the TXT record content.")
		);
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
			Example.javaScript("""
			{
				// Example: Automate DNS TXT record creation via a DNS provider API
				$.log('Creating ACME challenge DNS record for domain: ' + $.args.domain);
				$.log('Record: ' + $.args.record + ' IN TXT ' + $.args.digest);

				// Call your DNS provider API to create the TXT record
				$.POST(
					'https://dns.provider.example/api/records',
					'application/json',
					'{"type":"TXT","name":"' + $.args.record + '","content":"' + $.args.digest + '","ttl":60}'
				);

				// Return a non-null value to signal that the record was created
				return true;
			}
			""", "Automate DNS TXT record creation for ACME challenge authorization")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
			"This method is only called for DNS challenges (`challenge=dns`). HTTP challenges are handled automatically by Structr.",
			"The method runs in the context of the superuser.",
			"See also: `afterAcmeChallenge()`, the `letsencrypt` maintenance command."
		);
	}

	@Override
	public List<Link> getLinkedConcepts() {

		final List<Link> links = super.getLinkedConcepts();

		links.add(Link.to("isexecutedby", ConceptReference.of(ConceptType.Topic, "Letsencrypt")));

		return links;
	}
}
