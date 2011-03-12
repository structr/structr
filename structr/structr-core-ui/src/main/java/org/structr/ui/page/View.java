/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.ui.page;

import javax.servlet.http.HttpServletResponse;
import org.structr.core.entity.Image;
import org.structr.core.entity.File;
import org.structr.core.entity.PlainText;
import org.structr.core.entity.AbstractNode;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.click.util.Bindable;
import org.apache.click.Page;

/**
 * 
 * @author amorgner
 */
public class View extends StructrPage {

    private static final Logger logger = Logger.getLogger(View.class.getName());

    @Bindable
    protected StringBuilder output = new StringBuilder();

    public View() {
        super();

        // create a context path for local links, like e.g.
        // "/structr-webapp/view.htm?nodeId="
        contextPath = getContext().getRequest().getContextPath().concat(
                getContext().getPagePath(View.class).concat("?").concat(
                NODE_ID_KEY.concat("=")));

    }

    /**
     * @see Page#onSecurityCheck()
     */
    @Override
    public boolean onSecurityCheck() {
        userName = getContext().getRequest().getRemoteUser();
        return true;
    }

    /**
     * Render current node to output.
     * This method calls the @see#renderView() method of the node's implementing class
     *
     * @see Page#onRender()
     */
    @Override
    public void onRender() {

        AbstractNode s = getNodeByIdOrPath(getNodeId());

        if (s == null) {

            logger.log(Level.FINE, "Node {0} not found", getNodeId());


            // TODO: change to structr page (make independent from Click framework)
            getContext().getResponse().setStatus(HttpServletResponse.SC_NOT_FOUND);
            setForward("/click/not-found.htm");

        } else {

            // inject session and request
            s.setRequest(getContext().getRequest());
            s.setSession(getContext().getSession());

            // Check visibility before access rights to assure that the
            // existance of hidden objects is not exposed

            if (!(s.isVisible(user))) {
                logger.log(Level.FINE, "Hidden page requested ({0})", getNodeId());

                // TODO: change to structr page (make independent from Click framework)
                getContext().getResponse().setStatus(HttpServletResponse.SC_NOT_FOUND);
                setForward("/click/not-found.htm");
                return;
            }

            // check read access right
            if (!(isSuperUser || s.readAllowed(user))) {
                logger.log(Level.FINE, "Secure page requested ({0})", getNodeId());

                // TODO: change to structr page (make independent from Click framework)
                getContext().getResponse().setStatus(HttpServletResponse.SC_FORBIDDEN);
                setForward("/not-authorized.html");

                return;
            }



            String contentType = s.getContentType();
            String editUrl = null;
            if (editNodeId != null) {
                AbstractNode editNode = getNodeByIdOrPath(editNodeId);
                editUrl = getContext().getRequest().getContextPath().concat(getContext().getPagePath(getEditPageClass(editNode))).concat("?").concat(NODE_ID_KEY).concat("=").concat(editNodeId.toString()).concat("&").concat(RENDER_MODE_KEY).concat("=").concat(INLINE_MODE);
            }

            // some nodes should be rendered directly to servlet response
            // note: HtmlSource is instanceof PlainText!
            if (s instanceof File || s instanceof Image || (s instanceof PlainText && !("text/html".equals(contentType)))) {
                // use Apache Click direct rendering
                HttpServletResponse response = getContext().getResponse();

                // Set response headers
                //response.setHeader("Content-Disposition", "attachment; filename=\"" + s.getName() + "\"");
                response.setContentType(contentType);
                //response.setHeader("Pragma", "no-cache");

//                            String appMode = getContext().getApplicationMode();

                // in production mode, enable caching by setting some header values
                //if (ConfigService.MODE_PRODUCTION.equals(appMode)) {

                // add some caching directives to header
                // see http://weblogs.java.net/blog/2007/08/08/expires-http-header-magic-number-yslow
                Calendar cal = new GregorianCalendar();
                int seconds = 7 * 24 * 60 * 60 + 1; // 7 days + 1 sec
                cal.add(Calendar.SECOND, seconds);
                response.addHeader("Cache-Control",
                        "public, max-age=" + seconds
                        + ", s-maxage=" + seconds);
                //+ ", must-revalidate, proxy-revalidate"

                DateFormat httpDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
                httpDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                response.addHeader("Expires", httpDateFormat.format(cal.getTime()));

                Date lastModified = s.getLastModifiedDate();
                if (lastModified != null) {
                    response.addHeader("Last-Modified", httpDateFormat.format(lastModified));
                }
                //}

                try {
                    // clean response's output
                    response.getOutputStream().flush();
                    s.renderDirect(response.getOutputStream(), s, editUrl, editNodeId, user);

                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Error while rendering to output stream: ", e.getStackTrace());

                }

            } else {

                StringBuilder out = new StringBuilder();
                s.renderView(out, s, editUrl, editNodeId, user);

                // enable outbound url rewriting rules
                output = new StringBuilder(getContext().getResponse().encodeURL(out.toString()));


            }
        }
    }
}
