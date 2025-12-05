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
package org.structr.test.web.advanced;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.property.GenericProperty;
import org.structr.core.script.Scripting;
import org.structr.rest.common.HttpHelper;
import org.structr.schema.action.ActionContext;
import org.structr.test.web.StructrUiTest;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.AssertJUnit.*;

/**
 */
public class EncodingTest extends StructrUiTest {

	@Test
	public void testEncoding() {

		final String testString = "abcdefgjihklmnopqrstuvwxyzäöüßóñABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ";
		final int port          = 54678;

		final SocketConfig socketConfig = SocketConfig.custom()
			.setSoTimeout(1000)
			.setTcpNoDelay(true)
			.build();

		final HttpServer server = ServerBootstrap.bootstrap()
	                .setListenerPort(port)
	                .setSocketConfig(socketConfig)
			.registerHandler("*", new HttpRequestHandler() {

				@Override
				public void handle(final HttpRequest request, final HttpResponse response, final HttpContext context) throws HttpException, IOException {

					response.setEntity(new ByteArrayEntity(testString.getBytes("iso-8859-1")));
					response.setStatusCode(200);
				}

			}).create();

		try {

		        server.start();

		} catch (IOException ioex) {
			ioex.printStackTrace();
		}

		try {
			final Object result = Scripting.evaluate(new ActionContext(securityContext), null, "${GET('http://localhost:" + port + "/')}", "test");

			assertNotNull("Result should not be null", result);
			assertTrue("Result should be a map", result instanceof GraphObjectMap);
			assertEquals(testString, ((GraphObjectMap)result).get(new GenericProperty<>(HttpHelper.FIELD_BODY)).toString());

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		server.stop();
	}
}
