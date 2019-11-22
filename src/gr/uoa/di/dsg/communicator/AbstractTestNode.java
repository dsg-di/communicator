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

import java.net.InetAddress;

public abstract class AbstractTestNode extends Node implements Runnable {
	private static final String TEST_GROUP = "ALL";
	private int port;
	protected AbstractCommunicator communicator;
	private String[] groups;

	public AbstractTestNode(int id, int port, String[] groups,
			String keyStoreFilename, String keyStorePassword) {
		super(id, keyStoreFilename, keyStorePassword);
		this.port = port;
		this.groups = groups;
	}

	public AbstractTestNode(int id, int port, String keyStoreFilename,
			String keyStorePassword) {
		super(id, keyStoreFilename, keyStorePassword);
		this.port = port;
		this.groups = new String[] { TEST_GROUP };
	}

	public AbstractTestNode(int id, int port, String keyStoreFilename,
			String keyStorePassword, String symmetricKeyStoreFilename) {
		super(id, keyStoreFilename, keyStorePassword, symmetricKeyStoreFilename);
		this.port = port;
		this.groups = new String[] { TEST_GROUP };
	}

	public AbstractTestNode(int id, int port, String[] groups) {
		super(id);
		this.port = port;
		this.groups = groups;
	}

	public AbstractTestNode(int id, int port) {
		super(id);
		this.port = port;
		this.groups = new String[] { TEST_GROUP };
	}

	public void setCommunicator(AbstractCommunicator communicator) {
		this.communicator = communicator;
	}

	public AbstractCommunicator getCommunicator() {
		return this.communicator;
	}

	@Override
	public InetAddress getAddress() {
		return InetAddress.getLoopbackAddress();
	}

	@Override
	public int getPort() {
		return port;
	}

	@Override
	public SERVICE_TYPE getServiceType() {
		return SERVICE_TYPE.TCP;
	}

	public void stop() {
		communicator.stop();
	}

	@Override
	public String[] getGroups() {
		return groups;
	}

}
