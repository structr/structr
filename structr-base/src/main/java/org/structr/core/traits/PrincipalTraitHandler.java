package org.structr.core.traits;

import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Favoritable;
import org.structr.core.entity.Group;
import org.structr.core.entity.Principal;

import java.util.Date;

public class PrincipalTraitHandler extends AbstractTraitImplementation implements PrincipalTrait {

	private final PrincipalTraitImplementation defaultImplementation;

	public PrincipalTraitHandler(final Traits traits) {

		super(traits);

		defaultImplementation = new PrincipalTraitImplementation(traits);

	}

	@Override
	public void onAuthenticate(final Principal principal) {
		defaultImplementation.onAuthenticate(principal);
	}

	@Override
	public Iterable<Favoritable> getFavorites(final Principal principal) {
		return defaultImplementation.getFavorites(principal);
	}

	@Override
	public Iterable<Group> getGroups(final Principal principal) {
		return defaultImplementation.getGroups(principal);
	}

	@Override
	public String getSessionData(final Principal principal) {
		return defaultImplementation.getSessionData(principal);
	}

	@Override
	public String getEMail(final Principal principal) {
		return defaultImplementation.getEMail(principal);
	}

	@Override
	public void setSessionData(final Principal principal, final String sessionData) throws FrameworkException {
		defaultImplementation.setSessionData(principal, sessionData);
	}

	@Override
	public boolean isAdmin(final Principal principal) {
		return defaultImplementation.isAdmin(principal);
	}

	@Override
	public boolean isBlocked(final Principal principal) {
		return defaultImplementation.isBlocked(principal);
	}

	@Override
	public void setFavorites(final Principal principal, final Iterable<Favoritable> favorites) throws FrameworkException {
		defaultImplementation.setFavorites(principal, favorites);
	}

	@Override
	public void setIsAdmin(final Principal principal, boolean isAdmin) throws FrameworkException {
		defaultImplementation.setIsAdmin(principal, isAdmin);
	}

	@Override
	public void setPassword(final Principal principal, final String password) throws FrameworkException {
		defaultImplementation.setPassword(principal, password);
	}

	@Override
	public void setEMail(final Principal principal, final String eMail) throws FrameworkException {
		defaultImplementation.setEMail(principal, eMail);
	}

	@Override
	public void setSalt(final Principal principal, final String salt) throws FrameworkException {
		defaultImplementation.setSalt(principal, salt);
	}

	@Override
	public String getLocale(final Principal principal) {
		return defaultImplementation.getLocale(principal);
	}

	@Override
	public boolean shouldSkipSecurityRelationships(final Principal principal) {
		return defaultImplementation.shouldSkipSecurityRelationships(principal);
	}

	@Override
	public void setTwoFactorConfirmed(final Principal principal, final boolean b) throws FrameworkException {
		defaultImplementation.setTwoFactorConfirmed(principal, b);
	}

	@Override
	public void setTwoFactorToken(final Principal principal, final String token) throws FrameworkException {
		defaultImplementation.setTwoFactorToken(principal, token);
	}

	@Override
	public boolean isTwoFactorUser(final Principal principal) {
		return defaultImplementation.isTwoFactorUser(principal);
	}

	@Override
	public void setIsTwoFactorUser(final Principal principal, final boolean b) throws FrameworkException {
		defaultImplementation.setIsTwoFactorUser(principal, b);
	}

	@Override
	public boolean isTwoFactorConfirmed(final Principal principal) {
		return defaultImplementation.isTwoFactorConfirmed(principal);
	}

	@Override
	public Integer getPasswordAttempts(final Principal principal) {
		return defaultImplementation.getPasswordAttempts(principal);
	}

	@Override
	public Date getPasswordChangeDate(final Principal principal) {
		return defaultImplementation.getPasswordChangeDate(principal);
	}

	@Override
	public void setPasswordAttempts(final Principal principal, final int num) throws FrameworkException {
		defaultImplementation.setPasswordAttempts(principal, num);
	}

	@Override
	public void setLastLoginDate(final Principal principal, final Date date) throws FrameworkException {
		defaultImplementation.setLastLoginDate(principal, date);
	}

	@Override
	public String[] getSessionIds(final Principal principal) {
		return defaultImplementation.getSessionIds(principal);
	}

	@Override
	public Iterable<Group> getParents(final Principal principal) {
		return defaultImplementation.getParents(principal);
	}

	@Override
	public Iterable<Group> getParentsPrivileged(final Principal principal) {
		return defaultImplementation.getParentsPrivileged(principal);
	}

	@Override
	public boolean addSessionId(final Principal principal, final String sessionId) {
		return defaultImplementation.addSessionId(principal, sessionId);
	}

	@Override
	public boolean addRefreshToken(final Principal principal, final String refreshToken) {
		return defaultImplementation.addRefreshToken(principal, refreshToken);
	}

	@Override
	public void removeSessionId(final Principal principal, final String sessionId) {
		defaultImplementation.removeSessionId(principal, sessionId);
	}

	@Override
	public void removeRefreshToken(final Principal principal, final String refreshToken) {
		defaultImplementation.removeRefreshToken(principal, refreshToken);
	}

	@Override
	public void clearTokens(final Principal principal) {
		defaultImplementation.clearTokens(principal);
	}

	@Override
	public boolean isValidPassword(final Principal principal, final String password) {
		return defaultImplementation.isValidPassword(principal, password);
	}

	@Override
	public String getEncryptedPassword(final Principal principal) {
		return defaultImplementation.getEncryptedPassword(principal);
	}

	@Override
	public String getSalt(final Principal principal) {
		return defaultImplementation.getSalt(principal);
	}

	@Override
	public String getTwoFactorSecret(final Principal principal) {
		return defaultImplementation.getTwoFactorSecret(principal);
	}

	@Override
	public String getTwoFactorUrl(final Principal principal) {
		return defaultImplementation.getTwoFactorUrl(principal);
	}
}
