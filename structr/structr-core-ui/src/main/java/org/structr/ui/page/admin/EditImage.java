/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
        if (image.getWidth() <= PREVIEW_WIDTH && image.getHeight() <= PREVIEW_HEIGHT) {
            previewImage = image;
        } else {
            previewImage = image.getScaledImage(user, PREVIEW_WIDTH, PREVIEW_HEIGHT);
        }

        externalPreviewViewUrl = previewImage.getNodeURL(user, contextPath);
        localPreviewViewUrl = contextPath + "/view.htm?nodeId=" + previewImage.getId();

    }
}
