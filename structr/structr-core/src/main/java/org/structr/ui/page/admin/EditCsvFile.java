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

import java.util.Map;
import java.util.Set;
import org.apache.click.control.FieldSet;
import org.apache.click.control.Form;
import org.apache.click.control.HiddenField;
import org.apache.click.control.Label;
import org.apache.click.control.Option;
import org.apache.click.control.Select;
import org.apache.click.control.Submit;
import org.structr.core.Services;
import org.structr.core.agent.ConversionTask;
import org.structr.core.agent.ProcessTaskCommand;
import org.structr.core.entity.CsvFile;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;
import org.structr.core.module.GetEntitiesCommand;
import org.structr.core.module.GetEntityClassCommand;

/**
 *
 * @author amorgner
 */
public class EditCsvFile extends EditFile {

    protected Submit createNodeListSubmit = new Submit("Create Node List", this, "onCreateNodeList");
    protected Form editContentForm = new Form("editContentForm");
    protected FieldSet conversionFields = new FieldSet("File Conversion");
    protected Select nodeTypeField = new Select(AbstractNode.Key.type.name(), "Select Node Type", true);

    protected CsvFile csvNode;

    public EditCsvFile() {

        super();
        conversionFields.add(new Label("A CSV file can be converted to a node list. The columns have to match exactly the node properties of the selected node type!"));
        nodeTypeField.add(new Option("", "--- Select Node Type ---"));

        Set<String> nodeTypes = ((Map<String, Class>) Services.command(GetEntitiesCommand.class).execute()).keySet();
        for (String className : nodeTypes) {
            Option o = new Option(className);
            nodeTypeField.add(o);
        }

        conversionFields.add(nodeTypeField);
        conversionFields.add(createNodeListSubmit);
        editContentForm.add(conversionFields);

    }

    @Override
    public void onInit() {

        super.onInit();

        if (node != null) {

            editContentForm.add(new HiddenField(AbstractNode.Key.nodeId.name(), getNodeId()));
            csvNode = (CsvFile) node;
        }

        addControl(editContentForm);

    }

    public boolean onCreateNodeList() {

        if (csvNode == null) {
            return true;
        }

        if (editContentForm.isValid()) {

            String className = nodeTypeField.getValue();
            if (className == null) {
                return true;
            }

	    User user = securityContext.getUser();
            Class nodeClass = (Class) Services.command(GetEntityClassCommand.class).execute(className);
            Services.command(ProcessTaskCommand.class).execute(new ConversionTask(user, csvNode, nodeClass));
//            Services.command(ConvertCsvToNodeListCommand.class).execute(user, csvNode, nodeClass);
        }
        return false;

    }
}
