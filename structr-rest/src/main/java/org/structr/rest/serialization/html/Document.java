package org.structr.rest.serialization.html;

import java.io.IOException;
import java.io.PrintWriter;

/**
 *
 * @author Christian Morgner
 */
public class Document extends Tag {

	private PrintWriter writer = null;
	
	public Document(final PrintWriter writer) {
		super(null, "html", false, true);
		
		this.writer = writer;
	}
	
	public void setIndent(final String indent) {
		
	}
	
	public void flush() {
		writer.flush();
	}
	
	public void render() throws IOException {
		
		writer.println("<!DOCTYPE html>");
		
		render(writer, 0);
	}
}
