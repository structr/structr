/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.cloud;

import org.structr.core.Command;

/**
 *
 * @author axel
 */
public abstract class CloudServiceCommand extends Command {

    @Override
    public Class getServiceClass() {

        return CloudService.class;

    }
}
