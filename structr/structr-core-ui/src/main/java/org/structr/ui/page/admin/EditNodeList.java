/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.ui.page.admin;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.click.control.ActionLink;
import org.apache.click.control.FieldSet;
import org.apache.click.control.Label;
import org.structr.core.Services;
import org.structr.core.entity.NodeList;
import org.structr.core.entity.StructrNode;
import org.structr.core.node.TestNodeCommand;

/**
 *
 * @author chrisi
 */
public class EditNodeList extends DefaultEdit
{
	Logger logger = Logger.getLogger(EditNodeList.class.getName());
	protected FieldSet fields = new FieldSet("NodeList test");

	public EditNodeList()
	{
		fields.add(new ActionLink("add", "Add node to list", this, "onAddClick"));
		fields.add(new ActionLink("del", "Delete last node from list", this, "onDelClick"));

		NodeList<StructrNode> list = (NodeList<StructrNode>)Services.createCommand(TestNodeCommand.class).execute();
		if(list != null)
		{
			fields.add(new Label("label", "Size: " + list.size()));

			for(StructrNode n : list)
			{
				if(n != null)
				{
					fields.add(new Label("label" + n.getId(), n.toString()));
				}

			}
		}

		editPropertiesForm.add(fields);
	}

	public void onAddClick()
	{
		logger.log(Level.INFO, "Adding node..");
		Services.createCommand(TestNodeCommand.class).execute("add");
	}

	public void onDelClick()
	{
		logger.log(Level.INFO, "Removing node");
		Services.createCommand(TestNodeCommand.class).execute("del");
	}
}
