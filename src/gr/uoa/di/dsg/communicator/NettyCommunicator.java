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

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Timer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;

import gr.uoa.di.dsg.communicator.netty.ClientHandler;
import gr.uoa.di.dsg.communicator.netty.HttpResponseFormatter;
import gr.uoa.di.dsg.communicator.netty.HttpServerHandler;
import gr.uoa.di.dsg.communicator.netty.RawMessageDecoder;
import gr.uoa.di.dsg.communicator.netty.RawMessageEncoder;
import gr.uoa.di.dsg.communicator.netty.ServerHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.IdentityCipherSuiteFilter;
import io.netty.handler.ssl.JdkSslClientContext;
import io.netty.handler.ssl.JdkSslServerContext;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;

public class NettyCommunicator extends AbstractCommunicator {
        private static final int CONNECTION_RETRIES = 12;
        private static final int CONNECTION_RETRY_DELAY_SECONDS = 10;
	private static class ConnectionRetryInfo {
		public Bootstrap b;
		public Node target;
		public int retryCount = 0;
		
		public ConnectionRetryInfo(Bootstrap b, Node target) {
			super();
			this.b = b;
			this.target = target;
		}
	}

	private EventLoopGroup bossGroup = new NioEventLoopGroup(1);
	private EventLoopGroup httpBossGroup = new NioEventLoopGroup(1);
	private EventLoopGroup workerGroup = new NioEventLoopGroup(1);
	private EventLoopGroup httpWorkerGroup = new NioEventLoopGroup(1);
	private EventLoopGroup sendersGroup = workerGroup;
	private SslContext sslServerContext = null;
	private SslContext sslClientContext = null;
	private ConcurrentHashMap<Channel, Node> nodeOfChannel = new ConcurrentHashMap<>();
	private ConcurrentHashMap<Integer, Channel> channelOfNode = new ConcurrentHashMap<>();
	private HashMap<Integer, ChannelFuture> pendingConnectionOfNode = new HashMap<>(); //this one is accessed only inside a lock
	private Object connectionLock = new Object();

	private HashMap<Integer, BlockingQueue<RawMessage>> outputQueues = new HashMap<>();
	private HttpRequestParser httpRequestParser;
	private Timer timer = new Timer("TimerThread", true);

	String trustedCertificate = null;

//	private static void initSSL(String keyStore, String pwd) {
//		System.setProperty("javax.net.ssl.keyStore", keyStore); // Private key
//																// of server for
//																// encryption
//		System.setProperty("javax.net.ssl.keyStorePassword", pwd);
//		System.setProperty("javax.net.ssl.trustStore", keyStore); // Certificates
//																	// you trust
//		System.setProperty("javax.net.ssl.trustStorePassword", pwd);
//	}

	public NettyCommunicator(Node current, Node[] allNodes) {
		super(current, allNodes);
	}

	@SuppressWarnings("deprecation")
	public NettyCommunicator(Node current, Node[] allNodes, String serverKey, String serverCert, String clientKey, String clientCert, String caCertificate, String password){
		super(current, allNodes);
		if(serverKey != null && serverCert != null && clientCert != null && clientKey != null && caCertificate != null && password != null){
			try {
				//sslServerContext = new JdkSslServerContext(new File(caCertificate), null, new File(serverCert), new File(serverKey), password, null, null, IdentityCipherSuiteFilter.INSTANCE, (ApplicationProtocolConfig) null, 0, 0);
				//sslClientContext = new JdkSslClientContext(new File(caCertificate), null, new File(clientCert), new File(clientKey), password, null, null, IdentityCipherSuiteFilter.INSTANCE, (ApplicationProtocolConfig) null, 0, 0);
				sslServerContext = new JdkSslServerContext(new File(caCertificate), null, new File(serverCert), new File(serverKey), password, null, Arrays.asList("TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256"), IdentityCipherSuiteFilter.INSTANCE, (ApplicationProtocolConfig) null, 0, 0);
				sslClientContext = new JdkSslClientContext(new File(caCertificate), null, new File(clientCert), new File(clientKey), password, null, Arrays.asList("TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256"), IdentityCipherSuiteFilter.INSTANCE, (ApplicationProtocolConfig) null, 0, 0);
			} catch (SSLException e) {
				e.printStackTrace();
			}
		}
	}

	protected void initializeImplementation() {
		
	}
	
	public HttpRequestParser getHttpRequestParser() {
		return httpRequestParser;
	}
	
	@Override
	public void sendGroup(String group, Message msg) {
		int type = msg.getType();
		byte[] data = msg.serialize();
		for (Node target : nodesPerGroup.get(group))
			llsend(target, type, data, msg);
	}
	
	@Override
	public int getNumNodesOfGroup(String group) {
		try {
			return nodesPerGroup.get(group).size();
		} catch (Exception ex) {
			throw new RuntimeException("No nodes for group " + group, ex);
		}
	}
	
	@Override
	public void sendAll(Message msg) {
		int type = msg.getType();
		byte[] data = msg.serialize();
		for (Node target : allNodes)
			llsend(target, type, data, msg);
	}

	@Override
	public void send(Node target, Message msg) {
		if (target instanceof AnonymousNode) {
			//long currentTime = System.currentTimeMillis();
			//String outputMessage = String.format("CMSND [%s] %d %d %s S %s C %d", Thread.currentThread().getName(), currentNode.nodeId, -1, msg.getMessageName(), msg.getSubject(), currentTime);
			//System.err.println(outputMessage);
			
			AnonymousNode atarget = (AnonymousNode) target;
			HttpResponseFormatter.sendHttpResponse(atarget.getChannel(), atarget.getRequest(), (HttpResponseMessage)msg);
		} else	
			llsend(target, msg.getType(), target.getNodeId() != currentNode.nodeId ? msg.serialize() : null, msg);
	}

	private void llsend(Node target, int type, byte[] data, Message msg) {
                //Watch out: data will be null if target is the current node!!!
		if (crashed)
			if(!sendMessageEvenIfCrashed(type))
				return;
		
		int targetNodeId = target.getNodeId();
		if (produceLog) {
			long currentTime = System.currentTimeMillis();
			String outputMessage = String.format("CMSND {%s} %d %d %s S %s C %d", Thread.currentThread().getName(), currentNode.nodeId, targetNodeId, msg.getMessageName(), msg.getSubject(), currentTime);
			System.err.println(outputMessage);
		}
		
		//this is a shortcut to avoid creating a channel to ourself.
		if (targetNodeId == currentNode.nodeId)
			inputEnqueue(currentNode, msg);
		else {
			RawMessage message = new RawMessage(type, data);
			outputEnqueue(targetNodeId, message);
			Channel channel = channelOfNode.get(targetNodeId);
			//optimize for the happy case: if we can simply get the channel of node, move on quickly
			if (channel == null ) {
				//if however we can't get the channel, we pay the synchronization price and repeat
				//String currentThreadName = Thread.currentThread().getName();
				synchronized(connectionLock) {
					channel = channelOfNode.get(targetNodeId);
					if (channel != null ) {
						//if the channel was updated just before we got the lock, that's fine
						channel.pipeline().fireChannelWritabilityChanged();
					} else {
						if (pendingConnectionOfNode.get(targetNodeId) == null) {
							NettyCommunicator myself = this;
							// well then, setup a new channel to this node and send the
							// message when connection is established
							Bootstrap b = new Bootstrap();
							b.group(sendersGroup);
							b.channel(NioSocketChannel.class);
							b.option(ChannelOption.SO_KEEPALIVE, true);
							b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000);
							b.handler(new ChannelInitializer<SocketChannel>() {
								@Override
								public void initChannel(SocketChannel ch) throws Exception {
									if(sslClientContext != null){
										SslHandler sh = sslClientContext.newHandler(ch.alloc());
										sh.setHandshakeTimeoutMillis(30000);
										//System.err.println("CLIENT SSL TIMEOUT=" + sh.getHandshakeTimeoutMillis());
										ch.pipeline().addLast(sh);
									}
									
									ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(64 * 1024, 0, 2, 0, 2));
									ch.pipeline().addLast(new LengthFieldPrepender(2));
									ch.pipeline().addLast(new RawMessageDecoder());
									ch.pipeline().addLast(new RawMessageEncoder());
									ch.pipeline().addLast(new ClientHandler(myself, targetNodeId, sslClientContext != null));
								}
							});
							
							ConnectionRetryInfo connectionRetryInfo = new ConnectionRetryInfo(b, target);
							
							// Start the client.
							ChannelFuture connectFuture = b.connect(target.getAddress(), target.getPort());
							pendingConnectionOfNode.put(targetNodeId, connectFuture);
							connectFuture.addListener((ChannelFuture cf) -> connectionResultListener(cf, connectionRetryInfo));
						}
					}
				}
			} else {
				channel.pipeline().fireChannelWritabilityChanged();
			}
		}
	}
	
	private void connectionResultListener(ChannelFuture cf, ConnectionRetryInfo connectionRetryInfo) {
		int targetNodeId = connectionRetryInfo.target.getNodeId();
		//this code will run in a secondary thread!
		if (cf.isDone() && cf.isSuccess()) {
			Channel newChannel = cf.channel();
			if( produceLog ) {
				String currentThreadName = Thread.currentThread().getName();
				System.err.format("CMESTOUT {%s} %d via %s %s C %d%n", 
						currentThreadName, targetNodeId, Thread.currentThread().getName(), newChannel.toString(), System.currentTimeMillis());
			}
			synchronized(connectionLock) {
				nodeOfChannel.put(newChannel, allNodes[targetNodeId]);
				channelOfNode.put(targetNodeId, newChannel);
				pendingConnectionOfNode.remove(targetNodeId);
			}
		} else {
			connectionRetryInfo.retryCount += 1;
			cf.channel().close(); //close the channel (just to be safe)
			String currentThreadName = Thread.currentThread().getName();
			if ( connectionRetryInfo.retryCount <= CONNECTION_RETRIES ) {
				System.err.format("CMWARNC {%s} %d via %s cause_follows C %d%n%s%n", 
						currentThreadName, targetNodeId, Thread.currentThread().getName(),	System.currentTimeMillis(),
				cf.cause() != null ? cf.cause().toString() : "Unknown");
				//schedule a retry 3 seconds from now
				cf.channel().eventLoop().schedule(() -> {
					ChannelFuture connectFuture = connectionRetryInfo.b.connect(connectionRetryInfo.target.getAddress(), connectionRetryInfo.target.getPort());
					connectFuture.addListener((ChannelFuture cf2) -> connectionResultListener(cf2, connectionRetryInfo));
				}, CONNECTION_RETRY_DELAY_SECONDS, TimeUnit.SECONDS);
			} else {
				System.err.format("CMERRC {%s} %d via %s cause_follows C %d%n%s%n", 
						currentThreadName, targetNodeId, Thread.currentThread().getName(),	System.currentTimeMillis(),
				cf.cause() != null ? cf.cause().toString() : "Unknown");
			}
		}
		
	}
	
	private void outputEnqueue(int targetNodeId, RawMessage message) {
		BlockingQueue<RawMessage> queue;
		queue = outputQueues.get(targetNodeId);
		if (queue == null) {
			queue = new LinkedBlockingQueue<RawMessage>();
			outputQueues.put(targetNodeId, queue);
		}
		try {
			//*DEBUG*: outputEnqueue
			//Message m = deserialize(message.type, message.data);
			//System.err.println("In outputEnqueue:: FROM source: " + getCurrentNode().nodeId + " to target: " + targetNodeId + " message: " + m.toString());
			//*END*
				
			queue.put(message);
		} catch (InterruptedException ex) {
			throw new RuntimeException("outputEnqueue for " + Integer.toString(targetNodeId), ex);
		}
	}
	
	public RawMessage outputDequeue(int targetNodeId) {
		BlockingQueue<RawMessage> queue;
		queue = outputQueues.get(targetNodeId);
		if (queue == null) {
			queue = new LinkedBlockingQueue<RawMessage>();
			outputQueues.put(targetNodeId, queue);
		}
		RawMessage ret = queue.poll();
		
		//*DEBUG* outputDequeue
		/*if(ret != null) {
			Message msg = deserialize(ret.type, ret.data);
			System.err.println("In outputDequeue:: FROM source: " + getCurrentNode().nodeId + " to target: " + targetNodeId + " message: " + msg.toString());
		}*/
		//*END*
			
		return ret;
	}

	public void start(Runnable appStart, int httpPort, HttpRequestParser parser) {
		initialize();
		this.httpRequestParser = parser;
		startInternalPort(currentNode.getPort());
		startHttpPort(httpPort);
		appStart.run();
		dispatch();
	}
	
	@Override
	public void start(Runnable appStart) {
		initialize();
		startInternalPort(currentNode.getPort());
		appStart.run();
		dispatch();
	}

	private void startInternalPort(int port) {
		ServerBootstrap b = new ServerBootstrap();
		b.option(ChannelOption.SO_BACKLOG, 1024);
		b.group(bossGroup, workerGroup);
		b.channel(NioServerSocketChannel.class);
		NettyCommunicator myself = this;
		b.childHandler(new ChannelInitializer<SocketChannel>() {
			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				if(sslServerContext != null){
					SSLEngine engine= sslServerContext.newEngine(ch.alloc());
	                engine.setUseClientMode(false);
	                engine.setNeedClientAuth(true);
					SslHandler sh = new SslHandler(engine);
					sh.setHandshakeTimeoutMillis(30000);
					//System.err.println("Handshake timeout =" + sh.getHandshakeTimeoutMillis());
	                ch.pipeline().addLast(sh);
				}
				ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(64 * 1024, 0, 2, 0, 2));
				ch.pipeline().addLast(new LengthFieldPrepender(2));
				ch.pipeline().addLast(new RawMessageDecoder());
				ch.pipeline().addLast(new RawMessageEncoder());
				ch.pipeline().addLast(new ServerHandler(myself, sslServerContext != null));
			}
		});
		b.bind(port);
		
	}
	
	private void startHttpPort(int port) {
		ServerBootstrap b = new ServerBootstrap();
		b.option(ChannelOption.SO_BACKLOG, 1024);
		b.group(httpBossGroup, httpWorkerGroup);
		b.channel(NioServerSocketChannel.class);
		NettyCommunicator myself = this;
		b.childHandler(new ChannelInitializer<SocketChannel>() {
			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
//				if(sslContext != null)
//					ch.pipeline().addLast(new SslHandler(sslContext.newEngine(ch.alloc())));
				ch.pipeline().addLast(new HttpServerCodec());
				ch.pipeline().addLast(new HttpObjectAggregator(512 * 1024));
				ch.pipeline().addLast(new HttpServerHandler(myself));
			}
		});
		b.bind(port);
	}
	
	//this one is called from other threads
	public int messageReceived(Channel channel, RawMessage msg) {
		int messageType = msg.type;
		byte[] data = msg.data;
		Node source = nodeOfChannel.get(channel);
		if (source == null) {
			if (messageType == MessageType.IDENTITY.getValue()) {
				IdentityMessage identity = IdentityMessage.deserialize(data);
				int newNodeId = identity.getNodeId();
				if (produceLog)
					System.err.format("CMINID {Node%d} %d via %s through %s%n",this.currentNode.nodeId, newNodeId, Thread.currentThread().getName(), channel.toString());
				source = allNodes[newNodeId];
				nodeOfChannel.put(channel, source);
				
				//this return value is a signal to ServerHandler, to mark the node behind this channel as identity.getNodeId()
				return identity.getNodeId(); 
						
			} else {
				// else message is dropped...
				if( produceLog )
					System.err.format("CMDROP message type %d%n", messageType);
			}
		} else{
			inputEnqueue(source, deserialize(messageType, data));
		}
		return -1;
	}

	public void messageReceived(AnonymousNode source, Message msg) {
		inputEnqueue(source, msg);
	}
	
	Message deserialize(int messageType, byte[] data) {
		return getDeserializer(messageType).deserialize(data);
	}

	@Override
	public Object setTimeout(int seconds, TimeoutHandler handler) {
		TimeoutTask task = new TimeoutTask(this, handler);
		timer.schedule(task, seconds * 1000);
		return task;
	}
	
	@Override
	public Object setTimeout(long milliseconds, TimeoutHandler handler) {
		TimeoutTask task = new TimeoutTask(this, handler);
		timer.schedule(task, milliseconds);
		return task;
	}
	
	@Override
	public void cancelTimeout(Object timeout) {
		((TimeoutTask) timeout).cancel();
	}
	
	@Override
	protected void closeAllChannels() {
		bossGroup.shutdownGracefully();
		workerGroup.shutdownGracefully();
		httpBossGroup.shutdownGracefully();
		httpWorkerGroup.shutdownGracefully();
		sendersGroup.shutdownGracefully();
		timer.cancel();
	}
	
}
