/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.entity.app;

import javax.servlet.http.HttpServletRequest;
import org.structr.common.StructrContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;

/**
 *
 * @author Christian Morgner
 */
public class TextField extends FormField implements InteractiveNode {

    private SessionValue<String> errorMessage = new SessionValue<String>("errorMessage", "");
    private String mappedName = null;

    public TextField() {

        // reset error message
        errorMessage.set("");
    }

    @Override
    public String getIconSrc() {
        return "/images/textfield.png";
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

        out.append(" type=\"text\" name=\"").append(name).append("\" value=\"").append(value != null ? value : "").append("\">");

        if (errorMessage.get().length() > 0) {
            out.append(errorMessage.get());
        }

    }

    // ----- interface InteractiveNode -----
    @Override
    public String getValue() {
        HttpServletRequest request = StructrContext.getRequest();
        String name = getName();
        String ret = null;

        if (request != null) {
            ret = request.getParameter(name);
            if (ret != null && ret.length() == 0) {
                ret = null;
            }
        }

        return (ret);
    }

    @Override
    public Class getParameterType() {
        return (String.class);
    }

    @Override
    public void setMappedName(String mappedName) {
        this.mappedName = mappedName;
    }

    @Override
    public String getMappedName() {
        if (this.mappedName != null) {
            return (mappedName);
        }

        return (getName());
    }

    @Override
    public void setErrorCondition(boolean error) {
        if (error) {
            errorMessage.set("Error");
        }
    }
}
