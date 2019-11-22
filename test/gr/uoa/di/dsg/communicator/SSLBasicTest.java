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

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class SSLBasicTest {
	private int numNodes = 4;
	private TestNode[] allNodes = new TestNode[numNodes];
	
	private class TestNode extends AbstractTestNode {
		private NettyCommunicator comm;
		private int received = 0;
		private int receivedTest2 = 0;
		private int totalMessages = 0;
		private int LIMIT = 1;
		
		//private String msgText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vestibulum viverra luctus nisl semper fringilla. Cras euismod mollis enim, eget semper mauris pretium eget. Aliquam sit amet dui non sapien laoreet varius at sit amet metus. Aenean interdum ullamcorper varius. Curabitur feugiat odio eget metus dignissim efficitur. Ut ac eros tempor, mattis enim ut, mattis elit. Sed non ipsum posuere, sodales turpis quis, rutrum magna. Aenean fermentum tincidunt leo, ut condimentum elit porta at. Ut venenatis leo a tincidunt egestas.Nam a porta magna. Mauris euismod mi ut est fringilla porttitor. Ut gravida interdum arcu a efficitur. In tempor non erat quis ornare. Donec magna ligula, lacinia vel dolor a, maximus congue nisl. Proin sed magna vel odio feugiat feugiat quis id tortor. Nulla facilisi. Praesent bibendum, nisi consequat tempor vehicula, est purus pharetra enim, eu convallis ligula metus condimentum sapien. Sed pretium volutpat nulla sed varius. Nunc tempor euismod blandit. Sed lorem erat, condimentum sit amet dignissim at, blandit et lectus. Nam dignissim elit lacus, sit amet convallis diam commodo in.Praesent ullamcorper, nunc eu mollis condimentum, dui nulla egestas arcu, at ornare turpis sapien quis mi. Aliquam tincidunt, sem ut iaculis egestas, elit augue varius nisi, sed feugiat tellus arcu id sapien. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Nulla ut sapien ipsum. Morbi a sagittis risus. Praesent tincidunt et felis et suscipit. Duis eu elit id ligula porta tincidunt. Phasellus tempor auctor est eu tristique. Sed erat nibh, dapibus vitae aliquet vitae, tempus gravida sem. Fusce in imperdiet risus. Praesent quam velit, auctor ornare semper a, tristique sed dolor. Cras ornare, erat vel sodales iaculis, nulla risus ornare ligula, ac tincidunt elit diam rhoncus mauris.Integer at ex placerat, dapibus magna vel, tempus sapien. Maecenas laoreet eget quam mollis ultricies. Sed ac justo tincidunt, imperdiet ex at, gravida massa. Proin cursus egestas mauris cras amet.";
		
		public TestNode(int nodeId, int port) {
			super(nodeId, port);
		}

		@Override
		public void run() {
			comm = new NettyCommunicator(this,  allNodes, 
					"/home/panos/utils/ca/archive/node" + nodeId + "/node" + nodeId + "P8.key.pem" , 
					"/home/panos/utils/ca/archive/node" + nodeId + "/node" + nodeId + ".crt.pem" , 
					"/home/panos/utils/ca/archive/node" + nodeId + "/node" + nodeId + "cP8.key.pem" , 
					"/home/panos/utils/ca/archive/node" + nodeId + "/node" + nodeId + "c.crt.pem" , 
					"/home/panos/utils/ca/archive/node" + nodeId + "/ca.crt.pem" , 
					"password");
//			comm = new NettyCommunicator(this,  allNodes);
		
			comm.enableLog();
			super.setCommunicator(comm);
			comm.registerMessage(TestMessage.type, (byte[] data) -> TestMessage.deserialize(data), (Message msg, Node source) -> process((TestMessage) msg, source));
			comm.registerMessage(Test2Message.type, (byte[] data) -> Test2Message.deserialize(data), (Message msg, Node source) -> process((Test2Message) msg, source));
			if( nodeId == 3 )
                            try {
                                Thread.sleep(13000);
                            } catch (InterruptedException ex) {
                                ex.printStackTrace();
                            }
                        comm.start( ()->init());
		}

		public void init() {
			sendToAll();
		}
		
		private void sendToAll() {
			for( int i = 0; i < numNodes; i++) {
				Node to = allNodes[i];
				comm.send(to, new TestMessage("0", textWhen(comm.currentNode, to) ));
			}
			//comm.sendAll(new TestMessage("X", msgText ));
		}
		
		private void send2ToAll() {
			for( int i = 0; i < numNodes; i++) {
				Node to = allNodes[i];
				comm.send(to, new Test2Message("0", textWhen(comm.currentNode, to) ));
			}
			//comm.sendAll(new TestMessage("X", msgText ));
		}
		
		private String textWhen(Node nFrom, Node nTo) {
			return String.format("%d_to_%d", nFrom.nodeId, nTo.nodeId);
		}
		
		private void process(TestMessage message, Node source) {
//			System.out.format("TESTPROC {Node%d} TestMessage %s from %d%n", this.getNodeId(), message.getValue(), source.getNodeId());
			assertEquals(textWhen(source, comm.currentNode), message.getValue());
			received += 1;
			if (received == numNodes){
				received = 0;
				totalMessages++;
				if(totalMessages < LIMIT)
					sendToAll(); //comm.sendAll(new TestMessage("X", msgText));
				else
					send2ToAll(); //comm.sendAll(new Test2Message("X", msgText));
			}
		}
		
		private void process(Test2Message message, Node source) {
//			System.out.format("TESTPROC {Node%d} Test2Message %s from %d%n", this.getNodeId(), message.getValue(), source.getNodeId());
			assertEquals(textWhen(source, comm.currentNode), message.getValue());
			receivedTest2 += 1;
			if(receivedTest2 == numNodes){
				System.out.println("Finishing node " + comm.getCurrentNode().getNodeId());
				comm.stop();
			}
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
		for(int i=0; i<numNodes; i++) {
			threads[i] = new Thread(allNodes[i], "Node" + i);
			threads[i].start();
		}
		
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
