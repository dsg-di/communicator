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

import io.netty.handler.codec.http.HttpResponseStatus;

public class HttpResponseMessage extends Message{
	public static int messageType = MessageType.HTTP_RESPONSE.getValue();
	private String content;
	private String subject;
	
	public HttpResponseMessage(String content, String subject) {
		super("");
		this.content = content;
		this.subject = subject;
	}
	
	public HttpResponseMessage(String content) {
		this(content, Message.DEFAULT_SUBJECT);
	}

	public String getContentType() {
		return "text/html";
	}
	
	public HttpResponseStatus getStatus(){
		return HttpResponseStatus.OK;
	}

	@Override
	public byte[] serialize() {
		return content.getBytes();
	}
	
	public static HttpResponseMessage deserialize(byte[] data) {
		return new HttpResponseMessage(new String(data));
	}

	@Override
	public int getType() {
		return messageType;
	}
	
	public String getContent() {
		return content;
	}

	public byte[] getContentBytes() {
		return content.getBytes();
	}
	
	@Override
	public String getSubject() {
		return subject;
	}
}
