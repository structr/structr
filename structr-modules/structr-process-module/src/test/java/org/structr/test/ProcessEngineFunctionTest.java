/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.test;

import com.auth0.jwt.interfaces.Claim;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.process.auth.ProcessJWTHelper;
import org.structr.process.bpmn.BpmnImporter;
import org.structr.process.function.ExportBPMNFunction;
import org.structr.process.function.ImportBPMNFunction;
import org.structr.process.function.NotifyFunction;
import org.structr.process.function.ProcessInstanceUrlFunction;
import org.structr.process.function.ProcessTokenFunction;
import org.structr.process.function.ValidateProcessTokenFunction;
import org.structr.schema.action.ActionContext;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.AssertJUnit.*;

/**
 * Tests for the process module's scripting functions (function/*) and the
 * {@link ProcessJWTHelper} that backs process-token creation/validation.
 *
 * Functions are exercised directly via {@code new XxxFunction().apply(ctx, caller, args)}
 * so no function registration is required.
 */
public class ProcessEngineFunctionTest extends AbstractProcessEngineTest {

	private String savedSecret;
	private String savedType;
	private String savedIssuer;

	@BeforeMethod
	public void setJwtSecret() {

		savedSecret = Settings.JWTSecret.getValue();
		savedType   = Settings.JWTSecretType.getValue();
		savedIssuer = Settings.JWTIssuer.getValue();
		Settings.JWTSecretType.setValue("secret");
		Settings.JWTSecret.setValue("0123456789abcdef0123456789abcdef"); // 32 chars
		Settings.JWTIssuer.setValue("structr");
	}

	@AfterMethod
	public void restoreJwtSecret() {

		Settings.JWTSecret.setValue(savedSecret);
		Settings.JWTSecretType.setValue(savedType);
		Settings.JWTIssuer.setValue(savedIssuer);
	}

	private ActionContext ctx() {

		return new ActionContext(securityContext);
	}

	// ==================================================================
	// ProcessJWTHelper
	// ==================================================================

	@Test
	public void testJwtTokenRoundTrip() throws Exception {

		final String token = ProcessJWTHelper.createProcessToken("pid-1", "tid-1", "review");
		assertNotNull(token);

		final Map<String, Claim> claims = ProcessJWTHelper.validateProcessToken(token);
		assertNotNull("a freshly-created token must validate", claims);
		assertEquals("pid-1",   ProcessJWTHelper.getProcessInstanceId(claims));
		assertEquals("tid-1",   ProcessJWTHelper.getTaskId(claims));
		assertEquals("review",  ProcessJWTHelper.getAction(claims));
		assertEquals("process", claims.get(ProcessJWTHelper.CLAIM_SCOPE).asString());
	}

	@Test
	public void testJwtInvalidTokensReturnNull() throws Exception {

		final String token = ProcessJWTHelper.createProcessToken("pid-1", "tid-1", "review");
		// validate never throws for bad tokens -- it returns null.
		assertNull(ProcessJWTHelper.validateProcessToken(token + "tampered"));
		assertNull(ProcessJWTHelper.validateProcessToken("not.a.jwt"));
	}

	@Test
	public void testJwtWrongIssuerReturnsNull() throws Exception {

		final String token = ProcessJWTHelper.createProcessToken("pid-1", "tid-1", "review");
		Settings.JWTIssuer.setValue("someone-else");
		assertNull("a token from a different issuer must not validate", ProcessJWTHelper.validateProcessToken(token));
	}

	@Test
	public void testJwtWeakSecretAndKeypairFailToCreate() throws Exception {

		Settings.JWTSecret.setValue("too-short");

		try {

			ProcessJWTHelper.createProcessToken("p", "t", "a");
			fail("expected 500 creating a token with a weak secret");

		} catch (final FrameworkException expected) {

			assertEquals(500, expected.getStatus());
		}

		// validate swallows the same failure and returns null.
		assertNull(ProcessJWTHelper.validateProcessToken("anything"));

		Settings.JWTSecret.setValue("0123456789abcdef0123456789abcdef");
		Settings.JWTSecretType.setValue("keypair");

		try {

			ProcessJWTHelper.createProcessToken("p", "t", "a");
			fail("expected 500 in keypair mode (unsupported)");

		} catch (final FrameworkException expected) {

			assertEquals(500, expected.getStatus());
		}
	}

	// ==================================================================
	// processToken / validateProcessToken functions
	// ==================================================================

	@Test
	public void testProcessTokenFunction() throws Exception {

		final Object token = new ProcessTokenFunction().apply(ctx(), null, new Object[] { "pid-1", "tid-1", "review" });
		assertNotNull(ProcessJWTHelper.validateProcessToken(token.toString()));

		// non-numeric expiryMinutes falls back to the default -> still a valid token.
		final Object token2 = new ProcessTokenFunction().apply(ctx(), null, new Object[] { "pid-1", "tid-1", "review", "notanumber" });
		assertNotNull(ProcessJWTHelper.validateProcessToken(token2.toString()));

		// too few args -> returns null (not a valid token).
		final Object resultForInvalidCall = new ProcessTokenFunction().apply(ctx(), null, new Object[] { "pid-1", "tid-1" });
		assertNull("calling processToken with incorrect number of arguments must yield null", resultForInvalidCall);
	}

	@Test
	public void testValidateProcessTokenFunction() throws Exception {

		final String token = ProcessJWTHelper.createProcessToken("pid-1", "tid-1", "review");
		final Object out = new ValidateProcessTokenFunction().apply(ctx(), null, new Object[] { token });

		assertTrue(out instanceof Map);
		@SuppressWarnings("unchecked")
		final Map<String, Object> m = (Map<String, Object>) out;
		assertEquals("pid-1",   m.get("processInstanceId"));
		assertEquals("tid-1",   m.get("taskId"));
		assertEquals("review",  m.get("action"));
		assertEquals("process", m.get("scope"));

		// invalid token -> null
		assertNull(new ValidateProcessTokenFunction().apply(ctx(), null, new Object[] { "bad.token" }));

		final Object resultForInvalidCall = new ValidateProcessTokenFunction().apply(ctx(), null, new Object[] { token, "extra" });
		assertNull("validate takes exactly one arg; calling it extra args must yield null, not a Map", resultForInvalidCall);
	}

	// ==================================================================
	// processInstanceUrl
	// ==================================================================

	@Test
	public void testProcessInstanceUrlNonNodeReturnsNull() throws Exception {

		try (final Tx tx = app.tx()) {

			// first arg is present (passes the arg-count assertion) but not a node -> null.
			assertNull(new ProcessInstanceUrlFunction().apply(ctx(), null, new Object[] { "not-a-node" }));
			tx.success();
		}
	}

	// ==================================================================
	// notify
	// ==================================================================

	@Test
	public void testNotifyFunction() throws Exception {

		try (final Tx tx = app.tx()) {

			// 'log' channel is SMTP-free and returns TRUE.
			assertEquals(Boolean.TRUE, new NotifyFunction().apply(ctx(), null, new Object[] { "log", "admin", "subject", "body" }));

			// unknown channel -> 422.
			try {

				new NotifyFunction().apply(ctx(), null, new Object[] { "carrier-pigeon", "x", "s", "m" });
				fail("expected 422 for an unknown notification channel");

			} catch (final FrameworkException expected) {

				assertEquals(422, expected.getStatus());
			}

			// too few args -> returns null (not TRUE).
			final Object resultForInvalidCall = new NotifyFunction().apply(ctx(), null, new Object[] { "log", "admin", "subject" });
			assertNull(resultForInvalidCall);
			tx.success();
		}
	}

	// ==================================================================
	// importBpmn / exportBpmn
	// ==================================================================

	@Test
	public void testImportBpmnFunction() throws Exception {

		try (final Tx tx = app.tx()) {

			final String xml = loadResource("/simple-approval.bpmn");
			final Object def = new ImportBPMNFunction().apply(ctx(), null, new Object[] { xml });

			assertTrue("import should return the created BpmnDefinitions node", def instanceof NodeInterface);

			// non-String arg -> returns null (not a node).
			final Object resultForInvalidCall = new ImportBPMNFunction().apply(ctx(), null, new Object[] { 42 });
			assertNull(resultForInvalidCall);
			tx.success();
		}
	}

	@Test
	public void testExportBpmnFunction() throws Exception {

		try (final Tx tx = app.tx()) {

			final String xml = loadResource("/simple-approval.bpmn");
			final NodeInterface defNode = new BpmnImporter(securityContext).importBpmn(xml);
			final Object out = new ExportBPMNFunction().apply(ctx(), null, new Object[] { defNode });

			assertNotNull(out);
			assertTrue("export should produce BPMN XML", out.toString().contains("definitions"));

			// non-Node arg -> returns null (not BPMN XML).
			final Object resultForInvalidCall = new ExportBPMNFunction().apply(ctx(), null, new Object[] { "not-a-node" });
			assertNull(resultForInvalidCall);
			tx.success();
		}
	}
}
