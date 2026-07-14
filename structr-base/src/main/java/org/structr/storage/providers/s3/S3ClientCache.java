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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared, long-lived S3 clients keyed by connection settings. Storage
 * provider instances are created per file operation, so they must not own
 * their own clients - each client carries a Netty event loop that would
 * otherwise leak with every operation.
 *
 * The bucket is deliberately not part of the cache key: clients are
 * bucket-agnostic. Clients are never closed - the cache is bounded by the
 * number of distinct connection settings in the system; eviction on
 * configuration deletion is a possible future refinement.
 */
public abstract class S3ClientCache {

	private static final Logger logger = LoggerFactory.getLogger(S3ClientCache.class);

	private static final Map<String, S3AsyncClient> CACHE = new ConcurrentHashMap<>();

	/**
	 * @return a shared client for the given connection settings, or null
	 * when the settings are incomplete (missing region or credentials)
	 */
	public static S3AsyncClient getOrCreate(final String endpoint, final String region, final String accessKey, final String secretKey) {

		if (region == null || accessKey == null || secretKey == null) {

			logger.warn("Incomplete S3 connection settings: region, accessKey and secretKey are required");
			return null;
		}

		final String cacheKey = endpoint + "|" + region + "|" + accessKey + "|" + secretKey;

		return CACHE.computeIfAbsent(cacheKey, key -> build(endpoint, region, accessKey, secretKey));
	}

	// ----- private methods -----
	private static S3AsyncClient build(final String endpoint, final String region, final String accessKey, final String secretKey) {

		final AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

		if (endpoint != null) {

			// custom S3-compatible storage (MinIO, rustfs, Hetzner, ...)

			// ensure the endpoint has a proper URI scheme
			String normalizedEndpoint = endpoint;
			if (!endpoint.startsWith("http://") && !endpoint.startsWith("https://")) {

				// default to https if no scheme is provided
				normalizedEndpoint = "https://" + endpoint;
				logger.debug("No URI scheme provided for endpoint, using https: {}", normalizedEndpoint);
			}

			logger.debug("Creating S3 client for endpoint {}, region {}", normalizedEndpoint, region);

			return S3AsyncClient.builder()
				.endpointOverride(URI.create(normalizedEndpoint))
				.credentialsProvider(StaticCredentialsProvider.create(credentials))
				.region(Region.of(region))
				.forcePathStyle(true)
				.build();
		}

		// AWS
		logger.debug("Creating S3 client for AWS region {}", region);

		return S3AsyncClient.builder()
			.credentialsProvider(StaticCredentialsProvider.create(credentials))
			.region(Region.of(region))
			.build();
	}
}
