package org.structr.core.script;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import static junit.framework.TestCase.fail;
import org.structr.common.StructrTest;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.TestOne;
import org.structr.core.entity.TestThree;
import org.structr.core.graph.Tx;
import org.structr.schema.action.ActionContext;

/**
 *
 * @author Christian Morgner
 */
public class StructrScriptableTest extends StructrTest {


	@Override
	public void setUp() throws Exception {

		final Map<String, Object> config = new HashMap<>();

		// set scripting engine to rhino
		config.put("scripting.engine", "rhino");

		// call super with additional config
		super.setUp(config);
	}

	public void testEngineDirectly() {

		final ScriptEngine engine = new ScriptEngineManager().getEngineByName("rhino");

		try (final Tx tx = app.tx()) {

			final ActionContext actionContext  = new ActionContext();
			final TestOne entity               = createTestNode(TestOne.class);
			final List<TestThree> testThrees   = createTestNodes(TestThree.class, 10);
			final StructrScriptable scriptable = new StructrScriptable(securityContext, actionContext, entity);
			final String allIdsConcatenated    = concatIds(testThrees);

			engine.put("Structr", scriptable);
			engine.put("securityContext", securityContext);
			engine.put("actionContext", actionContext);

			assertEquals("Invalid JavaScript evaluation result",                  1, engine.eval("Structr.find('TestOne').length;"));
			assertEquals("Invalid JavaScript evaluation result",                 10, engine.eval("Structr.find('TestThree').length;"));
			assertEquals("Invalid JavaScript evaluation result", allIdsConcatenated, engine.eval("var conc = ''; Structr.find('TestThree').forEach(function(s) { conc += s.id; }); conc.toString();"));

			engine.eval("var o = Structr.find('TestThree')[0]; o.name = 'Object'; o.xy = 2;");

			assertEquals("Invalid JavaScript evaluation result", "Object", engine.eval("Structr.find('TestThree')[0].name"));
			assertNull("Invalid JavaScript evaluation result", engine.eval("Structr.find('TestThree')[0].xy"));

			tx.success();

		} catch (ScriptException | FrameworkException fex) {

			fex.printStackTrace();

			fail("Unexpected exception.");
		}
	}

	private <T extends GraphObject> String concatIds(final List<T> list) {

		final StringBuilder buf = new StringBuilder();

		for (final T t : list) {
			buf.append(t.getUuid());
		}

		return buf.toString();
	}
}
