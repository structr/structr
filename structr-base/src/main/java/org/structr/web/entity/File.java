package org.structr.web.entity;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;

import javax.activation.DataSource;
import java.io.OutputStream;
import java.util.Map;

public interface File extends AbstractFile, DataSource {

	String getXMLStructure(final SecurityContext securityContext) throws FrameworkException;
	Long doCSVImport(final SecurityContext securityContext, final Map<String, Object> parameters) throws FrameworkException;
	Long doXMLImport(final SecurityContext securityContext, final Map<String, Object> parameters) throws FrameworkException;
	Map<String, Object> getCSVHeaders(final SecurityContext securityContext, final Map<String, Object> parameters) throws FrameworkException;
	Map<String, Object> getFirstLines(final SecurityContext securityContext, final Map<String, Object> parameters);
	OutputStream getOutputStream(final boolean notifyIndexerAfterClosing, boolean append);
	boolean isTemplate();
	boolean dontCache();

	GraphObject getSearchContext(SecurityContext ctx, String searchTerm, int contextLength);

	void notifyUploadCompletion();
	void callOnUploadHandler(final SecurityContext ctx);
	void increaseVersion() throws FrameworkException;
	void setVersion(int version) throws FrameworkException;
	Integer getVersion();
	Integer getCacheForSeconds();
	Long getChecksum();
	Long getFileModificationDate();
	String getMd5();

	void setSize(Long size) throws FrameworkException;

	Folder getCurrentWorkingDir();
	String getFormattedSize();
	String getExtractedContent();
	String getPath();

	int getNumberOrDefault(Map<String, Object> data, String key, int defaultValue);

	void checkMoveBinaryContents(final StorageConfiguration newProvider);
	void checkMoveBinaryContents(final Folder previousParent, final Folder newParent);

	boolean doIndexing();

	Map<String, Object> extractStructure() throws FrameworkException;

	// ----- interface JavaScriptSource -----
	String getJavascriptLibraryCode();

	boolean isImmutable();

}
