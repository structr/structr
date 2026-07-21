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
package org.structr.test.storage.providers.s3;
import org.structr.storage.providers.s3.S3Errors;
import org.testng.annotations.Test;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;

import static org.testng.AssertJUnit.*;

/**
 * Unit tests for the concise S3 error summarizer - the SDK's verbose async
 * exception chains must collapse to a single readable line.
 */
public class S3ErrorsTest {

	@Test
	public void testConnectionRefusedIsConcise() {

		// the real chain when an endpoint is down: ExecutionException ->
		// SdkClientException -> (netty) ConnectException with host:port
		final Throwable error = new ExecutionException(
			SdkClientException.create(
				"Unable to execute HTTP request: Connection refused: /127.0.0.1:9000 (SDK Attempt Count: 4)",
				new ConnectException("Connection refused: /127.0.0.1:9000")));

		final String message = S3Errors.describe(error);

		assertEquals("Connection refused: /127.0.0.1:9000", message);
		assertFalse("Message must be a single line", message.contains("\n"));
		assertFalse("SDK retry-count noise must be stripped", message.contains("SDK Attempt Count"));
	}

	@Test
	public void testUnknownHostIsConcise() {

		final Throwable error = new ExecutionException(
			SdkClientException.create("Unable to execute HTTP request", new UnknownHostException("s3.example.invalid")));

		assertEquals("unknown host: s3.example.invalid", S3Errors.describe(error));
	}

	@Test
	public void testMissingBucketReportsStatusAndMessage() {

		final S3Exception s3Exception = (S3Exception) S3Exception.builder()
			.statusCode(404)
			.awsErrorDetails(AwsErrorDetails.builder().errorCode("NoSuchBucket").errorMessage("The specified bucket does not exist").build())
			.message("The specified bucket does not exist (Service: S3, Status Code: 404, ...)")
			.build();

		assertEquals("S3 request failed (HTTP 404): The specified bucket does not exist", S3Errors.describe(new ExecutionException(s3Exception)));
	}

	@Test
	public void testAccessDeniedReportsStatusAndMessage() {

		final S3Exception s3Exception = (S3Exception) S3Exception.builder()
			.statusCode(403)
			.awsErrorDetails(AwsErrorDetails.builder().errorCode("AccessDenied").errorMessage("Access Denied").build())
			.build();

		assertEquals("S3 request failed (HTTP 403): Access Denied", S3Errors.describe(new ExecutionException(s3Exception)));
	}

	@Test
	public void testGenericSdkErrorStripsRetryNoiseAndExtraLines() {

		final Throwable error = SdkClientException.create("Unable to execute HTTP request: boom (SDK Attempt Count: 2)\n\tat some.frame(x)");

		assertEquals("Unable to execute HTTP request: boom", S3Errors.describe(error));
	}

	@Test
	public void testNullIsHandled() {
		assertEquals("unknown error", S3Errors.describe(null));
	}
}
