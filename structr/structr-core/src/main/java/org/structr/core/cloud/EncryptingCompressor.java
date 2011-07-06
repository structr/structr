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
import com.esotericsoftware.kryo.SerializationException;
import com.esotericsoftware.kryo.Serializer;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import javax.crypto.Cipher;

/**
 *
 * @author Christian Morgner
 */
public class EncryptingCompressor extends Compressor {

	private CipherProvider cypherProvider = null;

	public EncryptingCompressor(Serializer serializer, CipherProvider cipherProvider) {

		this(serializer, cipherProvider, 2048);
	}

	public EncryptingCompressor(Serializer serializer, CipherProvider cipherProvider, int bufferSize) {

		super(serializer, bufferSize);
		this.cypherProvider = cipherProvider;
	}

	@Override
	public void compress(ByteBuffer inputBuffer, Object object, ByteBuffer outputBuffer) {

		try {

			Cipher encrypt = cypherProvider.getEncryptionCipher(Kryo.getContext().getRemoteEntityID());
			if(encrypt != null) {

				encrypt.doFinal(inputBuffer, outputBuffer);
			}

		} catch(GeneralSecurityException ex) {

			throw new SerializationException(ex);
		}
	}

	@Override
	public void decompress(ByteBuffer inputBuffer, Class type, ByteBuffer outputBuffer) {

		try {

			Cipher decrypt = cypherProvider.getDecryptionCipher(Kryo.getContext().getRemoteEntityID());
			if(decrypt != null) {

				decrypt.doFinal(inputBuffer, outputBuffer);
			}

		} catch(GeneralSecurityException ex) {

			throw new SerializationException(ex);
		}
	}
}
