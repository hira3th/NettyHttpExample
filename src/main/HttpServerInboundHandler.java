package main;
 

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;

/*
 * SimpleChannelInboundHandler for specified Objects, ChannelInboundHandlers will handle any incoming objects.
 */
public class HttpServerInboundHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
	private static Logger logger = LogManager.getFormatterLogger(HttpServerInboundHandler.class.getPackage().getName());

	@Override // channelRead0 will be replaced by messageReceived in Netty5
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {

		final String path = new QueryStringDecoder(request.uri()).path().replaceAll("/", "");
		HttpMethod method = request.method();
		logger.info("Incoming request:\n\tpath:" + path + ", method:" + method + ", uri:" + request.uri());
		try {
			Method m = this.getClass().getMethod(path, ChannelHandlerContext.class, FullHttpRequest.class);
			m.invoke(this, ctx, request);
		} catch (NoSuchMethodException e) {
			// Send error message, context does not exist
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
		logger.info("Flushed response!");
	}

	void writeDefaultResponse(ChannelHandlerContext ctx, FullHttpRequest request) throws UnsupportedEncodingException {
		String res = "I am OK";
		FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
				Unpooled.wrappedBuffer(res.getBytes("UTF-8")));
		response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
		response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
		if (HttpUtil.isKeepAlive(request)) {
			response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
		}
		ctx.writeAndFlush(response);
	}

	public void programbystation(ChannelHandlerContext ctx, FullHttpRequest request)
			throws UnsupportedEncodingException {
		HttpMethod method = request.method();
		final String uri = request.uri();
		QueryStringDecoder queryDecoder = new QueryStringDecoder(uri);

		Map<String, List<String>> params = null;

		// GET
		if (method == HttpMethod.GET) {
			params = queryDecoder.parameters();

			logger.info("-----\t\tSTART OF PARAMETERS\t\t-----");
			for (Entry<String, List<String>> entry : params.entrySet()) {
				logger.info(entry.getKey() + ":");
				for (String temp : entry.getValue()) {
					logger.info("\t" + temp);
				}
			}
			logger.info("-----\t\tEND OF PARAMETERS\t\t-----");
		}

		// POST
		else if (method == HttpMethod.POST) {
			ByteBuf content = request.content();
			if (content.isReadable()) {
				String param = content.toString(Charset.forName("UTF-8"));
				logger.info(param);
			}
		}

		writeDefaultResponse(ctx, request);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		logger.error(cause.getMessage());
		ctx.close();
	}
}