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

public class IdentityMessage extends Message {
	public static final int type = MessageType.IDENTITY.getValue();
	private byte nodeId;
	
	public IdentityMessage(int nodeId) {
		super("");
		this.nodeId = (byte) nodeId;
	}
	
	@Override
	public int getType() {
		return type;
	}
	
	public int getNodeId() {
		return nodeId;
	}

	@Override
	public byte[] serialize() {
		return new byte[] {nodeId};
	}
	
	public static IdentityMessage deserialize(byte[] data) {
		return new IdentityMessage(data[0]);
	}

}
