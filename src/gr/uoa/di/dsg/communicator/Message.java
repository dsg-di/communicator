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

/**
 * there must also exists a de-serialize message on each class this thing extends
 */
public abstract class Message
{
	public static final String DEFAULT_SUBJECT = "X";
	protected String applicationID;
	private int order;
	
	public Message(String applicationID){
		this.applicationID = applicationID;
	}
	
	public void setApplicationID(String appl)
	{
		this.applicationID = appl;
	}
	
	public String getApplicationID()
	{
		return applicationID;
	}
	
	public abstract int getType();
	public abstract byte[] serialize();
	
	public String getMessageName() {
		return this.getClass().getSimpleName();
	}
	
	public String getSubject() {
		return DEFAULT_SUBJECT;
	}
	
	public long getResponseGenerationDelay() {
		return 0;
	}

	public long getResponseDispatchDelay() {
		return 0;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}
}
