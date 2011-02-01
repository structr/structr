/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.ui.page.admin;

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

        Image previewImage = image.getScaledImage(user, PREVIEW_WIDTH, PREVIEW_HEIGHT);

        if (previewImage != null) {
            externalPreviewViewUrl = previewImage.getNodeURL(user, contextPath);
            localPreviewViewUrl = contextPath + "/view.htm?nodeId=" + previewImage.getId();
        } else {
            externalPreviewViewUrl = contextPath + image.getIconSrc();
            localPreviewViewUrl = contextPath + image.getIconSrc();
        }

    }
}
