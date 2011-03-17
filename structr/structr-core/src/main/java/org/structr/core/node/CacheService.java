/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.node;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.SingletonService;

/**
 *
 * @author amorgner
 */
public class CacheService implements SingletonService {

    private static final Logger logger = Logger.getLogger(CacheService.class.getName());

    private ConcurrentHashMap cache = null;

    // <editor-fold defaultstate="collapsed" desc="interface SingletonService">
    @Override
    public void injectArguments(Command command) {
        if (command != null) {
            command.setArgument("cache", cache);
        }
    }

    @Override
    public void initialize(Map<String, Object> context) {

        String dbPath = (String) context.get(Services.DATABASE_PATH);

        try {
            logger.log(Level.INFO, "Initializing cache ...", dbPath);
            cache = new ConcurrentHashMap();
            logger.log(Level.INFO, "Cache ready.");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Database could not be initialized.{0}", e.getMessage());
        }
    }

    @Override
    public void shutdown() {
        if (isRunning()) {
            cache = null;
        }
    }

    @Override
    public boolean isRunning() {
        return (cache != null);
    }

    @Override
    public String getName() {
        return CacheService.class.getName();
    }
    // </editor-fold>
}
