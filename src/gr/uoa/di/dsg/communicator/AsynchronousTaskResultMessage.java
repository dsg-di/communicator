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

import gr.uoa.di.dsg.communicator.Message;

public class AsynchronousTaskResultMessage extends Message {
	public static final int type = MessageType.ASYNCHRONOUS_TASK_RESULT.getValue();
	private String taskName;
	private String taskOperationName;
	private String subject;
	private final Runnable runable;
	private final long responseTime = System.currentTimeMillis(); //this is the time the result is ready
	private long dispatchTime;
	private long requestTime;
	
	public AsynchronousTaskResultMessage(String taskName, String taskOperationName, String subject, long requestTime, int sourceMessageOrder, Runnable runable) {
		super("");
		this.taskName = taskName;
		this.taskOperationName = taskOperationName;
		this.subject = subject;
		this.runable = runable;
		this.requestTime = requestTime;
		this.setOrder(sourceMessageOrder);
	}
	
	public Runnable getRunable() {
		dispatchTime = System.currentTimeMillis(); //this is the time the communicator asked for the runnable to run it, hence "dispatchTime"
		return runable;
	}

	@Override
	public int getType() {
		return type;
	}
	
	@Override
	public byte[] serialize() {
		throw new UnsupportedOperationException();
	}
	
	public static AsynchronousTaskResultMessage deserialize(byte[] data) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public String getMessageName() {
		if (taskOperationName.isEmpty())
			return "ASYNC_" + taskName;
		else
			return "ASYNC_" + taskName + "-" + taskOperationName;
	}
	
	@Override
	public String getSubject() {
		return subject;
	}
	
	@Override
	public long getResponseGenerationDelay() {
		return responseTime - requestTime;
	}
	
	@Override
	public long getResponseDispatchDelay() {
		return dispatchTime - responseTime;
	}
}
