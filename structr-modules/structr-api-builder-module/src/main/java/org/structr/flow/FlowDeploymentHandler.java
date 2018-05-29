package org.structr.flow;

import com.google.gson.Gson;
import org.structr.common.error.FrameworkException;

import java.nio.file.Path;

public abstract class FlowDeploymentHandler {

	public static void exportDeploymentData (final Path target, final Gson gson) throws FrameworkException {

	}

	public static void importDeploymentData (final Path source, final Gson gson) throws FrameworkException {

	}
}
