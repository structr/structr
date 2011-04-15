/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.entity.app;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;

/**
 *
 * @author Christian Morgner
 */
public class SubmitButton extends FormField {

    private static final Logger logger = Logger.getLogger(SubmitButton.class.getName());

    @Override
    public String getIconSrc() {
        return "/images/tag.png";
    }

    @Override
    public void renderView(StringBuilder out, AbstractNode startNode, String editUrl, Long editNodeId, User user) {
        // if this page is requested to be edited, render edit frame
        if (editNodeId != null && getId() == editNodeId.longValue()) {

            renderEditFrame(out, editUrl);

            // otherwise, render subnodes in edit mode
        } else {

            if (hasTemplate(user)) {
                template.setCallingNode(this);
                template.renderView(out, startNode, editUrl, editNodeId, user);

            } else {
                logger.log(Level.WARNING, "Encountered TextField without template: {0}", this);

                // TODO: default template for TextField?
            }
        }
    }
}
