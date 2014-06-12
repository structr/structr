package org.structr.schema.action;

/**
 *
 * @author Christian Morgner
 */
public class ActionEntry implements Comparable<ActionEntry> {

	private boolean runOnError = true;
	private Actions.Type type  = null;
	private String call        = null;
	private String name        = null;
	private int position       = 0;

	public ActionEntry(final String sourceName, final String value, final boolean runOnError) {

		int positionOffset = 0;

		if (sourceName.startsWith("___onSave")) {

			this.type = Actions.Type.Save;
			positionOffset = 9;

		} else if (sourceName.startsWith("___onCreate")) {

			this.type = Actions.Type.Create;
			positionOffset = 11;

		} else if (sourceName.startsWith("___onDelete")) {

			this.type = Actions.Type.Delete;
			positionOffset = 11;

		} else {

			this.type = Actions.Type.Custom;
			positionOffset = 3;
		}

		if (type.equals(Actions.Type.Custom)) {

			this.name = sourceName.substring(positionOffset);

		} else {
			// try to identify a position
			final String positionString = sourceName.substring(positionOffset);
			if (!positionString.isEmpty()) {

				try { position = Integer.parseInt(positionString); } catch (Throwable t) { /* ignore */ }
			}
		}

		this.call       = value.trim();
		this.runOnError = runOnError;
	}

	public String getSource() {

		final StringBuilder buf = new StringBuilder();

		buf.append(Actions.class.getSimpleName());
		buf.append(".execute(securityContext, this, \"${");
		buf.append(replaceQuotes(call));
		buf.append("}\")");

		return buf.toString();
	}

	@Override
	public int compareTo(ActionEntry o) {
		return getPosition().compareTo(o.getPosition());
	}

	public Actions.Type getType() {
		return type;
	}

	public String getCall() {
		return call;
	}

	public Integer getPosition() {
		return position;
	}

	public boolean runOnError() {
		return runOnError;
	}

	public String getName() {
		return name;
	}

	// ----- private methods -----
	private String replaceQuotes(final String source) {

		String result = source;

		result = result.replaceAll("\"", "\\\\\"");
		result = result.replaceAll("\'", "\\\\\'");
		result = result.replaceAll("\n", "\\\\n");

		return result;
	}
}

