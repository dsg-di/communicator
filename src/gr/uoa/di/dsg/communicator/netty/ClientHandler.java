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
package gr.uoa.di.dsg.communicator.netty;

import gr.uoa.di.dsg.communicator.IdentityMessage;
import gr.uoa.di.dsg.communicator.Message;
import gr.uoa.di.dsg.communicator.NettyCommunicator;
import gr.uoa.di.dsg.communicator.RawMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.io.IOException;

public class ClientHandler extends SimpleChannelInboundHandler<RawMessage> {
	private NettyCommunicator communicator;
	private int targetId; //the nodeId of the node we attempted connection to. Available since the constructor, as we knew it
	private final boolean isSSL;
	private boolean handshakeCompleted = false;
	private boolean produceLog;
	
	public ClientHandler(NettyCommunicator communicator, int targetId, boolean isSSL) {
		this.communicator = communicator;
		this.targetId = targetId;
		this.isSSL = isSSL;
		handshakeCompleted = ! isSSL;
		produceLog = communicator.getEnableLog();
	}

	private void writeIfPossible(Channel channel) {
		
//		Calendar cal = Calendar.getInstance();
//		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		
		RawMessage message;
		while ( channel.isWritable() && ((message = communicator.outputDequeue(targetId)) != null) ) {
			if( produceLog ) {
				Message m = communicator.getMessageFromRawMessage(message);
				System.err.format("CMRAWOUT {%s} %d->%d write type %d %s S %s through %s C %d%n", 
						Thread.currentThread().getName(), communicator.getCurrentNode().getNodeId(), targetId, 
						message.type, m.getClass().getSimpleName(), m.getSubject(), channel.toString(), System.currentTimeMillis());
			}
			//System.err.println("[Node"+communicator.getCurrentNode().getNodeId()+"] client side writing message to node ["+targetId+"] ("+System.currentTimeMillis()+")");
			channel.writeAndFlush(message);
		}
		
		//XXX: Back off protocol
		//TODO: Back off if my node id is smaller than targetId and multiple channels are available to targetNodeId 
//		if(communicator.indicateToBackOff(targetId)){
//			
//			Channel oldChannel = communicator.changeSendChannel(targetId, channel);
//			BackOffMessage bck = new BackOffMessage();
//			assert (oldChannel != null);
//			oldChannel.writeAndFlush(new RawMessage(bck.getType(), bck.serialize()));
//		}
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
//		Calendar cal = Calendar.getInstance();
//		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		
		//System.err.println("[Node"+communicator.getCurrentNode().getNodeId()+"]"+"channel active fired in client with handshakeDone: "+handshakeCompleted+" ("+System.currentTimeMillis()+")");
		if(!handshakeCompleted){
			if(ctx.pipeline().get(SslHandler.class)!=null){	
				ctx.pipeline().get(SslHandler.class).handshakeFuture().addListener(
					new GenericFutureListener<Future<Channel>>() {
						@Override
				        public void operationComplete(Future<Channel> future) throws IOException {
							//System.err.println("[Node"+communicator.getCurrentNode().getNodeId()+"] Handshake future operation complete in client with done: " 
							//						+future.isDone()+", success: "+future.isSuccess()+" cancelled: "+future.isCancelled()+" ("+System.currentTimeMillis()+")");
				            if( future.isDone() && future.isSuccess()){
//				            	SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
				            	//System.err.println("[Node"+communicator.getCurrentNode().getNodeId()+"] "+"Handshake success in client for node ["+targetId+"] ("+System.currentTimeMillis()+")");
				            	handshakeCompleted = true;
				            	sendIdentity(ctx.channel());
		              		}else{
				              	System.err.println("CMERRHC {Node"+communicator.getCurrentNode().getNodeId()+"} "+" Handshake failed in client for node "+targetId+" C "+System.currentTimeMillis() + 
		                   				future.cause() != null ? ("\n" + future.cause().toString()) : ""
				              			);
				            }
				       }
				    });
			}
		}else{
        	sendIdentity(ctx.channel());
		}
	}
	
	private void sendIdentity(Channel channel) {
		if( communicator.getEnableLog() )
			System.err.format("CMOUTID {Node%d} %d via %s through %s C %d%n", communicator.getCurrentNode().getNodeId(), targetId, Thread.currentThread().getName(), channel.toString(), System.currentTimeMillis());
		IdentityMessage cmsg = new IdentityMessage(communicator.getCurrentNode().getNodeId());
		RawMessage message = new RawMessage(cmsg.getType(), cmsg.serialize());
		channel.writeAndFlush(message);
		writeIfPossible(channel);
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
//		Calendar cal = Calendar.getInstance();
//		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		//System.err.println("[Node"+communicator.getCurrentNode().getNodeId()+"] client channel inactive fired for ["+ targetId +"] ("+  System.currentTimeMillis() + ")");
		if(handshakeCompleted && isSSL)
			handshakeCompleted = false;
	};
	
	@Override
	public void channelWritabilityChanged(ChannelHandlerContext ctx)
			throws Exception {
		if(handshakeCompleted)
			writeIfPossible(ctx.channel());
		//System.err.format("CC %d->%d WritabilityChanged%n", communicator.getCurrentNode().getNodeId(), targetId);
	}
	
    @Override
    public void channelRead0(ChannelHandlerContext ctx, RawMessage msg) {
    	System.err.format("CMERR {Node%d} Receive on client channel from %s%d", communicator.getCurrentNode().getNodeId(), ctx.channel().toString());
    	communicator.messageReceived(ctx.channel(), msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
//    	Calendar cal = Calendar.getInstance();
//    	SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    	//System.err.println("[Node"+communicator.getCurrentNode().getNodeId()+"] client side exception (Connection Reset By Peer) for ["+ targetId +"] ("+  System.currentTimeMillis() + ")");
    }	
	
}
