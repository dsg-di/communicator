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

import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class BasicTest {
	private int numNodes = 4;
	private TestNode[] allNodes = new TestNode[numNodes];
	
	private class TestNode extends AbstractTestNode {
		private NettyCommunicator comm;
		private int received = 0;
		
		public TestNode(int nodeId, int port) {
			super(nodeId, port);
		}
		
		@Override
		public void run() {
			comm = new NettyCommunicator(this, allNodes);
			super.setCommunicator(comm);
			comm.registerMessage(TestMessage.type, (byte[] data) -> TestMessage.deserialize(data), (Message msg, Node source) -> process((TestMessage) msg, source));
			comm.registerMessage(Test2Message.type, (byte[] data) -> Test2Message.deserialize(data), (Message msg, Node source) -> process((Test2Message) msg, source));
			comm.registerMessage(HttpMessage.type, (byte[] data) -> HttpMessage.deserialize(data), (Message msg, Node source) -> process((HttpMessage) msg, source));
			comm.registerMessage(HttpErrorNotFoundMessage.type, (byte[] data) -> HttpErrorNotFoundMessage.deserialize(data), (Message msg, Node source) -> process((HttpErrorNotFoundMessage) msg, source));
			comm.registerMessage(VoteMessage.type, (byte[] data) -> {return null;}, (Message msg, Node source) -> process((VoteMessage) msg, source));
			
			comm.start( ()->init(), 8080 + nodeId, (FullHttpMessage msg) -> httpParser(msg) );
		}

		public void init() {
			comm.sendAll(new TestMessage("0", "Msg 1"));
		}
		
		/**
		 * Http Parser
		 */
		
		public String URLPrefix = "/finer/";
		public int NUMBER_OF_POST_PARAMS = 2;
		public String[] POST_PARAMS = {"votecode", "serialno"};
		
		public Message httpParser(FullHttpMessage msg) {
			
			int electionID = 0;
			Map<String, String> params = null;
			
			if(msg instanceof FullHttpRequest){
				FullHttpRequest request = (FullHttpRequest)msg;
				
				if(request.getUri().startsWith(URLPrefix)){
					
					electionID = retrieveElectionID(request.getUri());
					
					switch(request.getMethod().name()){
						case "GET":
						default:
							//System.out.println("In GET method");
							return new HttpMessage("0", "OK");
						case "POST":
							System.out.println("In POST method");
							HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(new DefaultHttpDataFactory(false), request);
							List<InterfaceHttpData> content = decoder.getBodyHttpDatas();
							params = parsePostParams(content);
							if(params == null) {
								//return error message
							}
							//return Vote message
							System.out.println(electionID + "   "+params.toString());
							return new VoteMessage();
					}
				}else{
					return new HttpErrorNotFoundMessage(0);
				}
			}else{
				return new HttpErrorNotFoundMessage(0);
			}
		}
		
		private int retrieveElectionID(String uri) {
			return Integer.parseInt(uri.split("/")[2]);
			
		}

		private Map<String, String> parsePostParams(List<InterfaceHttpData> content) {
			
			Map<String, String> postValues = new HashMap<>();
			
			if(content.size() == NUMBER_OF_POST_PARAMS){
				for(InterfaceHttpData data : content){
					String key = data.toString().split("=")[0];
					String value = data.toString().split("=")[1];
					if(Arrays.asList(POST_PARAMS).contains(key))
						postValues.put(key, value);
					else{
						return null;
					}
				}
			}
			else{
				return null;
			}
			return postValues;
		}

		private void process(TestMessage message, Node source) {
			System.out.format("At %d process %s from %d %n", this.getNodeId(), message.toString(), source.getNodeId());
			received += 1;
			if (received == numNodes) {
                            received = 0;
                            comm.sendAll(new Test2Message("0", "Msg 2"));
                        }
		}
		
		private void process(Test2Message message, Node source) {
			System.out.format("At %d process %s from %d %n", this.getNodeId(), message.getValue(), source.getNodeId());
			received += 1;
			if (received == numNodes) {
                            received = 0;
                            comm.stop();
                        }
			//System.out.format("At %d process %s from %d%n", this.getNodeId(), message.getValue(), source.getNodeId());
		}
		
		private void process(VoteMessage message, Node source) {
			//System.out.format("At %d process %s from %d%n", this.getNodeId(), message.getValue(), source.getNodeId());
			comm.send(source,  new HttpResponseMessage("Receipt:ABCDEFGHIJ"));
		}
		
		private void process(HttpMessage message, Node source) {
			//System.out.format("At %d process %s from %d%n", this.getNodeId(), message.getValue(), source.getNodeId());
			comm.send(source, new HttpPageResponseMessage("text/html","form/form.html"));
		}
		
		private void process(HttpErrorNotFoundMessage message, Node source) {
			//System.out.println("Error called!");
		}
	}
	
	@Before
	public void setUp() throws Exception {
		for( int i = 0; i < numNodes; i++ ) {
			allNodes[i] = new TestNode(i, 3000+i);
		}
	}

	@Test
	public void testBasic() {
		System.out.println("Start of test.");
		Thread[] threads = new Thread[numNodes];
		for(int i=0; i<numNodes; i++)
			threads[i] = new Thread(allNodes[i], "Node-" + i);
		for(int i=0; i<numNodes; i++)
			threads[i].start();

		for(int i=0; i<numNodes; i++) {
			try {
				threads[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("End of test.");
	}
}
