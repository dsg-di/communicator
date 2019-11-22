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

import gr.uoa.di.dsg.communicator.AnonymousNode;
import gr.uoa.di.dsg.communicator.NettyCommunicator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpMessage;

public class HttpServerHandler extends
		SimpleChannelInboundHandler<FullHttpMessage> {
	private NettyCommunicator communicator; 
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		//when the channel is activated (i.e., a new client is connected), create a new AnonymousNode instance
		//System.err.format("HTTP channel active at %d%n", communicator.getCurrentNode().getNodeId());
	}
	
	public HttpServerHandler(NettyCommunicator communicator) {
		super();
		this.communicator = communicator;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx,
			FullHttpMessage msg) throws Exception {
		//System.err.format("CS %d received message of type %d%n", communicator.getCurrentNode().getNodeId(), msg.type);
		//communicator.messageReceived(ctx.channel(), msg);
		//System.err.format("HTTP request at %d%n%s%n", communicator.getCurrentNode().getNodeId(), msg.toString());
		AnonymousNode source = new AnonymousNode(ctx.channel(), msg);
		communicator.messageReceived(source, communicator.getHttpRequestParser().parse(msg));
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		// Close the connection when an exception is raised.
		cause.printStackTrace();
		ctx.close();
	}
}
