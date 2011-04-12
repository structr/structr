/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.entity.app;

import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;

/**
 *
 * @author Axel Morgner
 */
public class PasswordField extends TextField implements InteractiveNode {

    private SessionValue<String> errorMessage = new SessionValue<String>("errorMessage", "");

    public PasswordField() {

        // reset error message
        errorMessage.set("");
    }

    @Override
    public void renderView(final StringBuilder out, final AbstractNode startNode, final String editUrl, final Long editNodeId, final User user) {

        String name = getName();
        String label = getLabel();
        Object value = getValue();

        if (label != null) {
            out.append("<div class=\"label\">").append(label).append("</div>");
        }
        
        out.append("<input");

        if (errorMessage.get().length() > 0) {
            out.append("class=\"error\")");
        }

        out.append(" type=\"password\" name=\"").append(name).append("\" value=\"").append(value != null ? value : "").append("\">");

    }

}
