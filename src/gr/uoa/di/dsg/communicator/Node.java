/*******************************************************************************
 * Copyright (C) 2019 DSG at University of Athens
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package gr.uoa.di.dsg.communicator;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.SecretKey;

public abstract class Node {
	public enum SERVICE_TYPE {
		TCP,
		SSL,
		HTTP,
		HTTPS
	}
	
	protected int nodeId;
	protected KeyPair keyPair = null;
	private String keyStoreFilename;
	private String keyStorePassword;
	
	private SecretKey secretKey = null;
	private String symmetricKeyStorePath = null;
	
	public Node(int id) {
		this(id, "/tmp/keyStores", "password", "/tmp/keyStores/secretKey");
	}
	
	public Node(int id, String keyStoreFilename, String keyStorePassword) {
		this.nodeId = id;	
		this.keyStoreFilename = keyStoreFilename + "/node" + nodeId + "/node" + nodeId + ".jks";
		this.keyStorePassword = keyStorePassword;
	}
	
	public Node(int id, String keyStoreFilename, String keyStorePassword, String symmetricKeyStorePath) {
		this(id, keyStoreFilename, keyStorePassword);
		this.symmetricKeyStorePath = symmetricKeyStorePath;
	}
	
	public int getNodeId() {
		return nodeId;
	}
	
	public PublicKey getPublicKey() { 
		initKeys();
		return keyPair.getPublic();
	}
	public PrivateKey getPrivateKey() { 
		initKeys();
		return keyPair.getPrivate();
	}
	
	public Key getSymmetricKey(int targetID) {
		if(secretKey == null) {
			ObjectInputStream in = null;
			try {
				in = new ObjectInputStream(new FileInputStream(symmetricKeyStorePath));
				this.secretKey = (SecretKey) in.readObject();
				
				in.close();
			}
			catch (IOException | ClassNotFoundException ex) {
				throw new RuntimeException(ex);
			}
		}
		
		return secretKey;
	}
	
	public InetAddress getAddress()  { throw new RuntimeException("getAddress() unsupported");}
	public int getPort()  { throw new RuntimeException("getPort() unsupported");}
	public void init() { throw new RuntimeException("Init operation not supported!");}
	public SERVICE_TYPE getServiceType() { throw new RuntimeException("Init operation not supported!");}
	
	private void initKeys() {
		if (keyPair != null )
			return;
		try {
		    char[] password = keyStorePassword.toCharArray();
	
		    FileInputStream input = new FileInputStream(keyStoreFilename);
		    KeyStore keystore = KeyStore.getInstance("JKS");
		    keystore.load(input, password);
	
		    /* Get the node's private and public keys. */
		    String alias = "node" + nodeId;
		    this.keyPair = new KeyPair(keystore.getCertificate(alias).getPublicKey(), (PrivateKey) keystore.getKey(alias, password));
		}
		catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException | UnrecoverableKeyException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	@Override
	public String toString() {
		return "Node" + nodeId;
	}
	
	public String getKeyStorePassword() {
		return keyStorePassword;
	}
	
	public String getKeyStoreFilename() {
		return keyStoreFilename;
	}
	
	public String[] getGroups() {
		return new String[] {};
	}
}
