/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.core.entity;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * The SuperUser entity. Please note that this class is not persistent but will
 * be instantiated when needed.
 */
public class SuperUser implements Principal {

	@Override
	public boolean isAdmin() {
		return true;
	}

	public String getRealName() {
		return "Super User";
	}

	public String getPassword() {
		return null;
	}

	public String getConfirmationKey() {
		return null;
	}

	public String getSessionId() {
		return null;
	}

	@Override
	public NodeInterface getWrappedNode() {
		return null;
	}

	@Override
	public String getType() {
		return null;
	}

	@Override
	public List<Group> getParents() {
		return Collections.emptyList();
	}

	@Override
	public List<Group> getParentsPrivileged() {
		return Collections.emptyList();
	}

	@Override
	public String getUuid() {
		return Principal.SUPERUSER_ID;
	}

	public boolean shouldSkipSecurityRelationships() {
		return true;
	}

	@Override
	public void setPassword(final String passwordValue) {
		// not supported
	}

	public void setRealName(final String realName) {
		// not supported
	}

	public void setConfirmationKey(String value) throws FrameworkException {}

	@Override
	public String getName() {
		return "superadmin";
	}

	@Override
	public Iterable<NodeInterface> getOwnedNodes() {
		return null;
	}

	@Override
	public boolean addSessionId(String sessionId) {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void removeSessionId(String sessionId) {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public boolean addRefreshToken(String refreshToken) {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void removeRefreshToken(String refreshToken) {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void clearTokens() {

	}

	@Override
	public boolean isValidPassword(final String password) {
		return false;
	}

	@Override
	public String getEncryptedPassword() {
		return null;
	}

	@Override
	public String getSalt() {
		return null;
	}

	@Override
	public String getTwoFactorSecret() {
		return null;
	}

	@Override
	public String getTwoFactorUrl() {
		return null;
	}

	@Override
	public void setTwoFactorConfirmed(boolean b) throws FrameworkException {

	}

	@Override
	public void setTwoFactorToken(String token) throws FrameworkException {

	}

	@Override
	public boolean isTwoFactorUser() {
		return false;
	}

	@Override
	public void setIsTwoFactorUser(boolean b) throws FrameworkException {

	}

	@Override
	public boolean isTwoFactorConfirmed() {
		return false;
	}

	@Override
	public Integer getPasswordAttempts() {
		return 0;
	}

	@Override
	public Date getPasswordChangeDate() {
		return null;
	}

	@Override
	public void setPasswordAttempts(int num) throws FrameworkException {

	}

	@Override
	public void setLastLoginDate(Date date) throws FrameworkException {

	}

	@Override
	public String[] getSessionIds() {
		return new String[0];
	}

	@Override
	public String getLocale() {
		return null;
	}

	@Override
	public String getSessionData() {
		return null;
	}

	@Override
	public void setSessionData(String sessionData) throws FrameworkException {
		// nothing to do for SuperUser
	}

	@Override
	public boolean isBlocked() {
		return false;
	}

	@Override
	public void setIsAdmin(boolean isAdmin) throws FrameworkException {
		// nothing to do
	}

	@Override
	public void setEMail(String eMail) throws FrameworkException {
		// nothing to do
	}

	@Override
	public void setSalt(String salt) throws FrameworkException {
		// nothing to do
	}

	@Override
	public String getEMail() {
		return null;
	}

	@Override
	public List<Group> getGroups() {
		return Collections.emptyList();
	}
}
