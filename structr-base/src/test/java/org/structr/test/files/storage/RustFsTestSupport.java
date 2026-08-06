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
package org.structr.test.files.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.storage.providers.s3.S3ClientCache;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Shared rustfs (S3-compatible object storage) test container plus SDK
 * helpers for out-of-band bucket access in tests. Started once per test
 * class (test classes run in separate forks); hard-fails when Docker is
 * not available, like the Keycloak integration test.
 */
final class RustFsTestSupport {

	static final String ACCESS_KEY = "structr-test-access";
	static final String SECRET_KEY = "structr-test-secret";
	static final String REGION     = "us-east-1";

	private static final Logger logger = LoggerFactory.getLogger(RustFsTestSupport.class);

	private static GenericContainer<?> container = null;
	private static String endpoint               = null;
	private static S3AsyncClient client          = null;

	private RustFsTestSupport() {
	}

	static synchronized void start() {

		if (container != null) {

			return;
		}

		container = new GenericContainer<>("rustfs/rustfs:latest")
			.withExposedPorts(9000)
			.withEnv("RUSTFS_ACCESS_KEY", ACCESS_KEY)
			.withEnv("RUSTFS_SECRET_KEY", SECRET_KEY)
			.waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)));

		container.start();

		endpoint = "http://" + container.getHost() + ":" + container.getMappedPort(9000);

		// sharing the production client cache doubles as a smoke test of it
		client = S3ClientCache.getOrCreate(endpoint, REGION, ACCESS_KEY, SECRET_KEY);

		logger.info("rustfs test container running at {}", endpoint);
	}

	static synchronized void stop() {

		if (container != null) {

			container.stop();
			container = null;
			endpoint  = null;
			client    = null;
		}
	}

	static String getEndpoint() {

		return endpoint;
	}

	static Map<String, String> providerConfig(final String bucket) {

		return Map.of("endpoint",   endpoint, "bucketName", bucket, "region",     REGION, "accessKey",  ACCESS_KEY, "secretKey",  SECRET_KEY);
	}

	/**
	 * Creates the given bucket, retrying for up to ~30 seconds - the first
	 * request also acts as a functional readiness check for the container.
	 */
	static void createBucket(final String name) {

		final long deadline = System.currentTimeMillis() + 30_000;
		Throwable lastError = null;

		while (System.currentTimeMillis() < deadline) {

			try {

				client.createBucket(CreateBucketRequest.builder().bucket(name).build()).get();

				return;

			} catch (InterruptedException iex) {

				Thread.currentThread().interrupt();
				throw new RuntimeException("Interrupted while creating bucket " + name, iex);

			} catch (Throwable t) {

				lastError = t;

				try { Thread.sleep(1000); } catch (InterruptedException ignore) {}
			}
		}

		throw new RuntimeException("Unable to create bucket " + name, lastError);
	}

	static void putObject(final String bucket, final String key, final byte[] data, final Map<String, String> metadata) {

		try {

			final PutObjectRequest.Builder builder = PutObjectRequest.builder().bucket(bucket).key(key);

			if (metadata != null) {

				builder.metadata(metadata);
			}

			client.putObject(builder.build(), AsyncRequestBody.fromBytes(data)).get();

		} catch (InterruptedException iex) {

			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted while putting object " + key, iex);

		} catch (ExecutionException eex) {

			throw new RuntimeException("Unable to put object " + key, eex.getCause());
		}
	}

	/**
	 * @return the object's head data, or null if the object does not exist
	 */
	static HeadObjectResponse head(final String bucket, final String key) {

		try {

			return client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build()).get();

		} catch (InterruptedException iex) {

			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted while heading object " + key, iex);

		} catch (ExecutionException eex) {

			if (eex.getCause() instanceof S3Exception s3e && s3e.statusCode() == 404) {

				return null;
			}

			throw new RuntimeException("Unable to head object " + key, eex.getCause());
		}
	}

	static byte[] getObject(final String bucket, final String key) {

		try {

			return client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build(), AsyncResponseTransformer.toBytes()).get().asByteArray();

		} catch (InterruptedException iex) {

			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted while getting object " + key, iex);

		} catch (ExecutionException eex) {

			throw new RuntimeException("Unable to get object " + key, eex.getCause());
		}
	}

	static void deleteObject(final String bucket, final String key) {

		try {

			client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build()).get();

		} catch (InterruptedException iex) {

			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted while deleting object " + key, iex);

		} catch (ExecutionException eex) {

			throw new RuntimeException("Unable to delete object " + key, eex.getCause());
		}
	}
}
