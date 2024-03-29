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

public class SimpleMessage extends Message {
	private String value;
	public static final int type = 101;
	
	public SimpleMessage(String applicationID, String value) {
		super(applicationID);
		this.value = value;
	}

	@Override
	public int getType() {
		return type;
	}
	
	public String getValue() {
		return value;
	}
	
	public static SimpleMessage deserialize(byte[] data) {
		return new SimpleMessage("0", new String(data));
	}

	@Override
	public byte[] serialize() {
		return value.getBytes();
	}
}
