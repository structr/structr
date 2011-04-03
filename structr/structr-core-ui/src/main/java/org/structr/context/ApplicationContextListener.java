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
import org.structr.core.cloud.StartCloudService;
import org.structr.core.entity.SuperUser;
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

            String tmpPath = properties.getProperty(Services.TMP_PATH);
            logger.log(Level.INFO, "Config file: Temp path: {0}", tmpPath);

            String databasePath = properties.getProperty(Services.DATABASE_PATH);
            logger.log(Level.INFO, "Config file: Database path: {0}", databasePath);

            String filesPath = properties.getProperty(Services.FILES_PATH);
            logger.log(Level.INFO, "Config file: Files path: {0}", filesPath);

            String modulesPath = properties.getProperty(Services.MODULES_PATH);
            logger.log(Level.INFO, "Config file: Modules path: {0}", modulesPath);

            String entityPackages = properties.getProperty(Services.ENTITY_PACKAGES);
            logger.log(Level.INFO, "Config file: Entity packages: {0}", entityPackages);

            String tcpPort = properties.getProperty(Services.TCP_PORT);
            logger.log(Level.INFO, "Config file: TCP port {0}", tcpPort);

            String udpPort = properties.getProperty(Services.UDP_PORT);
            logger.log(Level.INFO, "Config file: UDP port {0}", udpPort);

            if (appTitle != null) {
                context.put(Services.APPLICATION_TITLE, appTitle);
            } else {
                context.put(Services.APPLICATION_TITLE, servletContext.getInitParameter(Services.APPLICATION_TITLE));
            }

            if (tmpPath != null) {
                context.put(Services.TMP_PATH, tmpPath);
            } else {
                context.put(Services.TMP_PATH, servletContext.getInitParameter(Services.TMP_PATH));
            }

            if (databasePath != null) {
                context.put(Services.DATABASE_PATH, databasePath);
            } else {
                context.put(Services.DATABASE_PATH, servletContext.getInitParameter(Services.DATABASE_PATH));
            }

            if (filesPath != null) {
                context.put(Services.FILES_PATH, filesPath);
            } else {
                context.put(Services.FILES_PATH, servletContext.getInitParameter(Services.FILES_PATH));
            }

            if (modulesPath != null) {
                context.put(Services.MODULES_PATH, modulesPath);
            } else {
                context.put(Services.MODULES_PATH, servletContext.getInitParameter(Services.MODULES_PATH));
            }

            if (entityPackages != null) {
                context.put(Services.ENTITY_PACKAGES, entityPackages);
            } else {
                context.put(Services.ENTITY_PACKAGES, servletContext.getInitParameter(Services.ENTITY_PACKAGES));
            }

            if (tcpPort != null) {
                context.put(Services.TCP_PORT, tcpPort);
            } else {
                context.put(Services.TCP_PORT, servletContext.getInitParameter(Services.TCP_PORT));
            }

            if (udpPort != null) {
                context.put(Services.UDP_PORT, udpPort);
            } else {
                context.put(Services.UDP_PORT, servletContext.getInitParameter(Services.UDP_PORT));
            }

        } catch (Throwable t) {
            // handle error
            logger.log(Level.WARNING, "Could not inititialize all values");
        }

        // register predicate that can decide whether a given Class object is a subclass of StructrPage
        context.put(Services.STRUCTR_PAGE_PREDICATE, new Predicate<Class>() {

            @Override
            public boolean evaluate(Class obj) {
                return (StructrPage.class.isAssignableFrom(obj));
            }
        });

        Services.initialize(context);
        //Services.setContext(context);

        // Initialize cloud service
        Services.command(StartCloudService.class);

        logger.log(Level.INFO, "structr application context initialized (structr started successfully)");

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

        // clean session..
        HttpSession session = se.getSession();

        if (session != null) {
            String servletSessionId = se.getSession().getId();

            logger.log(Level.FINE, "HTTP session destroyed, cleaning ", servletSessionId);

            Long sessionId = (Long) session.getAttribute(SessionMonitor.SESSION_ID);

            if (sessionId != null) {

                SessionMonitor.logActivity(new SuperUser(), sessionId, "Logout");

                // Remove session from internal session management
                SessionMonitor.unregisterUserSession(sessionId, session.getServletContext());
            }

        }

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
