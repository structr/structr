/*
 *  Copyright (C) 2012 Axel Morgner
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.android.restclient;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import java.io.InputStream;
import java.security.KeyStore;
import org.apache.http.conn.ssl.SSLSocketFactory;

/**
 * The base class for all asynchronous connectors in this package. Instances of
 * this class use the default shared preferences of the given activity to obtain
 * server URL, user name and password that are used for connecting to the structr
 * REST server. The keys used for these values are "Server", "UserName" and
 * "Password".
 *
 * @author Christian Morgner
 */
public abstract class StructrConnector<T> extends AsyncTask<Object, Progress, T> {

	public static final String SERVER_KEY   = "Server";
	public static final String USERNAME_KEY = "UserName";
	public static final String PASSWORD_KEY = "Password";
	
	private static SSLSocketFactory socketFactory = null;
	private static String server                  = null;
	private static String userName                = null;
	private static String password                = null;

	/**
	 * This method must be called at least once to initialize the connection
	 * settings for the structr REST client. Call this method in the onCreate
	 * method of you main activity.
	 * 
	 * @param context the context to initialize from
	 */
	public static void initialize(Context context, int sslKeyStoreId, String sslKeyStorePassword) {
		
		// initialize settings from shared preferences
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		prefs.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
			@Override
			public void onSharedPreferenceChanged(SharedPreferences sp, String string) {
				updatePreferences(sp);
			}
		});
		
		// create socket factory only once
		if(socketFactory == null) {
			socketFactory = createSslSocketFactory(context, sslKeyStoreId, sslKeyStorePassword);
		}
	}
	
	private static void updatePreferences(SharedPreferences prefs) {
		server   = prefs.getString(SERVER_KEY, "");
		userName = prefs.getString(USERNAME_KEY, "");
		password = prefs.getString(PASSWORD_KEY, "");
	}
	
	public static SSLSocketFactory getSocketFactory() {
		if(socketFactory == null) {
			throw new IllegalStateException("StructrConnector not initialized! You must call StructrConnector.initialize() before using it.");
		}
		return socketFactory;
	}
	
	public static String getServer() {
		if(server == null) {
			throw new IllegalStateException("StructrConnector not initialized! You must call StructrConnector.initialize() before using it.");
		}
		return server;
	}
	
	public static String getUserName() {
		if(userName == null) {
			throw new IllegalStateException("StructrConnector not initialized! You must call StructrConnector.initialize() before using it.");
		}
		return userName;
	}
	
	public static String getPassword() {
		if(password == null) {
			throw new IllegalStateException("StructrConnector not initialized! You must call StructrConnector.initialize() before using it.");
		}
		return password;
	}
	
	private static SSLSocketFactory createSslSocketFactory(Context context, int resourceId, String keyStorePassword) {
		
		try {
			KeyStore trusted = KeyStore.getInstance("BKS");
			InputStream in = context.getResources().openRawResource(resourceId);
			
			try {
				trusted.load(in, keyStorePassword.toCharArray());
				
			} finally {
				in.close();
			}
			
			return new SSLSocketFactory(trusted);
			
		} catch(Exception e) {
			throw new AssertionError(e);
		}
	}	
	

}
