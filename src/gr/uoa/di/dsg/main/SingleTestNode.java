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

import gr.uoa.di.dsg.communicator.AbstractTestNode;
import gr.uoa.di.dsg.communicator.AnonymousNode;
import gr.uoa.di.dsg.communicator.HttpErrorNotFoundMessage;
import gr.uoa.di.dsg.communicator.HttpPageResponseMessage;
import gr.uoa.di.dsg.communicator.Message;
import gr.uoa.di.dsg.communicator.NettyCommunicator;
import gr.uoa.di.dsg.communicator.Node;
import gr.uoa.di.dsg.main.messages.HttpMessage;
import gr.uoa.di.dsg.main.messages.VotePart;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SingleTestNode extends AbstractTestNode {

	private int numNodes = 0;
	private int received = 0;
	private int requestSession = 0;
	private double totalFaults = 0;

	private Map<Integer, AnonymousNode> requests = null;
	private Map<Integer, Integer> sessionReceivedMessages = null;

	private String ip = null;

	/**
	 * Http Parsing stuff
	 */
	public String URLPrefix = "/finer/";
	public int NUMBER_OF_POST_PARAMS = 2;
	public String[] POST_PARAMS = { "votecode", "serialno" };

	private NettyCommunicator comm = null;

	public SingleTestNode(int id, String ip, int port, int nodeNumber, Node[] allNodes) {
		super(id, port);
		this.ip = ip;
		
		requests = new HashMap<>();
		sessionReceivedMessages = new HashMap<>();
		numNodes = nodeNumber;
		totalFaults = Math.ceil( (numNodes-1)/3 );
		
		comm = new NettyCommunicator(this, allNodes);
		super.setCommunicator(comm);
	}
	
	public SingleTestNode(int id, String ip, int port, int nodeNumber, String serverKey,  String serverCert, String clientKey, String clientCert, String caCert, String password, Node[] allNodes) {
		super(id, port);
		this.ip = ip;
		
		requests = new HashMap<>();
		sessionReceivedMessages = new HashMap<>();
		numNodes = nodeNumber;
		totalFaults = Math.ceil( (numNodes-1)/3 );
		
		comm = new NettyCommunicator(this, allNodes, serverKey, serverCert, clientKey, clientCert, caCert, password);
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
		comm.registerMessage(VotePart.type, (byte[] data) -> VotePart.deserialize(data), (Message msg, Node source) -> process((VotePart) msg, source));
		comm.registerMessage(HttpMessage.type, (byte[] data) -> HttpMessage.deserialize(data), (Message msg, Node source) -> process((HttpMessage) msg, source));
		comm.registerMessage(HttpErrorNotFoundMessage.type, (byte[] data) -> HttpErrorNotFoundMessage.deserialize(data), (Message msg, Node source) -> process((HttpErrorNotFoundMessage) msg, source));
		comm.start( ()->init(), 8081, (FullHttpMessage msg) -> httpParser(msg) );
	}

	public void init(){
	}
	/**
	 * Process functions
	 */
	private void process(VotePart message, Node source) {
		//System.out.format("At %d process %s from %d%n", this.getNodeId(), message.getValue(), source.getNodeId());
		if(message.getNodeId() == this.nodeId){
			
			if(sessionReceivedMessages.get(message.getSession()) == null)
				sessionReceivedMessages.put(message.getSession(),1);
			else{
				sessionReceivedMessages.put(message.getSession(), sessionReceivedMessages.get(message.getSession())+1 );
			}
			
			if(sessionReceivedMessages.get(message.getSession()) == numNodes-totalFaults){
				if(requests.get(message.getSession()) == null){
					System.out.println(message.getSession()+ " is null ");
				}
				comm.send(requests.get(message.getSession()), new HttpPageResponseMessage("text/html", "form/form.html"));
			}
		}else{
			if(!sessionReceivedMessages.containsKey(message.getSession())){
				sessionReceivedMessages.put(message.getSession(), 0);
				comm.sendAll(message);
			}
		}
	}
	

	private void process(HttpMessage message, Node source) {
		int session = ++requestSession;
		assert(source != null);
		requests.put(session, (AnonymousNode)source);
		sessionReceivedMessages.put(session, received);
		comm.sendAll(new VotePart("0", this.nodeId, session));
	}
	
	private void process(HttpErrorNotFoundMessage message, Node source) {
		System.out.println("Error called!");
	}

	/**
	 * HTTP Parser
	 */
	public Message httpParser(FullHttpMessage msg) {
		int electionID = 0;
		Map<String, String> params = null;
		
		if (msg instanceof FullHttpRequest) {
			FullHttpRequest request = (FullHttpRequest) msg;
			if (request.getUri().startsWith(URLPrefix)) {
				electionID = retrieveElectionID(request.getUri());
				switch (request.getMethod().name()) {
					case "GET":
					default:
						// System.out.println("In GET method");
						return new HttpMessage("0", "OK");
					case "POST":
						System.out.println("In POST method");
						HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(
								new DefaultHttpDataFactory(false), request);
						List<InterfaceHttpData> content = decoder
								.getBodyHttpDatas();
						params = parsePostParams(content);
						if (params == null) {
							// return error message
						}
						// return Vote message
						System.out.println(electionID + "   " + params.toString());
						return new HttpErrorNotFoundMessage(0);
				}
			} else {
				return new HttpErrorNotFoundMessage(0);
			}
		} else {
			return new HttpErrorNotFoundMessage(0);
		}
	}

	private int retrieveElectionID(String uri) {
		return Integer.parseInt(uri.split("/")[2]);

	}

	private Map<String, String> parsePostParams(List<InterfaceHttpData> content) {

		Map<String, String> postValues = new HashMap<>();

		if (content.size() == NUMBER_OF_POST_PARAMS) {
			for (InterfaceHttpData data : content) {
				String key = data.toString().split("=")[0];
				String value = data.toString().split("=")[1];
				if (Arrays.asList(POST_PARAMS).contains(key))
					postValues.put(key, value);
				else {
					return null;
				}
			}
		} else {
			return null;
		}
		return postValues;
	}
	
	public static void main(String[] args){
		
		System.out.println("usage:<ip1> <ip2> <ip3> <ip4> <port> <numNodes> <nodeId> <keystore> <ceriticate>");
		
		int port     = Integer.parseInt(args[4]);
		int numNodes = Integer.parseInt(args[5]);
		int nodeId   = Integer.parseInt(args[6]);
		
		SingleTestNode[] nodes = new SingleTestNode[numNodes];
		System.out.println("Running for "+numNodes+ " nodes");
		
		for( int i = 0; i < numNodes; i++ ) {
			nodes[i] = new SingleTestNode(i, args[i], port, numNodes, args[7], args[8], args[9], args[10], args[11], args[12], nodes);
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
