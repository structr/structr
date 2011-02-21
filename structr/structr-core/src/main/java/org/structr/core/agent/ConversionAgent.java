/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.agent;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.Services;
import org.structr.core.UnsupportedArgumentError;
import org.structr.core.entity.CsvFile;
import org.structr.core.entity.StructrNode;
import org.structr.core.entity.User;
import org.structr.core.node.ConvertCsvToNodeListCommand;

/**
 *
 * @author amorgner
 */
public class ConversionAgent extends Agent {

    private static final Logger logger = Logger.getLogger(ConversionAgent.class.getName());

    public ConversionAgent() {
        setName("ConversionAgent");
    }

    @Override
    public Class getSupportedTaskType() {
        return (ConversionTask.class);
    }

    @Override
    public ReturnValue processTask(Task task) {

        if (task instanceof ConversionTask) {

            ConversionTask ct = (ConversionTask) task;
            logger.log(Level.INFO, "Task found, starting conversion ...");
            convert(ct.getUser(), ct.getSourceNode(), ct.getTargetNodeClass());
            logger.log(Level.INFO, " done.");

        }

        return (ReturnValue.Success);
    }

    private void convert(final User user, final StructrNode sourceNode, final Class targetClass) {

        if (sourceNode == null) {
            throw new UnsupportedArgumentError("Source node is null!");
        }

        if (sourceNode instanceof CsvFile) {
            Services.command(ConvertCsvToNodeListCommand.class).execute(user, sourceNode, targetClass);
        } else {
            throw new UnsupportedArgumentError("Source node type " + sourceNode.getType() + " not supported. This agent can convert only CSV files.");
        }

    }
}
