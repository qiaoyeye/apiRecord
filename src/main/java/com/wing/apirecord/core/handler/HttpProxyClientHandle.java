package com.wing.apirecord.core.handler;

import com.wing.apirecord.core.intercept.HttpProxyIntercept;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;

public class HttpProxyClientHandle extends ChannelInboundHandlerAdapter {

    private Channel clientChannel;
    private HttpProxyIntercept httpProxyHook;

    public HttpProxyClientHandle(Channel clientChannel) {
        this.clientChannel = clientChannel;
        this.httpProxyHook = ((HttpProxyServerHandle) clientChannel.pipeline().get("serverHandle")).getHttpProxyIntercept();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        HttpResponse response = null;
        if (msg instanceof HttpResponse) {
            response = (HttpResponse) msg;
            if (!httpProxyHook.afterResponse(clientChannel, ctx.channel(), response)) {
                return;
            }
        } else if (msg instanceof HttpContent) {
            if (!httpProxyHook.afterResponse(clientChannel, ctx.channel(), (HttpContent) msg)) {
                return;
            }
        }
        clientChannel.writeAndFlush(msg);
        if (response != null) {
            if (HttpHeaderValues.WEBSOCKET.toString().equals(response.headers().get(HttpHeaderNames.UPGRADE))) {
                //websocket转发原始报文
                ctx.pipeline().remove("httpCodec");
                clientChannel.pipeline().remove("httpCodec");
            }

        }
    }
}
