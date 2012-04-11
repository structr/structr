/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.node;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tools.shell.Global;
import org.structr.common.error.FrameworkException;

/**
 * Execute given code as ECMAScript return output
 *
 * @author amorgner
 */
public class ECMAScriptCommand extends NodeServiceCommand {

    private static final Logger logger = Logger.getLogger(ECMAScriptCommand.class.getName());

    @Override
    public Object execute(Object... parameters) throws FrameworkException {

        StringBuilder out = new StringBuilder();

        StringBuilder scriptContent = new StringBuilder();

        // this command takes exactly 1 parameter of type String
        if (parameters != null && parameters.length == 1) {

            Object o = parameters[0];

            if (o instanceof String) {

                scriptContent.append((String) o);

                if (scriptContent != null && scriptContent.length() > 0) {

                    // the following block is needed to turn some non-standard
                    // features on like load, print etc.
                    Global global = new Global();
                    Context cx = ContextFactory.getGlobal().enterContext();
                    global.init(cx);
                    cx.setOptimizationLevel(-1); // don't compile (there's a 64KB limit)
                    //cx.setLanguageVersion(Context.VERSION_1_5);
                    Scriptable scope = cx.initStandardObjects(global);

                    try {
                        // wrap output to be accessible from inside script
                        Object wrappedOut = Context.javaToJS(out, scope);
                        ScriptableObject.putProperty(scope, "out", wrappedOut);
                        Object result = cx.evaluateString(scope, scriptContent.toString(), "ScriptCommand", 1, null);
                        logger.log(Level.INFO, Context.toString(result));

                    } catch (Throwable t) {
                        logger.log(Level.SEVERE, "Error evaluating script", t);
                    } finally {
                        Context.exit();
                    }


                }
            }

        }
        return out.toString();

    }
}
