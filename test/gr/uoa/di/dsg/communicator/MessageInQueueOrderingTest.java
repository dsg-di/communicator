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

import static org.junit.Assert.assertEquals;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import org.junit.Test;

public class MessageInQueueOrderingTest {
	private static final int ELEMENTS = 10;
	private BlockingQueue<InputMessageInQueue> queue = new PriorityBlockingQueue<>();
	
	@Test
	public void testNormal() {
		System.out.println("NORMAL");
		for( int i = 0; i < ELEMENTS; i++)
			enqueue(i);
		retrieve();
	}
	
	@Test
	public void testReverse() {
		System.out.println("REVERSE");
		for( int i = ELEMENTS - 1; i >= 0; i--)
			enqueue(i);
		retrieve();
	}
	
	private void enqueue(int i) {
		Message msg = new EndMessage();	
		msg.setOrder(i);
		msg.applicationID = Integer.toString(i);
		System.out.println("Enqueue " + msg.applicationID);
		queue.add(new InputMessageInQueue(null, msg));
	}
	
	private void retrieve() {
		//always expect in order retrieval
		int i = 0;
		InputMessageInQueue qmsg;
		while( (qmsg = queue.poll()) != null ) {
			assertEquals(i++, qmsg.message.getOrder());
			System.out.println("Dequeue " + qmsg.message.applicationID);
		}
	}

}
