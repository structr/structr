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
package org.structr.test.rest.servlet;

import jakarta.servlet.ServletException;
import org.structr.rest.servlet.DocumentationServlet;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * #1582: the DocumentationServlet was always mounted without authentication and its PUT/POST wrote
 * attacker-controlled content to disk and to the served docs. Writing over HTTP was a development-time
 * convenience and is now disabled: both methods must throw rather than perform any work.
 */
public class DocumentationServletTest {

	// subclass to reach the protected doPut/doPost from the test
	private static final class ExposedDocumentationServlet extends DocumentationServlet {

		void put() throws ServletException, IOException {

			doPut(null, null);
		}

		void post() throws ServletException, IOException {

			doPost(null, null);
		}
	}

	@Test
	public void doPutIsDisabled() throws IOException {

		try {

			new ExposedDocumentationServlet().put();
			fail("doPut must be disabled and throw ServletException instead of writing to disk");

		} catch (ServletException expected) {

			assertTrue("exception should mention PUT: " + expected.getMessage(), expected.getMessage() != null && expected.getMessage().contains("PUT"));
		}
	}

	@Test
	public void doPostIsDisabled() throws IOException {

		try {

			new ExposedDocumentationServlet().post();
			fail("doPost must be disabled and throw ServletException instead of regenerating docs");

		} catch (ServletException expected) {

			assertTrue("exception should mention POST: " + expected.getMessage(), expected.getMessage() != null && expected.getMessage().contains("POST"));
		}
	}
}
