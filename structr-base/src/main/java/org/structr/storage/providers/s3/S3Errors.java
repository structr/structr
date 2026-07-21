/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.storage.providers.s3;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Turns the AWS SDK's deeply nested, multi-line async exception chains into
 * a single concise, human-readable line - the raw SDK stack traces (retry
 * attempts, Netty connect frames, completable-future plumbing) are useless
 * to an end user and drown the logs when an endpoint is unreachable or a
 * bucket is missing.
 */
final public class S3Errors {

	private S3Errors() {
	}

	/**
	 * @return a concise, single-line description of the most meaningful cause
	 * in the given error chain
	 */
	public static String describe(final Throwable error) {

		if (error == null) {
			return "unknown error";
		}

		S3Exception s3Exception            = null;
		SdkException sdkException           = null;
		UnknownHostException unknownHost    = null;
		ConnectException connectException   = null;

		final Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());

		for (Throwable current = error; current != null && seen.add(current); current = current.getCause()) {

			if (s3Exception == null && current instanceof S3Exception e) {
				s3Exception = e;
			}

			if (sdkException == null && current instanceof SdkException e) {
				sdkException = e;
			}

			if (unknownHost == null && current instanceof UnknownHostException e) {
				unknownHost = e;
			}

			if (connectException == null && current instanceof ConnectException e) {
				connectException = e;
			}
		}

		// a real S3 service response (bucket missing, access denied, ...)
		if (s3Exception != null) {

			final AwsErrorDetails details = s3Exception.awsErrorDetails();
			final String message          = details != null ? details.errorMessage() : null;
			final int status              = s3Exception.statusCode();

			final StringBuilder result = new StringBuilder("S3 request failed");

			if (status > 0) {
				result.append(" (HTTP ").append(status).append(")");
			}

			if (message != null && !message.isBlank()) {
				result.append(": ").append(message);
			}

			return result.toString();
		}

		// connectivity problems
		if (unknownHost != null) {
			return "unknown host: " + firstLine(unknownHost.getMessage());
		}

		if (connectException != null) {
			return firstLine(connectException.getMessage());
		}

		// generic SDK client error (e.g. "Unable to execute HTTP request: ...")
		if (sdkException != null) {
			return firstLine(sdkException.getMessage());
		}

		return firstLine(error.getMessage());
	}

	// ----- private methods -----
	private static String firstLine(final String message) {

		if (message == null || message.isBlank()) {
			return "unknown error";
		}

		String line = message;

		final int newline = line.indexOf('\n');
		if (newline >= 0) {
			line = line.substring(0, newline);
		}

		// drop the SDK's noisy retry-count suffix
		final int attemptCount = line.indexOf(" (SDK Attempt Count:");
		if (attemptCount >= 0) {
			line = line.substring(0, attemptCount);
		}

		return line.trim();
	}
}
