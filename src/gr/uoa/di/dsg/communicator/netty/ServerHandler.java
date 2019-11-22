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

import gr.uoa.di.dsg.communicator.Message;
import gr.uoa.di.dsg.communicator.NettyCommunicator;
import gr.uoa.di.dsg.communicator.RawMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

public class ServerHandler extends
		SimpleChannelInboundHandler<RawMessage> {
	private NettyCommunicator communicator; 

	private final boolean isSSL;
	boolean handshakeCompleted = false;
	//TODO: handshakeCOmpleted false only in SSL mode. In TCP mode must be true from the beginning
	public int targetId; //the node id of the node that connected to us. Available when IDENTITY is processed
	//private static Calendar cal = Calendar.getInstance();
	boolean produceLog;
	
	public ServerHandler(NettyCommunicator communicator, boolean isSSL) {
		super();
		this.communicator = communicator;
		this.isSSL = isSSL;
		handshakeCompleted = ! isSSL;
		produceLog = communicator.getEnableLog();
	}
/*NIKOS:NOWRITE on SERVERCHANNEL
	private void writeIfPossible(Channel channel) {
		RawMessage message;
//		Calendar cal = Calendar.getInstance();
//		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		while ( channel.isWritable() && ((message = communicator.outputDequeue(this.targetId)) != null) ) {
			//System.err.println("[Node"+communicator.getCurrentNode().getNodeId()+"] server side writing message to node ["+targetId+"] ("+System.currentTimeMillis()+")");
			//System.err.format("CC %d->%d writing message of type %d%n", communicator.getCurrentNode().getNodeId(), targetId, message.type);
			channel.writeAndFlush(message);
		}
	}
*/
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
//		Calendar cal = Calendar.getInstance();
//		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		
		//System.err.println("[Node"+communicator.getCurrentNode().getNodeId()+"]"+"channel active fired in server with handshakeDone: "+handshakeCompleted+" ("+System.currentTimeMillis()+")");
		if( !handshakeCompleted){
			if(ctx.pipeline().get(SslHandler.class)!=null ){
				ctx.pipeline().get(SslHandler.class).handshakeFuture().addListener(
			         new GenericFutureListener<Future<Channel>>() {
			              @Override
			              public void operationComplete(Future<Channel> future) throws Exception {
//			            	  System.err.println("[Node"+communicator.getCurrentNode().getNodeId()+"] Handshake future operation complete in server with done: " 
//										+future.isDone()+", success: "+future.isSuccess()+" cancelled: "+future.isCancelled()+" ("+sdf.format(cal.getTime())+")");
			            	  	if (future.isDone() && future.isSuccess()){
//			                  		Calendar cal = Calendar.getInstance();
//			                  		SimpleDateFormat sdf = nw SimpleDateFormat("HH:mm:ss");
			                  		//X509Certificate[] certs = ctx.pipeline().get(SslHandler.class).engine().getSession().getPeerCertificateChain();
			                   		//System.err.println("=====================++> "+ certs[0].getSubjectDN().getName());
			                  		//System.err.println("[Node"+communicator.getCurrentNode().getNodeId()+"] "+"Handshake sucess in server for node ["+targetId+"] ("+System.currentTimeMillis()+")");
			                   		handshakeCompleted = true;
			                   		/*NIKOS:NOWRITE on SERVERCHANNEL
			                   		writeIfPossible(ctx.channel());*/
			              		}else{
			                   		System.err.println("CMERRHS {Node" + communicator.getCurrentNode().getNodeId() + "} " +
			                   				"Handshake failed in server C "+  System.currentTimeMillis() + 
			                   				future.cause() != null ? ("\n" + future.cause().toString()) : ""
			                   				);
			                   	}
			              }
			         });
			}
		}else{
			/*NIKOS:NOWRITE on SERVERCHANNEL
			writeIfPossible(ctx.channel());*/
		}
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx,
			RawMessage msg) throws Exception {
		if( produceLog ) {
			Message m = communicator.getMessageFromRawMessage(msg);
			System.err.format("CMRAWIN {Node%d} via %s type %d %s s %s through %s C %d%n", 
					communicator.getCurrentNode().getNodeId(), Thread.currentThread().getName(), msg.type, 
					m.getClass().getSimpleName(), m.getSubject(),ctx.channel().toString(), System.currentTimeMillis());
		}
		int te = communicator.messageReceived(ctx.channel(), msg);
		if(te != -1)
			this.targetId = te;
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
//		Calendar cal = Calendar.getInstance();
//		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		//System.err.println("[Node"+communicator.getCurrentNode().getNodeId()+"] server channel inactive fired for  ["+ targetId +"] ("+  System.currentTimeMillis() + ")");
		if(handshakeCompleted && isSSL)
			handshakeCompleted = false;
	};

	/*NIKOS:NOWRITE on SERVERCHANNEL
	@Override
	public void channelWritabilityChanged(ChannelHandlerContext ctx)
			throws Exception {
		if(handshakeCompleted)
			writeIfPossible(ctx.channel());
		//System.err.format("CC %d->%d WritabilityChanged%n", communicator.getCurrentNode().getNodeId(), targetId);
	}
	*/
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
//		Calendar cal = Calendar.getInstance();
//		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    	//System.err.println("[Node"+communicator.getCurrentNode().getNodeId()+"] server side exception (Connection Reset By Peer) for ["+ targetId +"] ("+  System.currentTimeMillis() + ")");
	}
}
