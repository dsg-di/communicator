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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class HttpPageResponseMessage extends HttpResponseMessage{

	String contentType;
	byte[] content;
	
	public HttpPageResponseMessage(String contentType, String loadContent) {
		super("");
		this.contentType = contentType;
		
		try {
			this.content = Files.readAllBytes(Paths.get(loadContent));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getContentType() {
		return contentType;
	}

	@Override
	public int getType() {
		return 0;
	}

	@Override
	public byte[] serialize() {
		return content;
	}

}
