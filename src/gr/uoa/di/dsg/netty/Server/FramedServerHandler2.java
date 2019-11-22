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

public class FramedServerHandler2 extends SimpleChannelInboundHandler<CustomMessage>  {
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, CustomMessage msg)
			throws Exception {
    	System.out.format("<FS2>read:%s%n", msg);
        System.out.flush();
        ctx.writeAndFlush(msg);
	}
	
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
    		throws Exception {
    	cause.printStackTrace();
    	ctx.close();
    }
}
