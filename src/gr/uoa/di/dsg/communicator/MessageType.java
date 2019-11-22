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


public enum MessageType implements GenericMessageType {
	HTTP_RESPONSE(95),
	TIMEOUT(96),
	TEST(97),
	IDENTITY(98),
	END(99),
	START(92),
	ASYNCHRONOUS_TASK_RESULT(91),
	HELLO(90);
	
	public final static int MIN = 90;
	public final static int MAX = 99;

	int value;
	MessageType( int value) {
		this.value  = value;
	}
	
	@Override
	public int getValue() {
		return value;
	}
}
