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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

public class TcpServer {
	private EventLoopGroup bossGroup = new NioEventLoopGroup(1);
	private EventLoopGroup workerGroup = new NioEventLoopGroup();
	private static FramedClient fClient;
	
	private Channel startDiscardServer(int port) throws InterruptedException {
		ServerBootstrap b = new ServerBootstrap();
		b.option(ChannelOption.SO_BACKLOG, 1024);
		b.group(bossGroup, workerGroup);
		b.channel(NioServerSocketChannel.class);
		b.childHandler(new ChannelInitializer<SocketChannel>() {
			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				ch.pipeline().addLast(new DiscardServerHandler());
			}
		});
		Channel ch = b.bind(port).sync().channel();
		return ch;
	}
	
	private Channel startEchoServer(int port) throws InterruptedException {
		ServerBootstrap b = new ServerBootstrap();
		b.option(ChannelOption.SO_BACKLOG, 1024);
		b.group(bossGroup, workerGroup);
		b.channel(NioServerSocketChannel.class);
		b.childHandler(new ChannelInitializer<SocketChannel>() {
			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				ch.pipeline().addLast(new EchoServerHandler());
			}
		});
		Channel ch = b.bind(port).sync().channel();
		return ch;
	}
	
	private Channel startFramedServer(int port) throws InterruptedException {
		ServerBootstrap b = new ServerBootstrap();
		b.option(ChannelOption.SO_BACKLOG, 1024);
		b.group(bossGroup, workerGroup);
		b.channel(NioServerSocketChannel.class);
		b.childHandler(new ChannelInitializer<SocketChannel>() {
			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(64 * 1024, 0, 2, 0, 2));
				ch.pipeline().addLast(new LengthFieldPrepender(2));
				ch.pipeline().addLast(new CustomMessageDecoder());
				ch.pipeline().addLast(new CustomMessageEncoder());
				ch.pipeline().addLast(new FramedServerHandler());
				ch.pipeline().addLast(new FramedServerHandler2());
			}
		});
		Channel ch = b.bind(port).sync().channel();
		return ch;
	}
	
	private void start() throws InterruptedException {
		startDiscardServer(3000);
		startEchoServer(3001);
		startFramedServer(3002);
	}
	
	private void block() throws InterruptedException {
		//ch1.closeFuture().sync();
		Thread.sleep(1000000);
	}
	
	private void close() {
		bossGroup.shutdownGracefully();
		workerGroup.shutdownGracefully();
	}
	
	public static void main(String[] args) throws InterruptedException {
		Thread serverThread, clientThread;
		
		serverThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					runServer();
				} catch (InterruptedException ex) {
					ex.printStackTrace();
				}
			}
		}, "server");

		clientThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					runClient();
				} catch (InterruptedException ex) {
					ex.printStackTrace();
				}
			}
		}, "client");
		
		serverThread.start();
		Thread.sleep(500);
		clientThread.start();
		Thread.sleep(500);

//		ByteBuf message = Unpooled.buffer(1);
//		message.writeByte((byte) 'E');
//		eClient.channel.writeAndFlush(message);
	}

	
	private static void runServer() throws InterruptedException {
		TcpServer server = new TcpServer();
		server.start();
		server.block();
		server.close();
	}
	
	private static void runClient() throws InterruptedException {
		//eClient = new EchoClient("localhost", 3001);
		//eClient.run();
		fClient = new FramedClient("localhost", 3002);
		fClient.run();
	}
}
