/*
 *  Copyright (C) 2011 Axel Morgner
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
package org.structr.core.cloud;

import com.esotericsoftware.kryo.Compressor;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;

/**
 *
 * @author Christian Morgner
 */
public class EncryptingCompressor extends Compressor {

	private static final Logger logger = Logger.getLogger(EncryptingCompressor.class.getName());
	
	public EncryptingCompressor(Serializer serializer) {

		this(serializer, CloudService.BUFFER_SIZE);
	}

	public EncryptingCompressor(Serializer serializer, int bufferSize) {

		super(serializer, bufferSize);
	}

	@Override
	public void compress(ByteBuffer inputBuffer, Object object, ByteBuffer outputBuffer) {

		int remoteEntityId = Kryo.getContext().getRemoteEntityID();

		Cipher cipher = EncryptionContext.getEncryptionCipher(remoteEntityId);
		if(cipher != null) {

			try {

				cipher.doFinal(inputBuffer, outputBuffer);

			} catch(Throwable t) {

				EncryptionContext.setErrorMessage(remoteEntityId, t.getMessage());

				t.printStackTrace();
			}

		} else {

			// no encryption
			outputBuffer.put(inputBuffer);
		}
	}

	@Override
	public void decompress(ByteBuffer inputBuffer, Class type, ByteBuffer outputBuffer) {

		int remoteEntityId = Kryo.getContext().getRemoteEntityID();

		Cipher cipher = EncryptionContext.getDecryptionCipher(remoteEntityId);
		if(cipher != null) {

			try {

				cipher.doFinal(inputBuffer, outputBuffer);

			} catch(Throwable t) {

				EncryptionContext.setErrorMessage(remoteEntityId, t.getMessage());

				t.printStackTrace();
			}

		} else {

			// no encryption
			outputBuffer.put(inputBuffer);
		}
	}
}
