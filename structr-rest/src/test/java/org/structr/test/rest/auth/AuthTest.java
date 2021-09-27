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

        Assert.assertTrue(AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication("[2a01:598:d826:fe8f:38ab:d115:7644:3223]", "2a01:598:d826:fe8f:28ab::/67"));

        Assert.assertTrue(AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication("2a01:598:d826:fe8f:38ab:d115:7644:3223", "2a01:598:d826:fe8f:38ab:d115:7644:3223"));

        Assert.assertTrue(AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication("2a11::", "2a01:598:d826:fe8f:28ab::/8"));
        Assert.assertFalse(AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication("2a11::", "2a01:598:d826:fe8f:28ab::/12"));

        Assert.assertTrue(AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication("192.168.1.55", "192.168.1.1/24"));
        Assert.assertFalse(AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication("192.168.2.1", "192.168.1.1/24"));

        Assert.assertTrue(AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication("192.168.1.255", "192.168.1.1/23"));
        Assert.assertFalse(AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication("192.168.1.45", "192.168.0.0/24"));
        Assert.assertTrue(AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication("192.168.1.45", "192.168.0.0/23"));

        Assert.assertTrue(AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication("192.168.1.1", "192.168.1.1/a"));
        Assert.assertFalse(AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication("192.168.1.2", "192.168.1.1/a"));

        Assert.assertTrue(AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication("192.168.1.1", "192.168.1.1/0"));
        Assert.assertFalse(AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication("192.168.1.2", "192.168.1.1/0"));

        Assert.assertFalse(AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication("254.254.254.254", "192.168.1.1/0"));
    }
}
