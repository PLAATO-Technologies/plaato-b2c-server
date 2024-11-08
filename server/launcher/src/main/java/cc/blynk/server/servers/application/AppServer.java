package cc.blynk.server.servers.application;

import cc.blynk.server.Holder;
import cc.blynk.server.application.handlers.main.MobileChannelStateHandler;
import cc.blynk.server.application.handlers.main.auth.MobileGetServerHandler;
import cc.blynk.server.application.handlers.main.auth.MobileLoginHandler;
import cc.blynk.server.application.handlers.main.auth.MobileRegisterHandler;
import cc.blynk.server.application.handlers.sharing.auth.MobileShareLoginHandler;
import cc.blynk.server.common.handlers.UserNotLoggedHandler;
import cc.blynk.server.core.protocol.handlers.decoders.MessageDecoder;
import cc.blynk.server.core.protocol.handlers.encoders.MessageEncoder;
import cc.blynk.server.servers.BaseServer;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * Class responsible for handling all Application connections and netty pipeline initialization.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
@Deprecated
//todo remove in future releases
//Leaving it only for back compatibility
//should be no longer used
public class AppServer extends BaseServer {

    private final ChannelInitializer<SocketChannel> channelInitializer;

    public AppServer(Holder holder) {
        super(holder.props.getProperty("listen.address"),
                holder.props.getIntProperty("app.ssl.port", 8443), holder.transportTypeHolder);

        final MobileChannelStateHandler appChannelStateHandler = new MobileChannelStateHandler(holder.sessionDao);
        final MobileRegisterHandler registerHandler = new MobileRegisterHandler(holder);
        final MobileLoginHandler appLoginHandler = new MobileLoginHandler(holder);
        final MobileShareLoginHandler appShareLoginHandler = new MobileShareLoginHandler(holder);
        final UserNotLoggedHandler userNotLoggedHandler = new UserNotLoggedHandler();
        final MobileGetServerHandler getServerHandler = new MobileGetServerHandler(holder);

        final int readTimeout = 600;
        log.debug("app.socket.idle.timeout = {}", readTimeout);

        this.channelInitializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                //600 specifies maximum seconds when application socket could be idle. After which
                //socket will be closed due to non activity. In seconds.
                ch.pipeline()
                        .addLast("AReadTimeout", new IdleStateHandler(readTimeout, 0, 0))
                        .addLast("AChannelState", appChannelStateHandler)
                        .addLast("ASSL", holder.sslContextHolder.sslCtx.newHandler(ch.alloc()))
                        .addLast("AMessageDecoder", new MessageDecoder(holder.stats, holder.limits))
                        .addLast("AMessageEncoder", new MessageEncoder(holder.stats))
                        .addLast("AGetServer", getServerHandler)
                        .addLast("ARegister", registerHandler)
                        .addLast("ALogin", appLoginHandler)
                        .addLast("AShareLogin", appShareLoginHandler)
                        .addLast("ANotLogged", userNotLoggedHandler);
            }
        };
    }

    @Override
    public ChannelInitializer<SocketChannel> getChannelInitializer() {
        return channelInitializer;
    }

    @Override
    protected String getServerName() {
        return "Application";
    }

    @Override
    public ChannelFuture close() {
        System.out.println("Shutting down Application SSL server...");
        return super.close();
    }

}
