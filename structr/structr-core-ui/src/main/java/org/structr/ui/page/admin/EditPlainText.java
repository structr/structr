/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.ui.page.admin;

import org.structr.core.entity.AbstractNode;
import java.util.HashMap;
import java.util.Map;

import org.apache.click.control.Form;
import org.apache.click.control.HiddenField;
import org.apache.click.control.Panel;
import org.apache.click.control.Submit;
import org.apache.click.control.TextArea;
import org.apache.click.util.*;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.PlainText;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;

/**
 * Edit text.
 * 
 * @author amorgner
 */
public class EditPlainText extends DefaultEdit {

    /**
     * The main form for editing node content.
     * Child pages should just append fields to this form.
     */
    @Bindable
    protected Form editContentForm = new Form("editContentForm");
    @Bindable
    protected TextArea textArea;
    @Bindable
    protected String textType;
    @Bindable
    protected Panel editPlainTextPanel = new Panel("editPlainTextPanel", "/panel/edit-plain-text-panel.htm");

    public EditPlainText() {

        textArea = new TextArea(PlainText.CONTENT_KEY, false) {

            // override render method: don't include cols and rows attributes
            @Override
            public void render(HtmlStringBuffer buffer) {
                buffer.elementStart(textArea.getTag());

                buffer.appendAttribute("name", getName());
                buffer.appendAttribute("id", getId());
                //buffer.appendAttribute("rows", getRows());
                //buffer.appendAttribute("cols", getCols());
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
        textArea.setLabelStyle("display: none;");

        editContentForm.add(textArea);
        editContentForm.setActionURL(editContentForm.getActionURL() + "#content-tab");
        editContentForm.add(new Submit("save", " Save ", this, "onSaveContent"));
//        editContentForm.add(new Submit("saveAndView", " Save and View ", this, "onSaveAndView"));



    }

    @Override
    public void onRender() {

        super.onRender();

//        Command transactionCommand = Services.command(TransactionCommand.class);
//        transactionCommand.execute(new StructrTransaction() {
//
//            @Override
//            public Object execute() throws Throwable {
                AbstractNode s = getNodeByIdOrPath(getNodeId());

                if (editPropertiesForm.isValid()) {
                    editPropertiesForm.copyFrom(s);
                }

                // set text type (e.g. css)
                String contentType = s.getContentType();
                if (contentType != null && contentType.indexOf('/') > 0) {
                    textType = contentType.split("/")[1];
                }

                if (editContentForm.isValid()) {
                    editContentForm.copyFrom(s);
                }

//                return (null);
//            }
//        });

        // set node id
        if (editPropertiesForm.isValid()) {
            editPropertiesForm.add(new HiddenField(NODE_ID_KEY, nodeId != null ? nodeId : ""));
            editPropertiesForm.add(new HiddenField(RENDER_MODE_KEY, renderMode != null ? renderMode : ""));
        }

        // set node id
        if (editContentForm.isValid()) {
            editContentForm.add(new HiddenField(NODE_ID_KEY, nodeId != null ? nodeId : ""));
            editContentForm.add(new HiddenField(RENDER_MODE_KEY, renderMode != null ? renderMode : ""));
        }
    }

    /**
     * Save form data and stay in edit mode
     *
     * @return
     */
    public boolean onSaveContent() {

        saveContent();

        okMsg = "Node content successfully saved.";

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(NODE_ID_KEY, nodeId);
        setRedirect(getPath(), parameters);

        return false;
    }

    /**
     * Save content
     */
    private void saveContent() {
        Command transactionCommand = Services.command(TransactionCommand.class);
        transactionCommand.execute(new StructrTransaction() {

            @Override
            public Object execute() throws Throwable {
                AbstractNode s = getNodeByIdOrPath(getNodeId());

                if (editContentForm.isValid()) {
                    editContentForm.copyTo(s);
                }

                return (null);
            }
        });

    }
//
//    @Override
//    public boolean onSaveAndView() {
//        Command transactionCommand = Services.command(TransactionCommand.class);
//        transactionCommand.execute(new StructrTransaction() {
//
//            @Override
//            public Object execute() throws Throwable {
//                AbstractNode s = getNodeByIdOrPath(getNodeId());
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
//        parameters.put(NODE_ID_KEY, nodeId.toString());
//        setRedirect(getEditPageClass(node), parameters);
////        setRedirect(getViewPageClass(node), parameters);
//
//        return false;
//    }
}
