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
package gr.uoa.di.dsg.main;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import gr.uoa.di.dsg.communicator.AbstractTestNode;
import gr.uoa.di.dsg.communicator.Message;
import gr.uoa.di.dsg.communicator.NettyCommunicator;
import gr.uoa.di.dsg.communicator.Node;
import gr.uoa.di.dsg.main.messages.Test2Message;
import gr.uoa.di.dsg.main.messages.TestMessage;
import io.netty.handler.codec.http.FullHttpMessage;

public class SSLTestNode extends AbstractTestNode {

	private int numNodes = 0;
	private int received = 0;

	private String ip = null;

	private NettyCommunicator comm = null;
	private int receivedTest2 = 0;
	private int totalMessages = 0;
	private int LIMIT = 0;
	private int REPORT_RATE = 0;

	private String msgText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vestibulum viverra luctus nisl semper frin";
	
	public SSLTestNode(int id, String ip, int port, int nodeNumber, Node[] allNodes) {
		super(id, port);
		this.ip = ip;
		this.numNodes = nodeNumber;
		this.comm = new NettyCommunicator(this, allNodes);
		super.setCommunicator(comm);
	}
	
	public SSLTestNode(int id, String ip, int port, int nodeNumber, Node[] allNodes, int limit, int reportRate) {
		super(id, port);
		this.ip = ip;
		this.numNodes = nodeNumber;

		LIMIT = limit;
		REPORT_RATE = reportRate;
		
		comm = new NettyCommunicator(this, allNodes,
				"Certificates/node" + id + "/node" + id + "P8.key.pem" , 
				"Certificates/node" + id + "/node" + id + ".crt.pem" , 
				"Certificates/node" + id + "/node" + id + "cP8.key.pem" , 
				"Certificates/node" + id + "/node" + id + "c.crt.pem" , 
				"Certificates/node" + id + "/ca.crt.pem" , 
				"password");
		super.setCommunicator(comm);
	}

	@Override
	public InetAddress getAddress() {
		try {
			return InetAddress.getByName(ip);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public void run() {
		comm.registerMessage(TestMessage.type, (byte[] data) -> TestMessage.deserialize(data), (Message msg, Node source) -> process((TestMessage) msg, source));
		comm.registerMessage(Test2Message.type, (byte[] data) -> Test2Message.deserialize(data), (Message msg, Node source) -> process((Test2Message) msg, source));
		
		comm.start( ()->init(), 8080 + nodeId, (FullHttpMessage msg) -> httpParser(msg) );
	}

	/**
	 * Http Parser
	 */
	public Message httpParser(FullHttpMessage msg) {
		return null;
	}
	
	public void init() {
		try {
			Thread.sleep(10000);
			comm.sendAll(new TestMessage("0", msgText ));
		} catch (InterruptedException e) {
			// Auto-generated catch block
			System.err.println("An Interrupted Exception was caught: " + e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	private String now() {
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		return sdf.format(Calendar.getInstance().getTime());
	}

	private void process(TestMessage message, Node source) {
		//System.out.format("At %d process %s from %d%n", this.getNodeId(), message.getValue(), source.getNodeId());
		received += 1;
		if (received == numNodes){
			received = 0;
			totalMessages++;
			
			if( totalMessages % REPORT_RATE == 0)
				System.out.println("At " + totalMessages + " " + now());
			
			if(totalMessages < LIMIT){
				comm.sendAll(new TestMessage("0", msgText));
			}else{
				comm.sendAll(new Test2Message("0", msgText));
			}
		}
	}
	
	private void process(Test2Message message, Node source) {
		receivedTest2 += 1;
		if(receivedTest2 == numNodes){
			System.out.println("Finishing node " + comm.getCurrentNode().getNodeId());
			comm.stop();
		}
		//System.out.format("At %d process %s from %d%n", this.getNodeId(), message.getValue(), source.getNodeId());
	}	
	
	public static void main(String[] args){
		
		System.out.println("usage:<ip1> <ip2> <ip3> <ip4> <port> <numNodes> <nodeId> <limit> <reportRate>");
		
		int port     = Integer.parseInt(args[4]);
		int numNodes = Integer.parseInt(args[5]);
		int nodeId   = Integer.parseInt(args[6]);
		int limit    = Integer.parseInt(args[7]);
		int reportRate = Integer.parseInt(args[8]);
		
		SSLTestNode[] nodes = new SSLTestNode[numNodes];
		System.out.println("Running for "+numNodes+ " nodes");
		
		for( int i = 0; i < numNodes; i++ ) {
			nodes[i] = new SSLTestNode(i, args[i], port, numNodes, nodes, limit, reportRate);
		}
				
		System.out.println("Start of test.");
		Thread threads = new Thread(nodes[nodeId]);
		threads.start();
		try {
			threads.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
