package org.structr.core.script.polyglot.function;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.ScriptingError;
import org.structr.core.GraphObject;
import org.structr.core.script.StructrScriptable;
import org.structr.core.script.polyglot.PolyglotWrapper;
import org.structr.schema.action.ActionContext;

import java.util.Arrays;

public class GrantFunction<T extends GraphObject> implements ProxyExecutable {
	private final ActionContext actionContext;
	private final T node;

	public GrantFunction(final ActionContext actionContext, final T node) {

		this.actionContext = actionContext;
		this.node = node;
	}

	@Override
	public Object execute(Value... arguments) {

		if (arguments != null && arguments.length > 0) {
			Object[] parameters = Arrays.stream(arguments).map(arg -> PolyglotWrapper.unwrap(actionContext, arg)).toArray();

			if (parameters.length > 0 && parameters[0] != null) {

				try {

					if (parameters.length >= 2 && parameters[1] != null) {

						// principal, node, string
						final Object principal = parameters[0];
						String permissions = parameters[1].toString();

						// append additional parameters to permission string
						if (parameters.length > 2) {

							for (int i = 2; i < parameters.length; i++) {

								if (parameters[i] != null) {
									permissions += "," + parameters[i].toString();
								}
							}
						}

						// call function, entity can be null here!
						new org.structr.core.function.GrantFunction().apply(actionContext, null, new Object[]{principal, node, permissions});
					}

					return null;

				} catch (FrameworkException ex) {

					actionContext.raiseError(422, new ScriptingError(ex));
				}
			}
		}

		return null;
	}
}
