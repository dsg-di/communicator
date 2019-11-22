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
package gr.uoa.di.dsg.netty.Server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class FramedClientHandler extends SimpleChannelInboundHandler<CustomMessage>  {
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
		super.channelActive(ctx);
		System.out.println("<FC>Channel Active, writing initial message");
		System.out.flush();
		//ctx.writeAndFlush("Hello world");
		ctx.writeAndFlush(new CustomMessage((byte) 3, "Hello world".getBytes()));
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, CustomMessage msg)
			throws Exception {
    	System.out.format("<FC>read:%s%n", msg);
        System.out.flush();
	}
	
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
    		throws Exception {
    	cause.printStackTrace();
    	ctx.close();
    }
}
