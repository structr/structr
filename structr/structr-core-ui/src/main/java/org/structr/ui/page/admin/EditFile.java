/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.ui.page.admin;

import org.apache.click.control.FieldSet;
import org.apache.click.control.PageLink;
import org.apache.click.control.TextField;
import org.apache.click.extras.control.LongField;
import org.apache.click.util.Bindable;
import org.structr.core.entity.File;

/**
 *
 * @author amorgner
 */
public class EditFile extends DefaultEdit {

    @Bindable
    protected TextField contentTypeField = new TextField(File.CONTENT_TYPE_KEY, "Internet Media Type (Content-Type)", 30);
    @Bindable
    protected TextField urlField = new TextField(File.URL_KEY, "URL", 100);
    @Bindable
    protected TextField relativeFilePathField = new TextField(File.RELATIVE_FILE_PATH_KEY, "Local File Path", 100);
    @Bindable
    protected LongField sizeField = new LongField(File.SIZE_KEY, "Size (Bytes)", 10);

    public EditFile() {

        super();

        FieldSet infoFields = new FieldSet("File Information");
        infoFields.add(new PageLink("download") {
            @Override
            public String getHref() {
                return localViewUrl;
            }
        });
        infoFields.add(contentTypeField);
        infoFields.add(urlField);
        infoFields.add(relativeFilePathField);
        infoFields.add(sizeField);

        editPropertiesForm.add(infoFields);
    }

    @Override
    public void onInit() {

        super.onInit();

        externalViewUrl = node.getNodeURL(user, contextPath);
//        //localViewUrl = getContext().getResponse().encodeURL(viewLink.getHref());
        localViewUrl = getContext().getRequest().getContextPath().concat(
                "/view".concat(
                node.getNodePath(user).replace("&", "%26")));


        // FIXME: find a good solution for file download
//                ByteArrayOutputStream out = new ByteArrayOutputStream();
//                node.renderDirect(out, rootNode, redirect, editNodeId, user);
//                rendition = out.toString();
        // provide rendition's source
        //source = ClickUtils.escapeHtml(rendition);

        //renditionPanel = new Panel("renditionPanel", "/panel/rendition-panel.htm");
    }
}
