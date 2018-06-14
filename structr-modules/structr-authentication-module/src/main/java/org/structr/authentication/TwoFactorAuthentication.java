/**
 * Copyright (C) 2010-2018 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.authentication;
 
import com.j256.twofactorauth.TimeBasedOneTimePasswordUtil;
import java.security.GeneralSecurityException;

/**
 *
 * @author ts
 */

public class TwoFactorAuthentication {
   private TimeBasedOneTimePasswordUtil twoFacUtil= new TimeBasedOneTimePasswordUtil(); 
   
   public String generateKey()
   {
       return twoFacUtil.generateBase32Secret(); 
   }
   
   public String getImage(String user, String secret)
   {
       return twoFacUtil.qrImageUrl(user, secret);
   }
   
   public String generateCurrentNumber(String secret) throws GeneralSecurityException
   {
       return twoFacUtil.generateCurrentNumberString(secret);     
   }
   
   public String getSecret(String user)
   {
       return null;
   }
}
