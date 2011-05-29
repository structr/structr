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

import org.apache.click.control.FieldSet;
import org.apache.click.control.PageLink;
import org.apache.click.control.TextField;
import org.apache.click.extras.control.LongField;
import org.structr.core.entity.File;

/**
 *
 * @author amorgner
 */
public class EditFile extends DefaultEdit {

    protected TextField contentTypeField = new TextField(File.CONTENT_TYPE_KEY, "Internet Media Type (Content-Type)", 30);
    protected TextField urlField = new TextField(File.URL_KEY, "URL", 100);
    protected TextField relativeFilePathField = new TextField(File.RELATIVE_FILE_PATH_KEY, "Local File Path", 100);
    protected LongField sizeField = new LongField(File.SIZE_KEY, "Size", 10);

    public EditFile() {

        super();

        sizeField.setReadonly(true);

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
        sizeField.setTextAlign("right");
        sizeField.setReadonly(true);
        infoFields.add(sizeField);

        editPropertiesForm.add(infoFields);
        addControl(editPropertiesForm);
    }

    @Override
    public void onInit() {

        super.onInit();

        if (node != null) {

            externalViewUrl = "/view/" + node.getId();
            localViewUrl = getContext().getRequest().getContextPath().concat(
                    "/view".concat(
                    node.getNodePath().replace("&", "%26")));
        }
    }
}
