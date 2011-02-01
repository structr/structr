/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.ui.page.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.click.Page;
import org.apache.click.control.Form;
import org.apache.click.control.HiddenField;
import org.apache.click.control.Option;
import org.apache.click.control.Select;
import org.apache.click.control.Submit;
import org.apache.click.dataprovider.DataProvider;
import org.apache.click.extras.control.PickList;
import org.apache.click.util.Bindable;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.StructrNode;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;

/**
 *
 * @author amorgner
 */
public class Security extends Nodes {

    /**
     * The main form for editing security parameter.
     * Child pages should just append fields to this form.
     */
    @Bindable
    protected Form securityForm = new Form("securityForm");
    @Bindable
    protected Select userSelect = new Select("selectUser", " Select User ");

    public Security() {

        securityForm.add(userSelect);

        PickList allowed = new PickList(StructrRelationship.ALLOWED_KEY, "Actions");
        allowed.setHeaderLabel("Denied", "Allowed");

        List<Option> optionList = new ArrayList<Option>();
        Option readOption = new Option(StructrRelationship.READ_KEY, "Read");
        optionList.add(readOption);

        Option writeOption = new Option(StructrRelationship.WRITE_KEY, "Write");
        optionList.add(writeOption);
        
        Option executeOption = new Option(StructrRelationship.EXECUTE_KEY, "Execute");
        optionList.add(executeOption);

        allowed.addAll(optionList);
        securityForm.add(allowed);

        securityForm.add(new Submit("save", " Save ", this, "onSave"));
        //securityForm.add(new Submit("saveAndView", " Save And View ", this, "onSaveAndView"));

    }

    @Override
    public void onInit() {

        userSelect.setDataProvider(new DataProvider() {

            @Override
            public List<Option> getData() {

                List<Option> optionList = new ArrayList<Option>();

                List<StructrNode> principals =  node.getSecurityPrincipals();
                for (StructrNode p : principals) {
                    Option o = new Option(p.getName());
                    optionList.add(o);
                }

                return optionList;
            }
        });

    }

    /**
     * @see Page#onRender()
     */
    @Override
    public void onRender() {

        super.onRender();

        securityForm.copyFrom(node);
        securityForm.add(new HiddenField(NODE_ID_KEY, nodeId != null ? nodeId : ""));

    }

    /**
     * Save form data and stay in edit mode (keep tab)
     *
     * @return
     */
    public boolean onSave() {

        save();
        return redirect();
    }


    /**
     * Save form data
     */
    private void save() {

        Command transactionCommand = Services.createCommand(TransactionCommand.class);
        transactionCommand.execute(new StructrTransaction() {

            @Override
            public Object execute() throws Throwable {
                StructrNode s = getNodeByIdOrPath(getNodeId());

                if (securityForm.isValid()) {
                    securityForm.copyTo(s);
                }

                okMsg = "Save action successful!"; // TODO: localize

                return (null);
            }
        });

    }
}
