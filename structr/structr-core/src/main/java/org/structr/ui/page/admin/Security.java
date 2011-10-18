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

import java.util.LinkedList;
import java.util.List;
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
import org.structr.core.entity.AbstractNode;
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

        PickList allowed = new PickList(StructrRelationship.Key.allowed.name(), "Actions");
        allowed.setHeaderLabel("Denied", "Allowed");

        List<Option> optionList = new LinkedList<Option>();
        Option readOption = new Option(StructrRelationship.Key.read.name(), "Read");
        optionList.add(readOption);

        Option writeOption = new Option(StructrRelationship.Key.write.name(), "Write");
        optionList.add(writeOption);
        
        Option executeOption = new Option(StructrRelationship.Key.execute.name(), "Execute");
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

                List<Option> optionList = new LinkedList<Option>();

                List<AbstractNode> principals =  node.getSecurityPrincipals();
                for (AbstractNode p : principals) {
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

        Command transactionCommand = Services.command(securityContext, TransactionCommand.class);
        transactionCommand.execute(new StructrTransaction() {

            @Override
            public Object execute() throws Throwable {
                AbstractNode s = getNodeByIdOrPath(getNodeId());

                if (securityForm.isValid()) {
                    securityForm.copyTo(s);
                }

                okMsg = "Save action successful!"; // TODO: localize

                return (null);
            }
        });

    }
}
