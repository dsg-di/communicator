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
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class EchoClientHandler extends ChannelInboundHandlerAdapter {
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		System.out.print("<EC>Channel Active, writing initial message");
		ByteBuf message = Unpooled.buffer(1);
		for (int i = 0; i < message.capacity(); i ++) {
			message.writeByte((byte) ('A' + i));
        }
		ctx.writeAndFlush(message);
	}
	
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
//    	boolean theEnd = true;
        ByteBuf in = (ByteBuf) msg; // (1)
        try {
        	System.out.print("<EC>");
        	while (in.isReadable()) { // (1)
            	char input = (char) in.readByte();
                System.out.print(input);
                if( input == 'C' || input == 'G' ) {
                	//the end
                } else {
//                	theEnd = false;
            		ByteBuf message = Unpooled.buffer(1);
            		message.writeByte((byte) (input + 1));
            		ctx.writeAndFlush(message);
                }
            }
            System.out.println("</EC>");
            System.out.flush();
//            if( theEnd )
//            	ctx.close();
        } finally {
            in.release();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }	
}
