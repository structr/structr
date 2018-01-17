/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.rest.resource;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.containsString;
import org.junit.Test;
import org.structr.rest.common.StructrRestTest;

public class SchemaJsonResourceTest extends StructrRestTest {

	@Test
	public void testGet() {

		createEntity("/schema_node", "{ \"name\": \"TestType0\", \"_foo\": \"String\" }");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)

				.body("result", containsString("TestType0"))
				.body("result_count", equalTo(1))
			.when()
				.get("/maintenance/_schemaJson");

	}

	@Test
	public void testPost() {

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
				.body("{ \"schema\": \"{ \\\"definitions\\\": { \\\"CustomTermAttribute\\\": { \\\"$extends\\\": \\\"https://structr.org/v1.1/definitions/CustomTermAttribute\\\", \\\"type\\\": \\\"object\\\" }, \\\"DataFeed\\\": { \\\"$extends\\\": \\\"https://structr.org/v1.1/definitions/DataFeed\\\", \\\"type\\\": \\\"object\\\" }, \\\"FeedItem\\\": { \\\"$extends\\\": \\\"https://structr.org/v1.1/definitions/FeedItem\\\", \\\"type\\\": \\\"object\\\" }, \\\"FeedItemContent\\\": { \\\"$extends\\\": \\\"https://structr.org/v1.1/definitions/FeedItemContent\\\", \\\"type\\\": \\\"object\\\" }, \\\"FeedItemEnclosure\\\": { \\\"$extends\\\": \\\"https://structr.org/v1.1/definitions/FeedItemEnclosure\\\", \\\"type\\\": \\\"object\\\" }, \\\"File\\\": { \\\"$extends\\\": \\\"https://structr.org/v1.1/definitions/FileBase\\\", \\\"type\\\": \\\"object\\\" }, \\\"Folder\\\": { \\\"$extends\\\": \\\"https://structr.org/v1.1/definitions/Folder\\\", \\\"type\\\": \\\"object\\\" }, \\\"Group\\\": { \\\"$extends\\\": \\\"https://structr.org/v1.1/definitions/Group\\\", \\\"type\\\": \\\"object\\\" }, \\\"Image\\\": { \\\"$extends\\\": \\\"https://structr.org/v1.1/definitions/Image\\\", \\\"type\\\": \\\"object\\\" }, \\\"MQTTClient\\\": { \\\"$extends\\\": \\\"https://structr.org/v1.1/definitions/MQTTClient\\\", \\\"type\\\": \\\"object\\\" }, \\\"MQTTSubscriber\\\": { \\\"$extends\\\": \\\"https://structr.org/v1.1/definitions/MQTTSubscriber\\\", \\\"type\\\": \\\"object\\\" }, \\\"MailTemplate\\\": { \\\"$extends\\\": \\\"https://structr.org/v1.1/definitions/MailTemplate\\\", \\\"type\\\": \\\"object\\\" }, \\\"ODSExporter\\\": { \\\"$extends\\\": \\\"https://structr.org/v1.1/definitions/ODSExporter\\\", \\\"type\\\": \\\"object\\\" }, \\\"ODTExporter\\\": { \\\"$extends\\\": \\\"https://structr.org/v1.1/definitions/ODTExporter\\\", \\\"type\\\": \\\"object\\\" }, \\\"Page\\\": { \\\"$extends\\\": \\\"https://structr.org/v1.1/definitions/Page\\\", \\\"type\\\": \\\"object\\\" }, \\\"PaymentItemNode\\\": { \\\"$extends\\\": \\\"https://structr.org/v1.1/definitions/PaymentItemNode\\\", \\\"type\\\": \\\"object\\\" }, \\\"PaymentNode\\\": { \\\"$extends\\\": \\\"https://structr.org/v1.1/definitions/PaymentNode\\\", \\\"type\\\": \\\"object\\\" }, \\\"PreferredTerm\\\": { \\\"$extends\\\": \\\"https://structr.org/v1.1/definitions/PreferredTerm\\\", \\\"type\\\": \\\"object\\\" }, \\\"TestType\\\": { \\\"type\\\": \\\"object\\\" }, \\\"ThesaurusConcept\\\": { \\\"$extends\\\": \\\"https://structr.org/v1.1/definitions/ThesaurusConcept\\\", \\\"type\\\": \\\"object\\\" }, \\\"ThesaurusTerm\\\": { \\\"$extends\\\": \\\"https://structr.org/v1.1/definitions/ThesaurusTerm\\\", \\\"type\\\": \\\"object\\\" }, \\\"User\\\": { \\\"$extends\\\": \\\"https://structr.org/v1.1/definitions/User\\\", \\\"type\\\": \\\"object\\\" }, \\\"VideoFile\\\": { \\\"$extends\\\": \\\"https://structr.org/v1.1/definitions/VideoFile\\\", \\\"type\\\": \\\"object\\\" }, \\\"Widget\\\": { \\\"$extends\\\": \\\"https://structr.org/v1.1/definitions/Widget\\\", \\\"type\\\": \\\"object\\\" }, \\\"XMPPClient\\\": { \\\"$extends\\\": \\\"https://structr.org/v1.1/definitions/XMPPClient\\\", \\\"type\\\": \\\"object\\\" } }, \\\"id\\\": \\\"https://structr.org/schema/faeaf388412c49eaa31c0443cef905e5/#\\\", \\\"methods\\\": [ { \\\"comment\\\": \\\"\\\", \\\"name\\\": \\\"replaceOwnSchema\\\", \\\"source\\\": \\\"POST(\\\\u0027/_jsonSchema\\\\u0027,\\\\u0027{\\\\\\\"schema\\\\\\\":\\\\\\\"{ \\\\\\\\\\\\\\\"definitions\\\\\\\\\\\\\\\": { \\\\\\\\\\\\\\\"CustomTermAttribute\\\\\\\\\\\\\\\": { \\\\\\\\\\\\\\\"$extends\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"https://structr.org/v1.1/definitions/CustomTermAttribute\\\\\\\\\\\\\\\", \\\\\\\\\\\\\\\"type\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"object\\\\\\\\\\\\\\\" }, \\\\\\\\\\\\\\\"DataFeed\\\\\\\\\\\\\\\": { \\\\\\\\\\\\\\\"$extends\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"https://structr.org/v1.1/definitions/DataFeed\\\\\\\\\\\\\\\", \\\\\\\\\\\\\\\"type\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"object\\\\\\\\\\\\\\\" }, \\\\\\\\\\\\\\\"FeedItem\\\\\\\\\\\\\\\": { \\\\\\\\\\\\\\\"$extends\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"https://structr.org/v1.1/definitions/FeedItem\\\\\\\\\\\\\\\", \\\\\\\\\\\\\\\"type\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"object\\\\\\\\\\\\\\\" }, \\\\\\\\\\\\\\\"FeedItemContent\\\\\\\\\\\\\\\": { \\\\\\\\\\\\\\\"$extends\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"https://structr.org/v1.1/definitions/FeedItemContent\\\\\\\\\\\\\\\", \\\\\\\\\\\\\\\"type\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"object\\\\\\\\\\\\\\\" }, \\\\\\\\\\\\\\\"FeedItemEnclosure\\\\\\\\\\\\\\\": { \\\\\\\\\\\\\\\"$extends\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"https://structr.org/v1.1/definitions/FeedItemEnclosure\\\\\\\\\\\\\\\", \\\\\\\\\\\\\\\"type\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"object\\\\\\\\\\\\\\\" }, \\\\\\\\\\\\\\\"File\\\\\\\\\\\\\\\": { \\\\\\\\\\\\\\\"$extends\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"https://structr.org/v1.1/definitions/FileBase\\\\\\\\\\\\\\\", \\\\\\\\\\\\\\\"type\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"object\\\\\\\\\\\\\\\" }, \\\\\\\\\\\\\\\"Folder\\\\\\\\\\\\\\\": { \\\\\\\\\\\\\\\"$extends\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"https://structr.org/v1.1/definitions/Folder\\\\\\\\\\\\\\\", \\\\\\\\\\\\\\\"type\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"object\\\\\\\\\\\\\\\" }, \\\\\\\\\\\\\\\"Group\\\\\\\\\\\\\\\": { \\\\\\\\\\\\\\\"$extends\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"https://structr.org/v1.1/definitions/Group\\\\\\\\\\\\\\\", \\\\\\\\\\\\\\\"type\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"object\\\\\\\\\\\\\\\" }, \\\\\\\\\\\\\\\"Image\\\\\\\\\\\\\\\": { \\\\\\\\\\\\\\\"$extends\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"https://structr.org/v1.1/definitions/Image\\\\\\\\\\\\\\\", \\\\\\\\\\\\\\\"type\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"object\\\\\\\\\\\\\\\" }, \\\\\\\\\\\\\\\"MQTTClient\\\\\\\\\\\\\\\": { \\\\\\\\\\\\\\\"$extends\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"https://structr.org/v1.1/definitions/MQTTClient\\\\\\\\\\\\\\\", \\\\\\\\\\\\\\\"type\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"object\\\\\\\\\\\\\\\" }, \\\\\\\\\\\\\\\"MQTTSubscriber\\\\\\\\\\\\\\\": { \\\\\\\\\\\\\\\"$extends\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"https://structr.org/v1.1/definitions/MQTTSubscriber\\\\\\\\\\\\\\\", \\\\\\\\\\\\\\\"type\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"object\\\\\\\\\\\\\\\" }, \\\\\\\\\\\\\\\"MailTemplate\\\\\\\\\\\\\\\": { \\\\\\\\\\\\\\\"$extends\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"https://structr.org/v1.1/definitions/MailTemplate\\\\\\\\\\\\\\\", \\\\\\\\\\\\\\\"type\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"object\\\\\\\\\\\\\\\" }, \\\\\\\\\\\\\\\"ODSExporter\\\\\\\\\\\\\\\": { \\\\\\\\\\\\\\\"$extends\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"https://structr.org/v1.1/definitions/ODSExporter\\\\\\\\\\\\\\\", \\\\\\\\\\\\\\\"type\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"object\\\\\\\\\\\\\\\" }, \\\\\\\\\\\\\\\"ODTExporter\\\\\\\\\\\\\\\": { \\\\\\\\\\\\\\\"$extends\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"https://structr.org/v1.1/definitions/ODTExporter\\\\\\\\\\\\\\\", \\\\\\\\\\\\\\\"type\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"object\\\\\\\\\\\\\\\" }, \\\\\\\\\\\\\\\"Page\\\\\\\\\\\\\\\": { \\\\\\\\\\\\\\\"$extends\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"https://structr.org/v1.1/definitions/Page\\\\\\\\\\\\\\\", \\\\\\\\\\\\\\\"type\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"object\\\\\\\\\\\\\\\" }, \\\\\\\\\\\\\\\"PaymentItemNode\\\\\\\\\\\\\\\": { \\\\\\\\\\\\\\\"$extends\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"https://structr.org/v1.1/definitions/PaymentItemNode\\\\\\\\\\\\\\\", \\\\\\\\\\\\\\\"type\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"object\\\\\\\\\\\\\\\" }, \\\\\\\\\\\\\\\"PaymentNode\\\\\\\\\\\\\\\": { \\\\\\\\\\\\\\\"$extends\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"https://structr.org/v1.1/definitions/PaymentNode\\\\\\\\\\\\\\\", \\\\\\\\\\\\\\\"type\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"object\\\\\\\\\\\\\\\" }, \\\\\\\\\\\\\\\"PreferredTerm\\\\\\\\\\\\\\\": { \\\\\\\\\\\\\\\"$extends\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"https://structr.org/v1.1/definitions/PreferredTerm\\\\\\\\\\\\\\\", \\\\\\\\\\\\\\\"type\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"object\\\\\\\\\\\\\\\" }, \\\\\\\\\\\\\\\"TestType\\\\\\\\\\\\\\\": { \\\\\\\\\\\\\\\"type\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"object\\\\\\\\\\\\\\\" }, \\\\\\\\\\\\\\\"ThesaurusConcept\\\\\\\\\\\\\\\": { \\\\\\\\\\\\\\\"$extends\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"https://structr.org/v1.1/definitions/ThesaurusConcept\\\\\\\\\\\\\\\", \\\\\\\\\\\\\\\"type\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"object\\\\\\\\\\\\\\\" }, \\\\\\\\\\\\\\\"ThesaurusTerm\\\\\\\\\\\\\\\": { \\\\\\\\\\\\\\\"$extends\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"https://structr.org/v1.1/definitions/ThesaurusTerm\\\\\\\\\\\\\\\", \\\\\\\\\\\\\\\"type\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"object\\\\\\\\\\\\\\\" }, \\\\\\\\\\\\\\\"User\\\\\\\\\\\\\\\": { \\\\\\\\\\\\\\\"$extends\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"https://structr.org/v1.1/definitions/User\\\\\\\\\\\\\\\", \\\\\\\\\\\\\\\"type\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"object\\\\\\\\\\\\\\\" }, \\\\\\\\\\\\\\\"VideoFile\\\\\\\\\\\\\\\": { \\\\\\\\\\\\\\\"$extends\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"https://structr.org/v1.1/definitions/VideoFile\\\\\\\\\\\\\\\", \\\\\\\\\\\\\\\"type\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"object\\\\\\\\\\\\\\\" }, \\\\\\\\\\\\\\\"Widget\\\\\\\\\\\\\\\": { \\\\\\\\\\\\\\\"$extends\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"https://structr.org/v1.1/definitions/Widget\\\\\\\\\\\\\\\", \\\\\\\\\\\\\\\"type\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"object\\\\\\\\\\\\\\\" }, \\\\\\\\\\\\\\\"XMPPClient\\\\\\\\\\\\\\\": { \\\\\\\\\\\\\\\"$extends\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"https://structr.org/v1.1/definitions/XMPPClient\\\\\\\\\\\\\\\", \\\\\\\\\\\\\\\"type\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"object\\\\\\\\\\\\\\\" } }, \\\\\\\\\\\\\\\"id\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"https://structr.org/schema/faeaf388412c49eaa31c0443cef905e5/#\\\\\\\\\\\\\\\", \\\\\\\\\\\\\\\"methods\\\\\\\\\\\\\\\": [ { \\\\\\\\\\\\\\\"comment\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"\\\\\\\\\\\\\\\", \\\\\\\\\\\\\\\"name\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"sendTestMail\\\\\\\\\\\\\\\", \\\\\\\\\\\\\\\"source\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"{\\\\\\\\\\\\\\\\n\\\\\\\\\\\\\\\\tvar fromAddress \\\\\\\\\\\\\\\\u003d \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\"christian.kramp@structr.com\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\";\\\\\\\\\\\\\\\\n\\\\\\\\\\\\\\\\tvar fromName \\\\\\\\\\\\\\\\u003d \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\"ck\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\";\\\\\\\\\\\\\\\\n\\\\\\\\\\\\\\\\tvar toAddress \\\\\\\\\\\\\\\\u003d \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\"christian.kramp@structr.com\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\";\\\\\\\\\\\\\\\\n\\\\\\\\\\\\\\\\tvar toName \\\\\\\\\\\\\\\\u003d \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\"ck\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\";\\\\\\\\\\\\\\\\n\\\\\\\\\\\\\\\\tvar subject \\\\\\\\\\\\\\\\u003d \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\"test\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\";\\\\\\\\\\\\\\\\n\\\\\\\\\\\\\\\\tvar htmlContent \\\\\\\\\\\\\\\\u003d \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\"test\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\";\\\\\\\\\\\\\\\\n\\\\\\\\\\\\\\\\tvar textContent \\\\\\\\\\\\\\\\u003d \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\"test\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\";\\\\\\\\\\\\\\\\n\\\\\\\\\\\\\\\\tvar attachments \\\\\\\\\\\\\\\\u003d [];\\\\\\\\\\\\\\\\n\\\\\\\\\\\\\\\\tattachments.push(Structr.first(Structr.find(\\\\\\\\\\\\\\\\u0027File\\\\\\\\\\\\\\\\u0027,\\\\\\\\\\\\\\\\u0027name\\\\\\\\\\\\\\\\u0027,\\\\\\\\\\\\\\\\u0027testfile.csv\\\\\\\\\\\\\\\\u0027)));\\\\\\\\\\\\\\\\n\\\\\\\\\\\\\\\\tattachments.push(Structr.first(Structr.find(\\\\\\\\\\\\\\\\u0027File\\\\\\\\\\\\\\\\u0027,\\\\\\\\\\\\\\\\u0027name\\\\\\\\\\\\\\\\u0027,\\\\\\\\\\\\\\\\u0027testfile2.csv\\\\\\\\\\\\\\\\u0027)));\\\\\\\\\\\\\\\\n\\\\\\\\\\\\\\\\tStructr.send_html_mail(fromAddress, fromName, toAddress, toName, subject, htmlContent, textContent, attachments);\\\\\\\\\\\\\\\\t\\\\\\\\\\\\\\\\n}\\\\\\\\\\\\\\\", \\\\\\\\\\\\\\\"visibleToAuthenticatedUsers\\\\\\\\\\\\\\\": false, \\\\\\\\\\\\\\\"visibleToPublicUsers\\\\\\\\\\\\\\\": false }, { \\\\\\\\\\\\\\\"comment\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"\\\\\\\\\\\\\\\", \\\\\\\\\\\\\\\"name\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"sendTestMailWithoutAttachment\\\\\\\\\\\\\\\", \\\\\\\\\\\\\\\"source\\\\\\\\\\\\\\\": \\\\\\\\\\\\\\\"{\\\\\\\\\\\\\\\\n\\\\\\\\\\\\\\\\tvar fromAddress \\\\\\\\\\\\\\\\u003d \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\"christian.kramp@structr.com\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\";\\\\\\\\\\\\\\\\n\\\\\\\\\\\\\\\\tvar fromName \\\\\\\\\\\\\\\\u003d \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\"ck\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\";\\\\\\\\\\\\\\\\n\\\\\\\\\\\\\\\\tvar toAddress \\\\\\\\\\\\\\\\u003d \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\"christian.kramp@structr.com\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\";\\\\\\\\\\\\\\\\n\\\\\\\\\\\\\\\\tvar toName \\\\\\\\\\\\\\\\u003d \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\"ck\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\";\\\\\\\\\\\\\\\\n\\\\\\\\\\\\\\\\tvar subject \\\\\\\\\\\\\\\\u003d \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\"test\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\";\\\\\\\\\\\\\\\\n\\\\\\\\\\\\\\\\tvar htmlContent \\\\\\\\\\\\\\\\u003d \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\"test\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\";\\\\\\\\\\\\\\\\n\\\\\\\\\\\\\\\\tvar textContent \\\\\\\\\\\\\\\\u003d \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\"test\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\";\\\\\\\\\\\\\\\\n\\\\\\\\\\\\\\\\tStructr.send_html_mail(fromAddress, fromName, toAddress, toName, subject, htmlContent, textContent);\\\\\\\\\\\\\\\\t\\\\\\\\\\\\\\\\n}\\\\\\\\\\\\\\\", \\\\\\\\\\\\\\\"visibleToAuthenticatedUsers\\\\\\\\\\\\\\\": false, \\\\\\\\\\\\\\\"visibleToPublicUsers\\\\\\\\\\\\\\\": false } ] }\\\\\\\"}\\\\u0027)\\\", \\\"visibleToAuthenticatedUsers\\\": false, \\\"visibleToPublicUsers\\\": false }, { \\\"comment\\\": \\\"\\\", \\\"name\\\": \\\"sendTestMail\\\", \\\"source\\\": \\\"{\\\\n\\\\tvar fromAddress \\\\u003d \\\\\\\"christian.kramp@structr.com\\\\\\\";\\\\n\\\\tvar fromName \\\\u003d \\\\\\\"ck\\\\\\\";\\\\n\\\\tvar toAddress \\\\u003d \\\\\\\"christian.kramp@structr.com\\\\\\\";\\\\n\\\\tvar toName \\\\u003d \\\\\\\"ck\\\\\\\";\\\\n\\\\tvar subject \\\\u003d \\\\\\\"test\\\\\\\";\\\\n\\\\tvar htmlContent \\\\u003d \\\\\\\"test\\\\\\\";\\\\n\\\\tvar textContent \\\\u003d \\\\\\\"test\\\\\\\";\\\\n\\\\tvar attachments \\\\u003d [];\\\\n\\\\tattachments.push(Structr.first(Structr.find(\\\\u0027File\\\\u0027,\\\\u0027name\\\\u0027,\\\\u0027testfile.csv\\\\u0027)));\\\\n\\\\tattachments.push(Structr.first(Structr.find(\\\\u0027File\\\\u0027,\\\\u0027name\\\\u0027,\\\\u0027testfile2.csv\\\\u0027)));\\\\n\\\\tStructr.send_html_mail(fromAddress, fromName, toAddress, toName, subject, htmlContent, textContent, attachments);\\\\t\\\\n}\\\", \\\"visibleToAuthenticatedUsers\\\": false, \\\"visibleToPublicUsers\\\": false }, { \\\"comment\\\": \\\"\\\", \\\"name\\\": \\\"sendTestMailWithoutAttachment\\\", \\\"source\\\": \\\"{\\\\n\\\\tvar fromAddress \\\\u003d \\\\\\\"christian.kramp@structr.com\\\\\\\";\\\\n\\\\tvar fromName \\\\u003d \\\\\\\"ck\\\\\\\";\\\\n\\\\tvar toAddress \\\\u003d \\\\\\\"christian.kramp@structr.com\\\\\\\";\\\\n\\\\tvar toName \\\\u003d \\\\\\\"ck\\\\\\\";\\\\n\\\\tvar subject \\\\u003d \\\\\\\"test\\\\\\\";\\\\n\\\\tvar htmlContent \\\\u003d \\\\\\\"test\\\\\\\";\\\\n\\\\tvar textContent \\\\u003d \\\\\\\"test\\\\\\\";\\\\n\\\\tStructr.send_html_mail(fromAddress, fromName, toAddress, toName, subject, htmlContent, textContent);\\\\t\\\\n}\\\", \\\"visibleToAuthenticatedUsers\\\": false, \\\"visibleToPublicUsers\\\": false } ] }\" }")

			.expect()
				.statusCode(200)

			.when()
				.post("/maintenance/_schemaJson");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
				.statusCode(200)

			.when()
				.get("/TestType");
	}

}
