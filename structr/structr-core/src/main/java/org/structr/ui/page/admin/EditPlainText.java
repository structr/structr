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
import java.util.Map;

import org.apache.click.control.Form;
import org.apache.click.control.HiddenField;
import org.apache.click.control.Panel;
import org.apache.click.control.Submit;
import org.apache.click.control.TextArea;
import org.apache.click.control.TextField;
import org.apache.click.util.*;
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
    protected Form editContentForm = new Form("editContentForm");
    protected TextArea textArea;
    protected String textType;
    protected Panel editPlainTextPanel = new Panel("editPlainTextPanel", "/panel/edit-plain-text-panel.htm");

    public EditPlainText() {

        super();

        editPropertiesForm.add(new TextField(PlainText.Key.contentType.name(), "Internet Media Type (Content-Type)", 30));

        addControl(editPlainTextPanel);

        textArea = new TextArea(PlainText.Key.content.name(), false) {

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

        addControl(editContentForm);

    }

    @Override
    public void onInit() {

        super.onInit();

        if (node == null) {
            return;
        }

        // set text type (e.g. css)
        String contentType = node.getContentType();
        if (contentType != null && contentType.indexOf('/') > 0) {
            textType = contentType.split("/")[1];
            addModel("textType", textType);
        }

        if (editContentForm.isValid()) {
            editContentForm.copyFrom(node);
        }

        // set node id
        editContentForm.add(new HiddenField(NODE_ID_KEY, nodeId != null ? nodeId : ""));
        editContentForm.add(new HiddenField(RENDER_MODE_KEY, renderMode != null ? renderMode : ""));
        editContentForm.setActionURL(editPropertiesForm.getActionURL().concat("#content-tab"));
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

        return redirect();
    }

    /**
     * Save content
     */
    private void saveContent() {

        Services.command(TransactionCommand.class).execute(new StructrTransaction() {

            @Override
            public Object execute() throws Throwable {

                if (node != null) {

                    if (editContentForm.isValid()) {
                        editContentForm.copyTo(node);
                    }
                }
                return null;
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
