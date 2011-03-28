/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.node;

import java.util.List;
import org.structr.core.RunnableService;
import org.structr.core.Service;
import org.structr.core.Services;

/**
 * A runnable service which runs in depends on NodeService
 *
 * Registers/unregisters on startup/shutdown with NodeService
 *
 * @author axel
 */
public abstract class RunnableNodeService extends Thread implements RunnableService {

    public RunnableNodeService() {
    }

    public RunnableNodeService(final String name) {
        super(name);
    }

    /**
     * Register this service at the NodeService after starting
     */
    @Override
    public void start() {

        super.start();

        List<Service> serviceList = Services.getServices();
        for (Service s : serviceList) {
            if (s instanceof NodeService) {
                ((NodeService) s).registerService(this);
            }
        }

    }

    /*
     * Unregister this service from the NodeService before interrupting
     * the thread
     */
    @Override
    public void interrupt() {

        List<Service> serviceList = Services.getServices();
        for (Service s : serviceList) {
            if (s instanceof NodeService) {
                ((NodeService) s).unregisterService(this);
            }
        }

        super.interrupt();

    }

}
