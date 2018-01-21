package com.meituan.hb.apirecord.core;

import com.meituan.hb.apirecord.core.handler.HttpProxyServerHandle;
import com.meituan.hb.apirecord.core.intercept.DefaultInterceptFactory;
import com.meituan.hb.apirecord.core.intercept.ProxyInterceptFactory;
import com.meituan.hb.apirecord.core.tools.CertUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.lang.reflect.Method;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;


public class NettyHttpProxyServer {

    public static HttpResponseStatus SUCCESS;
    public static SslContext clientSslCtx;
    public static String issuer;
    public static PrivateKey caPriKey;
    public static PublicKey caPubKey;
    public static PrivateKey serverPriKey;
    public static PublicKey serverPubKey;
    public static EventLoopGroup proxyGroup;

    private ProxyInterceptFactory proxyInterceptFactory;

    private void init() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        Method method = HttpResponseStatus.class.getDeclaredMethod("newStatus", int.class, String.class);
        method.setAccessible(true);
        SUCCESS = (HttpResponseStatus) method.invoke(null, 200, "Connection established");
        clientSslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        issuer = CertUtil.getSubject(classLoader.getResourceAsStream("ca.crt"));
        //CA私钥和公钥用于给动态生成的网站SSL证书签证
        caPriKey = CertUtil.loadPriKey(classLoader.getResourceAsStream("ca_private.pem"));
        caPubKey = CertUtil.loadPubKey(classLoader.getResourceAsStream("ca_public.der"));
        //生产一对随机公私钥用于网站SSL证书动态创建
        KeyPair keyPair = CertUtil.genKeyPair();
        serverPriKey = keyPair.getPrivate();
        serverPubKey = keyPair.getPublic();
        proxyGroup = new NioEventLoopGroup();
        if (proxyInterceptFactory == null) {
            proxyInterceptFactory = new DefaultInterceptFactory();
        }
    }

    public NettyHttpProxyServer initProxyInterceptFactory(ProxyInterceptFactory proxyInterceptFactory) {
        this.proxyInterceptFactory = proxyInterceptFactory;
        return this;
    }

    public void start(int port) {

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
//        ChannelInboundHandlerAdapter
        try {
            init();
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
//                    .option(ChannelOption.SO_BACKLOG, 100)
                    .option(ChannelOption.TCP_NODELAY, true)
//                    .handler(new LoggingHandler(LogLevel.ERROR))
                    .childHandler(new ChannelInitializer<Channel>() {

                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            ch.pipeline().addLast("httpCodec", new HttpServerCodec());
                            ch.pipeline().addLast(new ReadTimeoutHandler(10));
                            ch.pipeline().addLast(new WriteTimeoutHandler(10));
                            ch.pipeline().addLast("serverHandle", new HttpProxyServerHandle(proxyInterceptFactory.build()));
                        }
                    });
            ChannelFuture f = b
                    .bind(port)
                    .sync();
            f.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

}
