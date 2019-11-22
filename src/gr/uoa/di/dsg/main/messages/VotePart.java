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
package gr.uoa.di.dsg.main.messages;

import gr.uoa.di.dsg.communicator.Message;

public class VotePart extends Message {
	private int value;
	private byte nodeId;
	public static final int type = 102;
	
	public VotePart(String applicationID, int nodeId, int value) {
		super(applicationID);
		this.nodeId = (byte)nodeId;
		this.value = value;
	}

	@Override
	public int getType() {
		return type;
	}
	
	public int getNodeId(){
		return (int)nodeId;
	}
	
	public int getSession() {
		return value;
	}
	
	public static VotePart deserialize(byte[] data) {

		int accessStart = 1;
		int val = (((int)(data[accessStart + 0] & 0xFF )) << 24) +
                  (((int)(data[accessStart + 1] & 0xFF )) << 16) +
                  (((int)(data[accessStart + 2] & 0xFF )) << 8) +
                  ((int)(data[accessStart + 3] & 0xFF));
		
		return new VotePart("0", (int)data[0], val);
	}

	@Override
	public byte[] serialize() {
		byte[] stuff = new byte[5];
		stuff[0] = nodeId;
		int accessStart = 1;
		stuff[accessStart + 0] = (byte) ((value >>> 24) & 0xFF);
		stuff[accessStart + 1] = (byte) ((value >>> 16) & 0xFF);
		stuff[accessStart + 2] = (byte) ((value >>> 8)  & 0xFF);
		stuff[accessStart + 3] = (byte) (value & 0xFF);

		
		return stuff;
	}
}
