/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.ui.page.admin;

import org.structr.core.entity.StructrNode;
import java.util.HashMap;
import java.util.Map;

import org.apache.click.control.Form;
import org.apache.click.control.HiddenField;
import org.apache.click.control.Panel;
import org.apache.click.control.Submit;
import org.apache.click.control.TextArea;
import org.apache.click.util.*;
import org.structr.core.entity.web.Page;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.ui.page.admin.DefaultEdit;
import org.structr.ui.page.admin.DefaultEdit;

/**
 * Edit text.
 * 
 * @author amorgner
 */
public class EditScript extends DefaultEdit {

    @Bindable
    protected Form editContentForm = new Form("editContentForm");
    @Bindable
    protected TextArea textArea;
    @Bindable
    protected String textType;
    @Bindable
    protected Panel editScriptPanel = new Panel("editScriptPanel", "/panel/edit-script-panel.htm");
    private static String textAreaName = "text";

    public EditScript() {

        textArea = new TextArea(getTextAreaName(), false) {

            // override render method: don't include cols and rows attributes
            @Override
            public void render(HtmlStringBuffer buffer) {
                buffer.elementStart(textArea.getTag());

                buffer.appendAttribute("name", getName());
                buffer.appendAttribute("id", getId());

                // FIXME: hard-coded values
                buffer.appendAttribute("rows", 25);
                buffer.appendAttribute("cols", 80);


                buffer.appendAttribute("title", getTitle());
                if (isValid()) {
                    removeStyleClass("error");
                    if (isDisabled()) {
                        addStyleClass("disabled");
                    } else {
                        removeStyleClass("disabled");
                    }
                } else {
                    addStyleClass("error");
                }
                if (getTabIndex() > 0) {
                    buffer.appendAttribute("tabindex", getTabIndex());
                }

                appendAttributes(buffer);

                if (isDisabled()) {
                    buffer.appendAttributeDisabled();
                }
                if (isReadonly()) {
                    buffer.appendAttributeReadonly();
                }

                buffer.closeTag();

                buffer.appendEscaped(getValue());

                buffer.elementEnd(getTag());

                if (getHelp() != null) {
                    buffer.append(getHelp());
                }

            }
        };

        textArea.setId("code"); // needed for CodeMirror
        // limit maximum length to 1 MB
        textArea.setMaxLength(1024 * 1024);

        editContentForm.add(textArea);
        editContentForm.add(new Submit("save", " Save ", this, "onSave"));
//        editContentForm.add(new Submit("saveAndView", " Save and View ", this, "onSaveAndView"));

    }

    /**
     * @see Page#onRender()
     */
    @Override
    public void onRender() {

        super.onRender();

//        Command transactionCommand = Services.createCommand(TransactionCommand.class);
//        transactionCommand.execute(new StructrTransaction() {
//
//            @Override
//            public Object execute() throws Throwable {
                StructrNode s = getNodeByIdOrPath(getNodeId());

                if (editContentForm.isValid()) {
                    editContentForm.copyFrom(s);
                }

                // set text type (e.g. css)
                String mimeType = s.getContentType();
                if (mimeType != null && mimeType.indexOf('/') > 0) {
                    textType = mimeType.split("/")[1];
                }

//                return (null);
//            }
//        });

        // set node id
        if (editContentForm.isValid()) {
            editContentForm.add(new HiddenField(NODE_ID_KEY, nodeId != null ? nodeId : ""));
            editContentForm.add(new HiddenField(RENDER_MODE_KEY, renderMode != null ? renderMode : ""));
        }

    }

    @Override
    public boolean onSaveProperties() {
        Command transactionCommand = Services.createCommand(TransactionCommand.class);
        transactionCommand.execute(new StructrTransaction() {

            @Override
            public Object execute() throws Throwable {
                StructrNode s = getNodeByIdOrPath(getNodeId());

                if (editContentForm.isValid()) {
                    editContentForm.copyTo(s);
                }

                okMsg = "Save action successful!"; // TODO: localize

                return (null);
            }
        });

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(NODE_ID_KEY, nodeId.toString());
        setRedirect(this.getPath(), parameters);

        return false;
    }
//
//    @Override
//    public boolean onSaveAndView() {
//        Command transactionCommand = Services.createCommand(TransactionCommand.class);
//        transactionCommand.execute(new StructrTransaction() {
//
//            @Override
//            public Object execute() throws Throwable {
//                StructrNode s = getNodeByIdOrPath(getNodeId());
//
//                if (editContentForm.isValid()) {
//                    editContentForm.copyTo(s);
//                }
//
//                okMsg = "Save action successful!"; // TODO: localize
//
//                return (null);
//            }
//        });
//
//        Map<String, String> parameters = new HashMap<String, String>();
//        parameters.put(NODE_ID_KEY, nodeId);
//        setRedirect("view-script.htm", parameters);
//
//        return false;
//    }

    protected String getTextAreaName() {
        return EditScript.textAreaName;
    }
}
