package org.structr.common.error;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 *
 * @author Christian Morgner
 */
public class DiagnosticErrorToken extends SemanticErrorToken {

	private Diagnostic<? extends JavaFileObject> diagnostic = null;
	
	public DiagnosticErrorToken(final Diagnostic<? extends JavaFileObject> diagnostic) {
		
		super(base);
		
		this.diagnostic = diagnostic;
	}

	@Override
	public JsonElement getContent() {
		
		final JsonObject obj = new JsonObject();

		obj.add(diagnostic.getKind().name(), new JsonPrimitive(getErrorToken()));
		
		return obj;
	}

	@Override
	public String getErrorToken() {
		return diagnostic.getMessage(Locale.ENGLISH);
	}
}
