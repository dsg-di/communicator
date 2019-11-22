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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;


public abstract class AbstractCommunicator {
	
	private Map<Integer, MessageDispatchInfo> msgRouter = new HashMap<>();
	protected Node currentNode = null;
	protected Node[] allNodes = null;
	protected HashMap<String, ArrayList<Node>> nodesPerGroup = new HashMap<>();
	protected boolean produceLog = false;
	private BlockingQueue<InputMessageInQueue> inputQueue = new LinkedBlockingQueue<>();
	//private BlockingQueue<InputMessageInQueue> inputQueue = new PriorityBlockingQueue<>();
	private boolean keepDispatching = true;
	private int messageOrderCounter = Integer.MIN_VALUE;
	private Message currentlyDispatchedMessage;
	protected boolean crashed = false;

	protected ExecutorService executorService;
	
        protected abstract void initializeImplementation();
	public abstract void send(Node other, Message msg);
        public abstract void sendGroup(String group, Message msg);
	public abstract void sendAll(Message msg);
	
	public abstract void start(Runnable init);
    
	public AbstractCommunicator(Node current, Node[] allNodes)
	{
		this.currentNode = current;
		this.allNodes = allNodes;
		//initialize Node groups
		for (Node node: allNodes) {
			for (String group: node.getGroups()) {
				ArrayList<Node> groupNodes = nodesPerGroup.get(group);
				if (groupNodes == null) {
					groupNodes = new ArrayList<Node>();
					nodesPerGroup.put(group, groupNodes);
				}
				groupNodes.add(node);
			}
		}
		createWorkers(1);
	}
	
	public void enableLog() {
		produceLog = true;
	}
	
	public void disableLog() {
		produceLog = false;
	}
	
	public boolean getEnableLog() {
		return produceLog;
	}
	
	public void stop() {
		inputEnqueue(null, new EndMessage());
	}
	
	public void registerMessage(int type, Deserializer deser, Dispatcher disp){
		msgRouter.put(type, new MessageDispatchInfo(deser, disp));
	}
	
	public int getNumNodes() {
		return this.allNodes.length;
	}
	
	public abstract int getNumNodesOfGroup(String group);
	public List<Integer> getNodeIDsOfGroup(String group) {
		List<Integer> IDs = new ArrayList<>();
		
		for(Node node: nodesPerGroup.get(group))
			IDs.add(node.nodeId);
		
		return IDs;
	}
	
	public List<Node> getNodesOfGroup(String group) {
		List<Node> ret = new ArrayList<>();
		
		for(Node node: nodesPerGroup.get(group))
			ret.add(node);
		
		return ret;
	}
	
	public Node getCurrentNode() {
		return currentNode;
	}
	
	public Node getOtherNode(int nodeId) {
		return allNodes[nodeId];
	}
	
	public Deserializer getDeserializer(int messageType) {
		try {
			return msgRouter.get(messageType).deserializer;
		} catch (Exception ex) {
			throw new RuntimeException(String.format("Could not locate message deserializer for type %d", messageType));
		}
	}
	
	public Dispatcher getDispatcher(int messageType){
		try {
			return msgRouter.get(messageType).dispatcher;
		} catch (Exception ex) {
			throw new RuntimeException(String.format("Could not locate message handler for type %d", messageType));
		}
	}
	
	public Object setTimeout(int seconds, TimeoutHandler handler) { throw new UnsupportedOperationException(); }
	public Object setTimeout(long milliseconds, TimeoutHandler handler) { throw new UnsupportedOperationException(); }
	
	public void cancelTimeout(Object timeout) { throw new UnsupportedOperationException(); }
	
	public void setTransmitDelay(long millisecs) {throw new UnsupportedOperationException();}
	
	protected void createWorkers(int numThreads) {
		executorService = Executors.newFixedThreadPool(numThreads);
	}
	
	protected void destroyWorkers() {
		executorService.shutdown();
	}
	
	public void submitBackgroundTask(Runnable task) {
		executorService.execute(task);
	}
	
	public void inputEnqueue(Node source, Message msg) {
		try {
			//*DEBUG* inputEnqueue
			//System.err.println("In inputEnqueue:: FROM source: " + source.getNodeId() + " to target: " + getCurrentNode().nodeId + " message: " + msg.toString());
			//*END*
			
			int order;
			if ( msg instanceof AsynchronousTaskResultMessage) {
				order = msg.getOrder();
			} else {
				order = messageOrderCounter++;
				msg.setOrder(order);
			}
			
			inputQueue.put(new InputMessageInQueue(source, msg));
		} catch (Exception ex) {
			throw new RuntimeException("inputEnqueue", ex);
		}
	}
	
	public int getCurrentMessageOrder() {
		if( currentlyDispatchedMessage == null )
			return messageOrderCounter;
		return currentlyDispatchedMessage.getOrder();
		
	}

	protected void stopDispatching() {
		keepDispatching = false;
	}
	
	protected void dispatch() {
		InputMessageInQueue miq = null;
		while (keepDispatching) {
			try {
				miq = inputQueue.take();
				currentlyDispatchedMessage = miq.message;
				//currentMessageOrder = currentlyDispatchedMessage.getOrder();
				
				//*DEBUG* dispatch
				//System.err.println("In dispatch:: FROM source: " + miq.source.nodeId + " to target: " + getCurrentNode().nodeId + " message: " + miq.message.toString());
				//*END*
				
				// if(GlobalVariables.LOW_VERBOSE)
				// System.err.println("[SSL]: recv from " +
				// miq.source.getNodeId() + " msg " +
				// miq.message.toString()); //DEBUG
				if ( produceLog ) {
					long startTime = System.currentTimeMillis();
					getDispatcher(currentlyDispatchedMessage.getType()).dispatchMessage(currentlyDispatchedMessage, miq.source);				
					long endTime = System.currentTimeMillis();
					String outputMessage = String.format("CMDIS {%s} %d %s S %s E %d R %d D %d C %d", 
							Thread.currentThread().getName(), (miq.source != null ? miq.source.nodeId : currentNode.nodeId), currentlyDispatchedMessage.getMessageName(), currentlyDispatchedMessage.getSubject(), 
							endTime - startTime, currentlyDispatchedMessage.getResponseGenerationDelay(), currentlyDispatchedMessage.getResponseDispatchDelay(), startTime);
					System.err.println(outputMessage);
				} else {
					getDispatcher(currentlyDispatchedMessage.getType()).dispatchMessage(currentlyDispatchedMessage, miq.source);
				}
			} catch (InterruptedException ex) {
				throw new RuntimeException("inputQueue.take", ex);
			}
		}
		closeAllChannels();
	}
	
	protected void closeAllChannels() {
		
	}

	public Message getMessageFromRawMessage(RawMessage rmsg) {
		return getDeserializer(rmsg.type).deserialize(rmsg.data);
	}
	
	protected boolean sendMessageEvenIfCrashed(int type) {
		//[MessageType.MIN, MessageType.MAX] are communicator messages
		//Application level messages (e.g. Test Framework) should be >= 100
		return type >= MessageType.MIN;
	}

        public void setCrashed() {
		crashed = true;
	}
        
    protected void initialize() {
		//initialize internal communicator messages
		registerMessage(AsynchronousTaskResultMessage.type, 
				(data) -> AsynchronousTaskResultMessage.deserialize(data), 
				(msg, source) -> processAsynchronousTaskResultMessage((AsynchronousTaskResultMessage) msg, source));
		registerMessage(TimeoutMessage.type, 
				(data) -> TimeoutMessage.deserialize(data), 
				(msg, source) -> processTimeoutMessage((TimeoutMessage) msg, source));
		registerMessage(EndMessage.type, 
				(data) -> EndMessage.deserialize(data), 
				(msg, source) -> processEndMessage((EndMessage) msg, source));
		registerMessage(HelloMessage.type, 
				(data) -> HelloMessage.deserialize(data), 
				(msg, source) -> {});
		
		//fix thread name if not fixed yet
                String tn = Thread.currentThread().getName();
		if ( tn.equals("main") || tn.startsWith("Thread-") )
			Thread.currentThread().setName("Node" + currentNode.nodeId);
                
        initializeImplementation(); //give the specific communicator implementation a chance to initialize
        }
        
	private void processAsynchronousTaskResultMessage(AsynchronousTaskResultMessage msg, Node source) {
		msg.getRunable().run();
	}
	
	private void processTimeoutMessage(TimeoutMessage msg, Node source) {
		msg.getHandler().processTimeout();
	}
	
	private void processEndMessage(EndMessage msg, Node source) {
		stopDispatching();
                destroyWorkers();
	}

}
