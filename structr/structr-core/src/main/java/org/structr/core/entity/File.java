package org.structr.core.entity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;

import org.apache.commons.io.IOUtils;
import org.structr.common.Path;
import org.structr.core.Services;

/**
 * 
 * @author amorgner
 * 
 */
public class File extends AbstractNode {

    private final static String ICON_SRC = "/images/page_white.png";
    private static final Logger logger = Logger.getLogger(File.class.getName());
    public final static String URL_KEY = "url";
    public final static String CONTENT_TYPE_KEY = "contentType";
    public final static String SIZE_KEY = "size";
//    public final static String FORMATTED_SIZE_KEY = "formattedSize";
    public final static String RELATIVE_FILE_PATH_KEY = "relativeFilePath";

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }

    public String getUrl() {
        return (String) getProperty(URL_KEY);
    }

    @Override
    public String getContentType() {
        return (String) getProperty(CONTENT_TYPE_KEY);
    }

    public long getSize() {

        String relativeFilePath = getRelativeFilePath();

        if (relativeFilePath != null) {

            String filePath = Services.getFilePath(Path.Files, relativeFilePath);

            java.io.File fileOnDisk = new java.io.File(filePath);
            long fileSize = fileOnDisk.length();

            logger.log(Level.INFO, "File size of node {0} ({1}): {2}", new Object[]{getId(), filePath, fileSize});

            return fileSize;
        }
        return 0;
    }

    public String getFormattedSize() {
        return FileUtils.byteCountToDisplaySize(getSize());
    }

    public String getRelativeFilePath() {
        return (String) getProperty(RELATIVE_FILE_PATH_KEY);
    }

    public void setRelativeFilePath(final String filePath) {
        setProperty(RELATIVE_FILE_PATH_KEY, filePath);
    }

    public void setUrl(final String url) {
        setProperty(URL_KEY, url);
    }

    public void setContentType(final String contentType) {
        setProperty(CONTENT_TYPE_KEY, contentType);
    }

    public void setSize(final long size) {
        setProperty(SIZE_KEY, size);
    }

    public URL getFileLocation() {
        String urlString = "file://" + Services.getFilesPath() + "/" + getRelativeFilePath();
        try {
            return new URL(urlString);
        } catch (MalformedURLException mue) {
            logger.log(Level.SEVERE, "Invalid URL: {0}", urlString);
        }
        return null;
    }

    public InputStream getInputStream() {

        URL url = null;
        try {
            url = getFileLocation();
            return url.openStream();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error while reading from {0}", new Object[]{url, e.getMessage()});
        }

        return null;

    }

    /**
     * Stream content directly to output.
     *
     * @param out
     */
    @Override
    public void renderDirect(OutputStream out, final AbstractNode startNode,
            final String editUrl, final Long editNodeId, final User user) {

        if (isVisible(user)) {
            try {

                InputStream in = getInputStream();

                if (in != null) {
                    // just copy to output stream
                    IOUtils.copy(in, out);
                }

            } catch (Throwable t) {
                logger.log(Level.SEVERE, "Error while rendering file", t);
            }
        }
    }
}
