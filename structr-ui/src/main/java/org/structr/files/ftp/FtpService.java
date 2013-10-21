/*
 *  Copyright (C) 2013 Axel Morgner
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU Affero General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.files.ftp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FileSystemFactory;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.SaltedPasswordEncryptor;
import org.apache.ftpserver.usermanager.UserFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.structr.core.Command;
import org.structr.core.RunnableService;
import org.structr.core.Services;

/**
 *
 * @author axel
 */
public class FtpService extends Thread implements RunnableService {

	private static final Logger logger = Logger.getLogger(FtpService.class.getName());
	private static final int port = 2221;
	
	private boolean doRun = false;

	@Override
	public void run() {

		// wait for service layer to be initialized
		while (!Services.isInitialized()) {
			try { Thread.sleep(1000); } catch(InterruptedException iex) { }
		}

		try {
			
			FtpServerFactory serverFactory = new FtpServerFactory();
			
			serverFactory.setUserManager(new StructrUserManager());
			serverFactory.setFileSystem( new StructrFileSystemFactory());
			
			ListenerFactory factory = new ListenerFactory();
			factory.setPort(port);
			serverFactory.addListener("default", factory.createListener());
			
			logger.log(Level.INFO, "Starting FTP server on port {0}", new Object[] {port});

			FtpServer server = serverFactory.createServer();         
			server.start();
			
		} catch (FtpException ex) {
			
			logger.log(Level.SEVERE, null, ex);
		}
	}
		
	@Override
	public void startService() {

		this.doRun = true;
		this.start();
		
		
	}

	@Override
	public void stopService() {
		//throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public boolean runOnStartup() {
		return true;
	}

	@Override
	public boolean isRunning() {
		return true;
	}

	@Override
	public void injectArguments(Command command) {
		//throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void initialize(Map<String, String> context) {
		//throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void shutdown() {
		//throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

}
