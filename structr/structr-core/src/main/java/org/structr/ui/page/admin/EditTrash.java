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
package org.structr.ui.page.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.click.control.Form;
import org.apache.click.control.HiddenField;
import org.apache.click.control.Submit;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Trash;
import org.structr.core.entity.User;
import org.structr.core.node.DeleteNodeCommand;
import org.structr.core.notification.AddNotificationCommand;
import org.structr.core.notification.SuccessNotification;

/**
 *
 * @author amorgner
 */
public class EditTrash extends EditFolder {

    private Trash trash;
    protected Form trashForm = new Form("trashForm");
    protected Submit emptyTrashSubmit = new Submit("emptyTrash", " Empty Trash ", this, "onEmptyTrash");

    public EditTrash() {
        super();
    }

    @Override
    public void onInit() {

        super.onInit();

        if (node != null && node instanceof Trash) {
            trash = (Trash) node;
        }

        trashForm.add(new HiddenField(NODE_ID_KEY, nodeId != null ? nodeId : ""));
        trashForm.add(emptyTrashSubmit);

        if (editChildNodesPanel != null) {
            editChildNodesPanel.add(trashForm);
        }

    }

    public boolean onEmptyTrash() {

        if (trash != null) {

            Command deleteNode = Services.command(DeleteNodeCommand.class);
	    User user = securityContext.getUser();

            List<AbstractNode> children = trash.getDirectChildNodes();
            for (AbstractNode nodeToDelete : children) {

                // Delete subnodes recursively
                deleteNode.execute(nodeToDelete, null, true, user);
            }

            List<AbstractNode> links = trash.getLinkedNodes();
            for (AbstractNode nodeToDelete : links) {

                // Delete links
                deleteNode.execute(nodeToDelete, trash, true, user);
            }

        }

        // assemble feedback message
        okMsg = "Trash emptied";

	Services.command(AddNotificationCommand.class).execute(new SuccessNotification(securityContext, okMsg));

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(NODE_ID_KEY, String.valueOf(getNodeId()));
        parameters.put(RENDER_MODE_KEY, renderMode);
        parameters.put(OK_MSG_KEY, okMsg);
        setRedirect(getRedirectPage(node), parameters);

        return false;

    }
}
