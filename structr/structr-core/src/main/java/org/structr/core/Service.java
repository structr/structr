/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core;

import java.util.Map;

/**
 * The base class for services in structr.
 *
 * @author cmorgner
 */
public interface Service {

    /**
     * Called by <code>Services.createCommand()</code> before the command is returned to
     * the user. Use this method to inject service-specific resources into your command
     * objects so you can access them later in the <code>execute()</code> method.
     *
     * @param command
     */
    public void injectArguments(Command command);

    /**
     * Called by <code>Serivces</code> after the service is instantiated to initialize
     * service-specific resources etc.
     *
     * @param context the context
     */
    public void initialize(Map<String, Object> context);

    /**
     * Called before the service is discarded. Note that this method will not be called
     * for instances of <code>PrototypeService</code>.
     */
    public void shutdown();

    /**
     * Return true if Service is running.
     * @return
     */
    public boolean isRunning();

    /**
     * Return name of service
     * @return
     */
    public String getName();
}
