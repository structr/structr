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
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;

/**
 *
 * @author Christian Morgner
 */
public class EncryptingCompressor extends Compressor {

	private static final Logger logger = Logger.getLogger(EncryptingCompressor.class.getName());
	
	private CipherProvider cypherProvider = null;

	public EncryptingCompressor(Serializer serializer, CipherProvider cipherProvider) {

		this(serializer, cipherProvider, CloudService.BUFFER_SIZE);
	}

	public EncryptingCompressor(Serializer serializer, CipherProvider cipherProvider, int bufferSize) {

		super(serializer, bufferSize);
		this.cypherProvider = cipherProvider;
	}

	@Override
	public void compress(ByteBuffer inputBuffer, Object object, ByteBuffer outputBuffer) {

		boolean unencrypted = false;
		
		try {

			Cipher encrypt = cypherProvider.getEncryptionCipher(Kryo.getContext().getRemoteEntityID());
			if(encrypt != null) {

				logger.log(Level.FINE, "Input buffer size {0}, output buffer size {1}", new Object[] { inputBuffer.capacity(), outputBuffer.capacity() } );
				encrypt.doFinal(inputBuffer, outputBuffer);
				
			} else {
				
				unencrypted = true;
			}

		} catch(GeneralSecurityException ex) {

			logger.log(Level.WARNING, "Exception while compressing {0}: {1}", new Object[] { object.getClass().getName(), ex.getMessage() });
			
			unencrypted = true;
		}
		
		if(unencrypted) {
			
			// no encryption..
			outputBuffer.put(inputBuffer);
		}
	}

	@Override
	public void decompress(ByteBuffer inputBuffer, Class type, ByteBuffer outputBuffer) {

		boolean unencrypted = false;
		
		try {

			Cipher decrypt = cypherProvider.getDecryptionCipher(Kryo.getContext().getRemoteEntityID());
			if(decrypt != null) {

				logger.log(Level.FINE, "Input buffer size {0}, output buffer size {1}", new Object[] { inputBuffer.capacity(), outputBuffer.capacity() } );
				decrypt.doFinal(inputBuffer, outputBuffer);	
				
			} else {
				
				unencrypted = true;
			}

		} catch(GeneralSecurityException ex) {

			logger.log(Level.WARNING, "Exception while decompressing {0}: {1}", new Object[] { type.getName(), ex.getMessage() });

			unencrypted = true;
		}
		
		if(unencrypted) {
			
			// no encryption..
			outputBuffer.put(inputBuffer);
		}
	}
}
