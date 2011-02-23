/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core;

/**
 * A service that can be run in a separate thread. It is a good practice to
 * let your RunnableService implementation extend java.lang.Thread and map
 * the startService() and stopService() methods appropriately.
 *
 * @author cmorgner
 */
public interface RunnableService extends Service {

    public void startService();

    public void stopService();

    @Override
    public boolean isRunning();
}
