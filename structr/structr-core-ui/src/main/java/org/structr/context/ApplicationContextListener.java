/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.context;

import java.io.FileInputStream;
import javax.servlet.http.HttpSessionEvent;
import org.structr.core.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionListener;
import org.structr.ui.page.StructrPage;

/**
 * Web application lifecycle listener.
 *
 * @author cmorgner
 */
public class ApplicationContextListener implements ServletContextListener, HttpSessionListener {

    private static final Logger logger = Logger.getLogger(ApplicationContextListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.log(Level.INFO, "Servlet context created");

        Map<String, Object> context = new ConcurrentHashMap<String, Object>();
        ServletContext servletContext = sce.getServletContext();

        String configFile = servletContext.getInitParameter(Services.CONFIG_FILE_PATH);
        context.put(Services.CONFIG_FILE_PATH, configFile);
        context.put(Services.SERVLET_CONTEXT, servletContext);

        try {
            // load config file
            Properties properties = new Properties();
            properties.load(new FileInputStream(configFile));

            String appTitle = properties.getProperty(Services.APPLICATION_TITLE);
            logger.log(Level.INFO, "Config file: Application title: {0}", appTitle);

            String databasePath = properties.getProperty(Services.DATABASE_PATH_IDENTIFIER);
            logger.log(Level.INFO, "Config file: Database path: {0}", databasePath);

            String filesPath = properties.getProperty(Services.FILES_PATH_IDENTIFIER);
            logger.log(Level.INFO, "Config file: Files path: {0}", filesPath);

            String pluginsPath = properties.getProperty(Services.MODULES_PATH_IDENTIFIER);
            logger.log(Level.INFO, "Config file: Plugins path: {0}", pluginsPath);

            String entityPackages = properties.getProperty(Services.ENTITY_PACKAGES_IDENTIFIER);
            logger.log(Level.INFO, "Config file: Entity Packages: {0}", entityPackages);

            context.put(Services.APPLICATION_TITLE, appTitle);
            context.put(Services.DATABASE_PATH_IDENTIFIER, databasePath);
            context.put(Services.FILES_PATH_IDENTIFIER, filesPath);
            context.put(Services.MODULES_PATH_IDENTIFIER, pluginsPath);
            context.put(Services.ENTITY_PACKAGES_IDENTIFIER, entityPackages);

        } catch (Throwable t) {
            // handle error
            // logger.log(Level.WARNING, "Problems reading config file, initializing with default values from servlet context", t);
            context.put(Services.APPLICATION_TITLE, servletContext.getInitParameter(Services.APPLICATION_TITLE));
            context.put(Services.DATABASE_PATH_IDENTIFIER, servletContext.getInitParameter(Services.DATABASE_PATH_IDENTIFIER));
            context.put(Services.FILES_PATH_IDENTIFIER, servletContext.getInitParameter(Services.FILES_PATH_IDENTIFIER));
            context.put(Services.MODULES_PATH_IDENTIFIER, servletContext.getInitParameter(Services.MODULES_PATH_IDENTIFIER));
            context.put(Services.ENTITY_PACKAGES_IDENTIFIER, servletContext.getInitParameter(Services.ENTITY_PACKAGES_IDENTIFIER));
        }

        // register predicate that can decide whether a given Class object is a subclass of StructrPage
        context.put(Services.STRUCTR_PAGE_PREDICATE, new Predicate<Class>() {

            @Override
            public boolean evaluate(Class obj) {
                return (StructrPage.class.isAssignableFrom(obj));
            }
        });


        // initialize services layzily, just set context parameter
        //Services.initialize(context);
        Services.setContext(context);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        logger.log(Level.INFO, "Servlet context destroyed");

        // TODO: remove servlet context attributes? Is it necessary?

        Services.shutdown();
    }

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        logger.log(Level.FINE, "HTTP session created", se.getSession().getId());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        logger.log(Level.FINE, "HTTP session destroyed, cleaning", se.getSession().getId());

        // clean session..
        HttpSession session = se.getSession();
        long sessionId = (Long) session.getAttribute(SessionMonitor.SESSION_ID);
        
        SessionMonitor.logActivity(sessionId, "Logout");

        // Remove session from internal session management
        SessionMonitor.unregisterUser(sessionId, session.getServletContext());

//
//        // TODO: when running embedded under Winstone,
//        // there's a ConcurrentModificationException thrown at logout
//        // I tried to fix it by adding the 'synchronized() block, but
//        // it didn't help. Must be investigated further ...
//        synchronized (session) {
//            for (Enumeration e = session.getAttributeNames(); e.hasMoreElements();) {
//                String name = (String) e.nextElement();
//                session.removeAttribute(name);
//            }
//        }
    }
}
