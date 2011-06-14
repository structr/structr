/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.entity.app;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.StructrOutputStream;
import org.structr.core.entity.AbstractNode;

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
    public void renderNode(StructrOutputStream out, final AbstractNode startNode, final String editUrl, final Long editNodeId) {
        // if this page is requested to be edited, render edit frame
        if (editNodeId != null && getId() == editNodeId.longValue()) {

            renderEditFrame(out, editUrl);

            // otherwise, render subnodes in edit mode
        } else {

            if (hasTemplate()) {
                template.setCallingNode(this);
                template.renderNode(out, startNode, editUrl, editNodeId);

            } else {
                logger.log(Level.WARNING, "Encountered TextField without template: {0}", this);

                // TODO: default template for TextField?
            }
        }
    }

	@Override
	public String getErrorMessage()
	{
		return(null);
	}

	@Override
	public Object getErrorValue()
	{
		return(null);
	}

	@Override
	public void setErrorValue(Object errorValue)
	{
	}
}
