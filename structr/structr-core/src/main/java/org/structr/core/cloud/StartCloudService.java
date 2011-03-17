/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.cloud;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author axel
 */
public class StartCloudService extends CloudServiceCommand {

    private static final Logger logger = Logger.getLogger(StartCloudService.class.getName());

    @Override
    public Object execute(Object... parameters) {

        logger.log(Level.INFO, "StartCloudService command executed.");

        return null;
    }
}
