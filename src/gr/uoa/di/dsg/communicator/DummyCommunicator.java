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

import java.util.Timer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class DummyCommunicator extends AbstractCommunicator {
	
	private class NetworkPair {
		public Node source;
		public Message msg;
		
		public NetworkPair(Node source, Message msg) {
			this.source = source;
			this.msg = msg;
		}
		
		@Override
		public String toString() {
			return "<" + source.nodeId + ", " + msg.toString() + ">";
		}
	}
	
	private BlockingQueue<NetworkPair> inboundQ = null;
	private Timer timer = new Timer("TimerThread", true);
	
	public DummyCommunicator(AbstractTestNode current, AbstractTestNode[] allNodes){
		super(current, allNodes);
		inboundQ = new LinkedBlockingQueue<>();
	}

        @Override
        protected void initializeImplementation() {
        }
        
	public void putInInboundQueue(int id, Message message) throws InterruptedException {
		inboundQ.put(new NetworkPair(getOtherNode(id), message));
	}
	
	@Override
	public void inputEnqueue(Node source, Message msg) {
		try {
			inboundQ.put(new NetworkPair(source, msg));
		} catch (InterruptedException ex) {
			throw new RuntimeException("inputEnqueue", ex);
		}
	}
	
	@Override
	public void start(Runnable init) {
		initialize();
		init.run();
		while(true){
			try {
				NetworkPair pair = inboundQ.take();
				if(pair.msg.getType() == MessageType.END.getValue())
						return;
				
				this.getDispatcher(pair.msg.getType()).dispatchMessage(pair.msg, pair.source);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void sendAll(Message msg) {
		for(int i = 0; i < this.getNumNodes(); i++){
			send(this.getOtherNode(i), msg);
		}
	}

	@Override
	public void send(Node other, Message msg) {
		if (crashed)
			if(!sendMessageEvenIfCrashed(msg.getType()))
				return;
		try{
			Message copy;
			if (msg.getType() == MessageType.END.getValue())
				copy = msg;
			else {
				byte[] ser = msg.serialize();
				copy = this.getDeserializer(msg.getType()).deserialize(ser);
			}
			DummyCommunicator comm = (DummyCommunicator) ((AbstractTestNode) other).getCommunicator();
			comm.putInInboundQueue(this.getCurrentNode().getNodeId(), copy);
		}
		catch(InterruptedException ex)
		{
			ex.printStackTrace();
		}
	}
	
	@Override
	public Object setTimeout(int seconds, TimeoutHandler handler) {
		TimeoutTask task = new TimeoutTask(this, handler);
		timer.schedule(task, seconds * 1000);
		return task;
	}
	
	@Override
	public void cancelTimeout(Object timeout) {
		((TimeoutTask) timeout).cancel();
	}

	@Override
	public void stop() {
		send(this.getCurrentNode(),new EndMessage());
	}

	@Override
	public void sendGroup(String group, Message msg) {
		sendAll(msg);
	}

	@Override
	public int getNumNodesOfGroup(String group) {
		return allNodes.length;
	}

    @Override
    protected void closeAllChannels() {
        timer.cancel();
    }
        
        
}
