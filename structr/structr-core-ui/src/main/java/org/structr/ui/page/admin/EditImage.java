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
import org.apache.click.extras.control.LongField;
import org.apache.click.util.Bindable;
import org.structr.core.entity.Image;

/**
 *
 * @author amorgner
 */
public class EditImage extends EditFile {

    private Image image;
    @Bindable
    protected String externalThumbnailViewUrl;
    @Bindable
    protected String localThumbnailViewUrl;
    @Bindable
    protected String externalPreviewViewUrl;
    @Bindable
    protected String localPreviewViewUrl;
    protected LongField widthField = new LongField(Image.WIDTH_KEY);
    protected LongField heightField = new LongField(Image.HEIGHT_KEY);

    public EditImage() {

        super();

        FieldSet imageInfoFields = new FieldSet("Image Information");
        imageInfoFields.add(widthField);
        imageInfoFields.add(heightField);

        editPropertiesForm.add(imageInfoFields);
        addControl(editPropertiesForm);
    }

    @Override
    public void onInit() {

        super.onInit();

        image = (Image) node;
    }

    @Override
    public void onRender() {

        super.onRender();

//        Image thumbnailImage = image.getScaledImage(user, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
//        externalThumbnailViewUrl = thumbnailImage.getNodeURL(user, contextPath);
//        localThumbnailViewUrl = contextPath + "/view.htm?nodeId=" + thumbnailImage.getId();

        Image previewImage;

        // If original image is smaller than requested size,
        // display original image as preview
        if (image.isThumbnail() || (image.getWidth() <= PREVIEW_WIDTH && image.getHeight() <= PREVIEW_HEIGHT)) {
            previewImage = image;
        } else {
            previewImage = image.getScaledImage(PREVIEW_WIDTH, PREVIEW_HEIGHT);
        }
        
        if (previewImage != null) {
            externalPreviewViewUrl = previewImage.getNodeURL(contextPath);
            localPreviewViewUrl = contextPath + "/view.htm?nodeId=" + previewImage.getId();
        }

    }
}
