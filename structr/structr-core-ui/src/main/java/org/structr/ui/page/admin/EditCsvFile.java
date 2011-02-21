/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
import org.structr.core.entity.StructrNode;
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
    protected Select nodeTypeField = new Select(StructrNode.TYPE_KEY, "Select Node Type", true);

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

            editContentForm.add(new HiddenField(StructrNode.NODE_ID_KEY, getNodeId()));
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

            Class nodeClass = (Class) Services.command(GetEntityClassCommand.class).execute(className);
            Services.command(ProcessTaskCommand.class).execute(new ConversionTask(user, csvNode, nodeClass));
//            Services.command(ConvertCsvToNodeListCommand.class).execute(user, csvNode, nodeClass);
        }
        return false;

    }
}
