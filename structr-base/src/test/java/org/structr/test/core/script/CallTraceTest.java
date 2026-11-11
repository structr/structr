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
package org.structr.test.core.script;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.error.AssertException;
import org.structr.common.error.FrameworkException;
import org.structr.common.event.RuntimeEvent;
import org.structr.common.event.RuntimeEventLog;
import org.structr.common.trace.CallTrace;
import org.structr.common.trace.RequestFrame;
import org.structr.common.trace.ScriptError;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.script.Scripting;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.SchemaMethodTraitDefinition;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Actions;
import org.structr.schema.export.StructrSchema;
import org.structr.test.common.LogCapture;
import org.structr.test.common.StructrTest;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * A failure of user code is reported once, where it happened, with the calls that led there, and not
 * at all when a script catches it.
 */
public class CallTraceTest extends StructrTest {

	@Test
	public void testFailureInNestedCallIsReportedOnceAtItsOrigin() throws FrameworkException {

		try (final Tx tx = app.tx()) {

			method("trace1", "{\n\treturn $.trace2();\n}");
			method("trace2", "{\n\treturn $.trace3();\n}");
			method("trace3", "{\n\treturn $.trace4();\n}");
			method("trace4", "{\n\treturn $.trace5();\n}");
			method("trace5", "{\n\tlet x = 1;\n\treturn $.jdbc('jdbc:sqlserver://nowhere.1433', 'select 1');\n}");

			tx.success();
		}

		final List<String> errors = capture(() -> evaluateExpectingFailure("${{ $.trace1() }}"));

		assertEquals(errors.toString(), 1, errors.size());

		final String[] lines = errors.get(0).split("\\R");

		assertTrue(lines[0], lines[0].contains(":trace5:3:"));
		assertTrue(lines[0], lines[0].contains("JDBC"));
		assertTrue(lines[1], lines[1].contains("called from") && lines[1].contains(":trace4:2:"));
		assertTrue(lines[2], lines[2].contains("called from") && lines[2].contains(":trace3:2:"));
		assertTrue(lines[3], lines[3].contains("called from") && lines[3].contains(":trace2:2:"));
		assertTrue(lines[4], lines[4].contains("called from") && lines[4].contains(":trace1:2:"));
		assertTrue(lines[5], lines[5].contains("called from") && lines[5].contains("outer:1:"));
		assertEquals(errors.get(0), 6, lines.length);
	}

	@Test
	public void testFailureCaughtByScriptIsNotReported() throws FrameworkException {

		try (final Tx tx = app.tx()) {

			method("catching1", "{\n\treturn $.catching2();\n}");
			method("catching2", "{\n\ttry {\n\t\treturn $.catching3();\n\t} catch (e) {\n\t\treturn 'caught';\n\t}\n}");
			method("catching3", "{\n\treturn $.catching4();\n}");
			method("catching4", "{\n\treturn $.jdbc('jdbc:sqlserver://nowhere.1433', 'select 1');\n}");

			tx.success();
		}

		final Object[] result = new Object[1];
		final List<String> errors = capture(() -> result[0] = evaluate("${{ $.catching1() }}"));

		assertEquals("caught", result[0]);
		assertEquals(errors.toString(), 0, errors.size());
	}

	@Test
	public void testScriptErrorInCalledMethodIsReportedAtItsOwnLine() throws FrameworkException {

		try (final Tx tx = app.tx()) {

			method("refCaller", "{\n\tlet a = 1;\n\treturn $.refCallee();\n}");
			method("refCallee", "{\n\tlet b = 2;\n\treturn item.foo;\n}");

			tx.success();
		}

		final List<String> errors = capture(() -> evaluateExpectingFailure("${{ $.refCaller() }}"));

		assertEquals(errors.toString(), 1, errors.size());

		final String[] lines = errors.get(0).split("\\R");

		assertTrue(lines[0], lines[0].contains(":refCallee:3:9: ReferenceError: item is not defined"));
		assertTrue(lines[1], lines[1].contains(":refCaller:3:"));
	}

	@Test
	public void testFailureThroughCallFunctionIsReportedOnce() throws FrameworkException {

		try (final Tx tx = app.tx()) {

			method("viaCallOuter", "{\n\treturn $.call('viaCallInner');\n}");
			method("viaCallInner", "{\n\treturn item.foo;\n}");

			tx.success();
		}

		final List<String> errors = capture(() -> evaluateExpectingFailure("${{ $.viaCallOuter() }}"));

		assertEquals(errors.toString(), 1, errors.size());
		assertTrue(errors.get(0), errors.get(0).split("\\R")[0].contains(":viaCallInner:2:"));
		assertTrue(errors.get(0), errors.get(0).split("\\R")[1].contains(":viaCallOuter:2:"));
	}

	@Test
	public void testStaticMethodIsNamedByItsType() throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.addType("TraceTarget");

			type.addMethod("failStatic", "{\n\treturn item.foo;\n}").setIsStatic(true);

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();
		}

		final List<String> errors = capture(() -> evaluateExpectingFailure("${{ $.TraceTarget.failStatic() }}"));

		assertEquals(errors.toString(), 1, errors.size());
		assertTrue(errors.get(0), errors.get(0).startsWith("TraceTarget[static]:failStatic:2:"));
	}

	@Test
	public void testFailedAssertionIsReportedOnce() throws FrameworkException {

		try (final Tx tx = app.tx()) {

			method("assertOuter", "{\n\treturn $.assertInner();\n}");
			method("assertInner", "{\n\t$.assert(false, 422, 'nope');\n}");

			tx.success();
		}

		final List<String> errors = capture(() -> evaluateExpectingFailure("${{ $.assertOuter() }}"));

		assertEquals(errors.toString(), 1, errors.size());
		assertTrue(errors.get(0), errors.get(0).contains(":assertInner:2:"));
	}

	@Test
	public void testCallStackListsCallersInnermostFirst() throws FrameworkException {

		try (final Tx tx = app.tx()) {

			method("stackOuter", "{\n\treturn $.stackInner();\n}");
			method("stackInner", "{\n\treturn $.callStack();\n}");

			tx.success();
		}

		final Object result = evaluate("${{ $.stackOuter() }}");

		assertTrue(String.valueOf(result), result instanceof List);

		final List<Map<String, Object>> frames = (List<Map<String, Object>>) result;

		assertEquals(frames.toString(), 3, frames.size());
		assertEquals("stackInner", frames.get(0).get("name"));
		assertEquals("METHOD",     frames.get(0).get("kind"));
		assertEquals("stackOuter", frames.get(1).get("name"));
		assertEquals("outer",      frames.get(2).get("name"));

		assertNull("the trace is gone once the evaluation is", CallTrace.current());
	}

	@Test
	public void testExceptionCarriesItsScriptError() throws FrameworkException {

		try (final Tx tx = app.tx()) {

			method("carried", "{\n\treturn item.foo;\n}");

			tx.success();
		}

		try (final Tx tx = app.tx()) {

			Scripting.evaluate(new ActionContext(securityContext), null, "${{ $.carried() }}", "outer", null);
			fail("expected a failure");

		} catch (FrameworkException fex) {

			final ScriptError error = ScriptError.of(fex);

			assertNotNull(error);
			assertEquals("carried", error.getOrigin().getName());
			assertEquals(2, error.getLine());
			assertTrue(error.isReported());
		}
	}

	@Test
	public void testFailureInNewTransactionWithoutHandlerIsReported() throws FrameworkException {

		try (final Tx tx = app.tx()) {

			method("dintUnhandled", "{\n\t$.doInNewTransaction(() => {\n\t\tundefinedVariable = 'x';\n\t});\n\treturn 'done';\n}");

			tx.success();
		}

		final Object[] result = new Object[1];
		final List<String> errors = capture(() -> result[0] = evaluate("${{ $.dintUnhandled() }}"));

		assertEquals("done", result[0]);
		assertEquals(errors.toString(), 1, errors.size());
		assertTrue(errors.get(0), errors.get(0).contains(":dintUnhandled:3:"));
		assertTrue(errors.get(0), errors.get(0).contains("undefinedVariable"));

		// the editor finds its marker by the id of the method, which the worker has to resolve as well
		final List<RuntimeEvent> events = RuntimeEventLog.getEvents(e -> "Scripting".equals(e.getType()) && "dintUnhandled".equals(e.getData().get("name")));

		assertEquals(events.toString(), 1, events.size());
		assertNotNull("the event names the method's node", events.get(0).getData().get("id"));
		assertEquals("SchemaMethod", events.get(0).getData().get("type"));
	}

	@Test
	public void testFailureInNewTransactionWithHandlerIsHandled() throws FrameworkException {

		try (final Tx tx = app.tx()) {

			method("dintHandled", "{\n\t$.doInNewTransaction(() => {\n\t\tundefinedVariable = 'x';\n\t}, (ex) => {\n\t\t$.log('handled ', ex);\n\t\treturn false;\n\t});\n\treturn 'done';\n}");

			tx.success();
		}

		final Object[] result     = new Object[1];
		final List<String> logged = new java.util.ArrayList<>();
		final List<String> errors = capture(() -> {

			logged.addAll(captureInfo(LogFunctionName, () -> result[0] = evaluate("${{ $.dintHandled() }}")));
			return null;
		});

		assertEquals("done", result[0]);
		assertEquals("an error the handler receives is not reported: " + errors, 0, errors.size());
		assertEquals(logged.toString(), 1, logged.size());
		assertTrue(logged.get(0), logged.get(0).contains("handled "));
		assertTrue("the handler sees where it happened: " + logged.get(0), logged.get(0).contains(":dintHandled:3:"));
	}

	@Test
	public void testLoggingACaughtScriptErrorShowsWhereItHappened() throws FrameworkException {

		try (final Tx tx = app.tx()) {

			method("logGuest", "{\n\ttry {\n\t\tundefinedVariable = 'wurst';\n\t} catch (e) {\n\t\t$.log(e);\n\t}\n\treturn 'ok';\n}");

			tx.success();
		}

		final Object[] result     = new Object[1];
		final List<String> logged = new java.util.ArrayList<>();
		final List<String> errors = capture(() -> {

			logged.addAll(captureInfo(LogFunctionName, () -> result[0] = evaluate("${{ $.logGuest() }}")));
			return null;
		});

		assertEquals("ok", result[0]);
		assertEquals(errors.toString(), 0, errors.size());
		assertEquals(logged.toString(), 1, logged.size());
		assertTrue(logged.get(0), logged.get(0).contains(":logGuest:3:"));
		assertTrue(logged.get(0), logged.get(0).contains("ReferenceError"));
		assertTrue(logged.get(0), logged.get(0).contains("called from"));
	}

	@Test
	public void testFailureOfTheOutermostMethodIsReported() throws FrameworkException {

		try (final Tx tx = app.tx()) {

			method("outermostGuest", "{\n\tlet a = 1;\n\treturn item.foo;\n}");
			method("outermostHost", "{\n\tlet a = 1;\n\treturn $.jdbc('jdbc:sqlserver://nowhere.1433', 'select 1');\n}");

			tx.success();
		}

		// called the way a REST request calls a method: the method is the outermost evaluation, and its
		// async wrapper is settled by the host rather than at an evaluation boundary
		final List<String> guest = capture(() -> callExpectingFailure("outermostGuest"));

		assertEquals(guest.toString(), 1, guest.size());
		assertTrue(guest.get(0), guest.get(0).contains(":outermostGuest:3:9: ReferenceError: item is not defined"));

		final List<String> host = capture(() -> callExpectingFailure("outermostHost"));

		assertEquals(host.toString(), 1, host.size());
		assertTrue(host.get(0), host.get(0).contains(":outermostHost"));
		assertTrue(host.get(0), host.get(0).contains("JDBC"));
	}

	@Test
	public void testCaughtFailureOfNestedCallReleasesTheContext() throws FrameworkException {

		try (final Tx tx = app.tx()) {

			method("releaseFails", "{\n\treturn item.foo;\n}");

			tx.success();
		}

		final ActionContext actionContext = new ActionContext(securityContext);

		try (final Tx tx = app.tx()) {

			assertEquals("caught", Scripting.evaluate(actionContext, null, "${{ try { $.releaseFails(); } catch (e) { 'caught'; } }}", "outer", null));

			tx.success();
		}

		// the evaluation is over, so nothing may still hold a reference to its context
		assertNull("the context of a finished evaluation must be closed", actionContext.getScriptingContexts().get("js"));
	}

	// ----- private methods -----
	@Test
	public void testFailingAttributeExpressionIsLoggedOnce() throws FrameworkException {

		final PropertyKey<String> nameKey = Traits.of(StructrTraits.GROUP).key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
		final NodeInterface node;

		try (final Tx tx = app.tx()) {

			node = app.create(StructrTraits.GROUP, new NodeAttribute<>(nameKey, "${{\n\tthrow new Error('attribute-marker');\n}}"));

			tx.success();
		}

		final List<String> lines = LogCapture.mentions("attribute-marker", () -> {

			try (final Tx tx = app.tx()) {

				assertNull(node.getPropertyWithVariableReplacement(new ActionContext(securityContext), nameKey));

				tx.success();

			} catch (FrameworkException fex) {

				fail("Unexpected exception " + fex.getMessage());
			}

			return null;
		});

		assertEquals(lines.toString(), 1, lines.size());
	}

	@Test
	public void testFailingFunctionPropertyReadIsLoggedOnce() throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.addType("TraceFunctionHolder");

			type.addFunctionProperty("broken").setReadFunction("{\n\tthrow new Error('function-property-marker');\n}");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();
		}

		final List<String> lines = LogCapture.mentions("function-property-marker", () -> {

			try (final Tx tx = app.tx()) {

				final NodeInterface node = app.create("TraceFunctionHolder", "holder");

				assertNull(node.getProperty(Traits.of("TraceFunctionHolder").key("broken")));

				tx.success();

			} catch (FrameworkException fex) {

				fail("Unexpected exception " + fex.getMessage());
			}

			return null;
		});

		assertEquals(lines.toString(), 1, lines.size());
	}

	@Test
	public void testFailingOnFinishFunctionOfAJobIsAttributed() {

		final List<String> lines = LogCapture.mentions("onfinish-marker", 10_000, () -> evaluate("${{\n\t$.schedule(() => {}, 'trace job', () => {\n\t\tthrow new Error('onfinish-marker');\n\t});\n}}"));

		assertEquals(lines.toString(), 1, lines.size());
		assertTrue(lines.get(0), lines.get(0).matches("(?s).*:3:\\d+: .*onfinish-marker.*"));
	}

	@Test
	public void testFlowLeftOnAThreadIsDiscardedBeforeTheNextOneEnters() {

		// a frame entered and never exited, as a pooled thread would carry it into its next request
		CallTrace.enter(new RequestFrame("GET", "/left-behind", "previous-session"));

		CallTrace.discardStaleFlow("HTTP GET /next");

		assertNull(CallTrace.current());

		final RequestFrame next = new RequestFrame("GET", "/next", null);

		assertTrue(next.isRoot());
		assertNull(next.getSessionId());
	}

	@Test
	public void testFailingPostCommitProcedureIsReportedWithTheTraceThatQueuedIt() {

		final List<String> lines = LogCapture.mentions("post-commit-marker", () -> {

			CallTrace.run(new RequestFrame("POST", "/trace-post-commit", null), () -> {

				try (final Tx tx = app.tx()) {

					TransactionCommand.queuePostProcessProcedure(() -> {

						try {

							Scripting.evaluate(new ActionContext(securityContext), null, "${{\n\tthrow new Error('post-commit-marker');\n}}", "postCommit", null);

						} catch (FrameworkException ignore) {
							// reported when the after-commit frame exits
						}
					});

					tx.success();

				} catch (FrameworkException fex) {

					fail("Unexpected exception " + fex.getMessage());
				}
			});

			return null;
		});

		assertEquals(lines.toString(), 1, lines.size());
		assertTrue(lines.get(0), lines.get(0).matches("(?s)postCommit:2:\\d+: .*post-commit-marker.*called from HTTP POST /trace-post-commit.*"));
	}

	@Test
	public void testFailingLifecycleMethodIsNamedByItsCallbackWithItsCodeSource() throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.addType("TraceLifecycleHolder");

			type.addMethod("onCreate", "{\n\tthrow new Error('lifecycle-marker');\n}");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();
		}

		final List<String> lines = LogCapture.mentions("lifecycle-marker", () -> {

			try (final Tx tx = app.tx()) {

				app.create("TraceLifecycleHolder", "holder");

				tx.success();

				fail("Expected onCreate to fail");

			} catch (FrameworkException expected) {
			}

			return null;
		});

		assertEquals(lines.toString(), 1, lines.size());
		assertTrue(lines.get(0), lines.get(0).matches("(?s).*TraceLifecycleHolder\\[[0-9a-f]{32}\\]:onCreate:2:\\d+: .*lifecycle-marker.*"));

		final List<RuntimeEvent> events = RuntimeEventLog.getEvents(e -> "Scripting".equals(e.getType()) && String.valueOf(e.getData().get("message")).contains("lifecycle-marker"));

		assertEquals(events.toString(), 1, events.size());
		assertEquals("onCreate", events.get(0).getData().get("name"));
		assertEquals("the event names the method's node", StructrTraits.SCHEMA_METHOD, events.get(0).getData().get("type"));
	}

	@Test
	public void testFailingAttributeExpressionIsReportedAtItsRowInTheValue() throws FrameworkException {

		final PropertyKey<String> nameKey = Traits.of(StructrTraits.GROUP).key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
		final NodeInterface node;

		try (final Tx tx = app.tx()) {

			node = app.create(StructrTraits.GROUP, new NodeAttribute<>(nameKey, "first ${'line'}\nsecond\n${{\n\tthrow new Error('attribute-row-marker');\n}}"));

			tx.success();
		}

		final List<String> lines = LogCapture.mentions("attribute-row-marker", () -> {

			try (final Tx tx = app.tx()) {

				node.getPropertyWithVariableReplacement(new ActionContext(securityContext), nameKey);

				tx.success();

			} catch (FrameworkException fex) {

				fail("Unexpected exception " + fex.getMessage());
			}

			return null;
		});

		assertEquals(lines.toString(), 1, lines.size());
		assertTrue(lines.get(0), lines.get(0).matches("(?s).*:4:\\d+: .*attribute-row-marker.*"));
	}

	private Object callExpectingFailure(final String name) {

		try (final Tx tx = app.tx()) {

			Actions.callWithSecurityContext(name, securityContext, Map.of());

			fail("Expected " + name + " to fail");

		} catch (FrameworkException | AssertException expected) {
		}

		return null;
	}

	private void method(final String name, final String source) throws FrameworkException {

		final Traits traits = Traits.of(StructrTraits.SCHEMA_METHOD);

		app.create(StructrTraits.SCHEMA_METHOD,
			new NodeAttribute<>(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name),
			new NodeAttribute<>(traits.key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), source)
		);
	}

	private Object evaluate(final String script) {

		try (final Tx tx = app.tx()) {

			final Object result = Scripting.evaluate(new ActionContext(securityContext), null, script, "outer", null);

			tx.success();

			return result;

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception " + fex.getMessage());
		}

		return null;
	}

	private Object evaluateExpectingFailure(final String script) {

		try (final Tx tx = app.tx()) {

			Scripting.evaluate(new ActionContext(securityContext), null, script, "outer", null);

			fail("Expected " + script + " to fail");

		} catch (FrameworkException | AssertException expected) {
		}

		return null;
	}

	private static final String LogFunctionName = "org.structr.core.function.LogFunction";

	private List<String> captureInfo(final String loggerName, final Supplier<Object> code) {

		final Logger logger                        = (Logger) LoggerFactory.getLogger(loggerName);
		final ListAppender<ILoggingEvent> appender = new ListAppender<>();

		appender.start();
		logger.addAppender(appender);

		try {

			code.get();

		} finally {

			logger.detachAppender(appender);
		}

		return appender.list.stream().filter(e -> e.getLevel() == Level.INFO).map(ILoggingEvent::getFormattedMessage).toList();
	}

	/**
	 * Runs the given code and answers the error lines the scripting layer logged meanwhile.
	 */
	private List<String> capture(final Supplier<Object> code) {

		final Logger logger                        = (Logger) LoggerFactory.getLogger(Scripting.class.getName());
		final ListAppender<ILoggingEvent> appender = new ListAppender<>();

		appender.start();
		logger.addAppender(appender);

		try {

			code.get();

		} finally {

			logger.detachAppender(appender);
		}

		return appender.list.stream().filter(e -> e.getLevel() == Level.ERROR).map(ILoggingEvent::getFormattedMessage).toList();
	}
}
