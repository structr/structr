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
package org.structr.test.rest.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.rest.auth.AuthHelper;
import org.structr.test.rest.common.StructrRestTestBase;
import org.structr.test.rest.test.AdvancedPagingTest;
import org.testng.Assert;
import org.testng.annotations.Test;

public class AuthTest  extends StructrRestTestBase {

    private static final Logger logger = LoggerFactory.getLogger(AdvancedPagingTest.class.getName());

    @Test
    public void test01TwoFactorWhitelistTest() {

        Assert.assertTrue (AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication("[2a01:598:d826:fe8f:38ab:d115:7644:3223]", "2a01:598:d826:fe8f:28ab::/67"));

        Assert.assertTrue (AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication("2a01:598:d826:fe8f:38ab:d115:7644:3223", "2a01:598:d826:fe8f:38ab:d115:7644:3223"));

        Assert.assertTrue (AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication("2a11::", "2a01:598:d826:fe8f:28ab::/8"));
        Assert.assertFalse(AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication("2a11::", "2a01:598:d826:fe8f:28ab::/12"));

        Assert.assertTrue (AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication("192.168.1.55", "192.168.1.1/24"));
        Assert.assertFalse(AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication("192.168.2.1", "192.168.1.1/24"));

        Assert.assertTrue (AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication("192.168.1.255", "192.168.1.1/23"));
        Assert.assertFalse(AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication("192.168.1.45", "192.168.0.0/24"));
        Assert.assertTrue (AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication("192.168.1.45", "192.168.0.0/23"));

        Assert.assertTrue (AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication("192.168.1.1", "192.168.1.1/a"));
        Assert.assertFalse(AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication("192.168.1.2", "192.168.1.1/a"));

        Assert.assertTrue (AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication("192.168.1.1", "192.168.1.1/0"));
        Assert.assertFalse(AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication("192.168.1.2", "192.168.1.1/0"));

        Assert.assertFalse(AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication("254.254.254.254", "192.168.1.1/0"));


        // expect malformed whitelist entries to not affect the result of the checks - only log entries will be generated
        Assert.assertTrue (AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication("[2a01:598:d826:fe8f:38ab:d115:7644:3223]", "xxx, 2a01:598:d826:fe8f:28ab::/67, xxx"));

        Assert.assertTrue (AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication("2a01:598:d826:fe8f:38ab:d115:7644:3223", "xxx, 2a01:598:d826:fe8f:38ab:d115:7644:3223, xxx"));

        Assert.assertTrue (AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication("2a11::", "xxx, 2a01:598:d826:fe8f:28ab::/8, xxx"));
        Assert.assertFalse(AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication("2a11::", "xxx, 2a01:598:d826:fe8f:28ab::/12, xxx"));

        Assert.assertTrue (AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication("192.168.1.55", "xxx, 192.168.1.1/24, xxx"));
        Assert.assertFalse(AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication("192.168.2.1", "xxx, 192.168.1.1/24, xxx"));

        Assert.assertTrue (AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication("192.168.1.255", "xxx, 192.168.1.1/23, xxx"));
        Assert.assertFalse(AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication("192.168.1.45", "xxx, 192.168.0.0/24, xxx"));
        Assert.assertTrue (AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication("192.168.1.45", "xxx, 192.168.0.0/23, xxx"));

        Assert.assertTrue (AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication("192.168.1.1", "xxx, 192.168.1.1/a, xxx"));
        Assert.assertFalse(AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication("192.168.1.2", "xxx, 192.168.1.1/a, xxx"));

        Assert.assertTrue (AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication("192.168.1.1", "xxx, 192.168.1.1/0, xxx"));
        Assert.assertFalse(AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication("192.168.1.2", "xxx, 192.168.1.1/0, xxx"));

        Assert.assertFalse(AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication("254.254.254.254", "xxx, 192.168.1.1/0, xxx"));
    }
}