/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.node;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tools.shell.Global;

/**
 * Execute given code as ECMAScript return output
 *
 * @author amorgner
 */
public class ECMAScriptCommand extends NodeServiceCommand {

    private static final Logger logger = Logger.getLogger(ECMAScriptCommand.class.getName());

    @Override
    public Object execute(Object... parameters) {

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
