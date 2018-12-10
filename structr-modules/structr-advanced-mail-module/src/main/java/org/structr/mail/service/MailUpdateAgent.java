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
package org.structr.mail.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.agent.Agent;
import org.structr.agent.ReturnValue;
import org.structr.agent.Task;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.mail.entity.Mail;
import org.structr.mail.entity.Mailbox;

import java.util.List;
import java.util.Properties;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;

public class MailUpdateAgent<T extends Mailbox> extends Agent<T> {

	private static final Logger logger = LoggerFactory.getLogger(MailUpdateAgent.class.getName());

	private void checkMail(String host, String storeType, String user, String password) {
		try {

			//create properties field
			Properties properties = new Properties();

			properties.put("mail.pop3.host", host);
			properties.put("mail.pop3.port", "995");
			properties.put("mail.pop3.starttls.enable", "true");
			Session emailSession = Session.getDefaultInstance(properties);

			//create the POP3 store object and connect with the pop server
			Store store = emailSession.getStore("pop3s");

			store.connect(host, user, password);

			//create the folder object and open it
			Folder emailFolder = store.getFolder("INBOX");
			emailFolder.open(Folder.READ_ONLY);

			// retrieve the messages from the folder in an array and print it
			Message[] messages = emailFolder.getMessages();
			logger.warn("messages.length---" + messages.length);

			for (int i = 0, n = messages.length; i < n; i++) {
				Message message = messages[i];
				logger.warn("---------------------------------");
				logger.warn("Email Number " + (i + 1));
				logger.warn("Subject: " + message.getSubject());
				logger.warn("From: " + message.getFrom()[0]);
				logger.warn("Text: " + message.getContent().toString());

			}

			//close the store and folder objects
			emailFolder.close(false);
			store.close();

		} catch (NoSuchProviderException e) {
			e.printStackTrace();
		} catch (MessagingException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public ReturnValue processTask(final Task<T> task) throws Throwable {

		logger.debug("Processing task {}", task.getClass().getName());

		final App app = StructrApp.getInstance();
		try (final Tx tx = app.tx(true, true, false)) {

			task.getWorkObjects().forEach( (m) -> {
				Mailbox mb = (Mailbox)m;
				checkMail(mb.getHost(), mb.getMailProtocol(), mb.getUser(), mb.getPassword());
			});

			tx.success();
		}

		return ReturnValue.Success;
	}

	@Override
	public Class getSupportedTaskType() {
		return MailUpdateTask.class;
	}

	@Override
	public boolean createEnclosingTransaction() {
		return false;
	}
}
