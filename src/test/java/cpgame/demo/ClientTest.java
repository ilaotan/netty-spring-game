package cpgame.demo;

import cpgame.demo.domain.ERequestType;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.http.*;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @project: nettygame
 * @Title: ClientTest.java
 * @Package: cpgame.nettygame
 * @author: chenpeng
 * @email: 46731706@qq.com
 * @date: 2015年8月20日 下午1:45:24
 * @description:
 * @version:
 */
public class ClientTest {

	private static ChannelFuture channelFuture;

	public static void sendMsgTest(String text) throws URISyntaxException, UnsupportedEncodingException {



	}


	public void connect(String host, int port, final ERequestType requestType) throws Exception {
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		String msg = "Are you ok?";
		if (ERequestType.SOCKET.equals(requestType)) {
			try {
				Bootstrap b = new Bootstrap();
				b.group(workerGroup).channel(NioSocketChannel.class);
				b.handler(new ChannelInitializer<Channel>() {
					@Override
					protected void initChannel(Channel ch) throws Exception {
						ChannelPipeline pipeline = ch.pipeline();
						pipeline.addLast("frameDecoder",
								new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
						pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
						pipeline.addLast("handler", new ClientInboundHandler());
					}
				});
				b.option(ChannelOption.SO_KEEPALIVE, true);

				channelFuture = b.connect(host, port).sync();
				ByteBuf messageData = Unpooled.buffer();
				messageData.writeInt(999);
				messageData.writeInt(msg.length());
				messageData.writeBytes(msg.getBytes());
				channelFuture.channel().writeAndFlush(messageData).sync();
				channelFuture.channel().closeFuture().sync();


			} catch (Exception e) {
				e.printStackTrace();
			}

		} else if (ERequestType.HTTP.equals(requestType)) {

			Bootstrap b = new Bootstrap();

			b.group(workerGroup);
			b.channel(NioSocketChannel.class);
			b.option(ChannelOption.SO_KEEPALIVE, true);
			b.handler(new ChannelInitializer<SocketChannel>() {
				@Override
				public void initChannel(SocketChannel ch) throws Exception {

					// 客户端接收到的是httpResponse响应，所以要使用HttpResponseDecoder进行解码
					ch.pipeline().addLast(new HttpResponseDecoder());
					// 客户端发送的是httprequest，所以要使用HttpRequestEncoder进行编码
					ch.pipeline().addLast(new HttpRequestEncoder());
					ch.pipeline().addLast(new ClientInboundHandler());

				}
			});
			channelFuture = b.connect(host, port).sync();
			b.option(ChannelOption.TCP_NODELAY, true);
			b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);

			URI uri = new URI("http://" + host + ":" + port);

			DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST,
					uri.toASCIIString(), Unpooled.wrappedBuffer(
							new StringBuffer().append("999").append(",").append(msg).toString().getBytes("UTF-8")));

			// 构建http请求
			request.headers().set(HttpHeaders.Names.HOST, host);
			request.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
			request.headers().set(HttpHeaders.Names.CONTENT_LENGTH, request.content().readableBytes());
			// 发送http请求
			channelFuture.channel().write(request);
			channelFuture.channel().flush();
			channelFuture.channel().closeFuture().sync();
		}

		try {
		} finally {
			workerGroup.shutdownGracefully();
		}

	}

	public static void main(String[] args) throws Exception {
		ClientTest client = new ClientTest();
		client.connect("127.0.0.1", 8080, ERequestType.HTTP);

		client.sendMsgTest("11111111111111111111");
	}
}
