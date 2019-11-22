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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class EchoServerHandler extends ChannelInboundHandlerAdapter  {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
    	System.out.print("<ES>read");
    	ByteBuf b = ((ByteBuf) msg).duplicate();
        ctx.write(msg);
        while(b.isReadable()) {
        	System.out.print((char) b.readByte());
        }
        System.out.flush();
    	System.out.println("</ES>read");
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    	System.out.println("<ES>read complete");
    	ctx.flush();
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
    		throws Exception {
    	cause.printStackTrace();
    	ctx.close();
    }
}
