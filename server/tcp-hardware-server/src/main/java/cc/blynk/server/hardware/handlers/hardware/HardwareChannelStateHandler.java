package cc.blynk.server.hardware.handlers.hardware;

import cc.blynk.server.Holder;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.device.Status;
import cc.blynk.server.core.model.widgets.notifications.Notification;
import cc.blynk.server.core.session.HardwareStateHolder;
import cc.blynk.server.notifications.push.GCMWrapper;
import cc.blynk.utils.properties.Placeholders;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.TimeUnit;

import static cc.blynk.server.internal.StateHolderUtil.getHardState;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/20/2015.
 * <p>
 * Removes channel from session in case it became inactive (closed from client side).
 */
@ChannelHandler.Sharable
public class HardwareChannelStateHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LogManager.getLogger(HardwareChannelStateHandler.class);

    private final SessionDao sessionDao;
    private final GCMWrapper gcmWrapper;
    private final int offlineDelay;
    private final String pushNotificationBody;

    public HardwareChannelStateHandler(Holder holder, int offlineDelay) {
        this.sessionDao = holder.sessionDao;
        this.gcmWrapper = holder.gcmWrapper;
        this.offlineDelay = offlineDelay;
        this.pushNotificationBody = holder.textHolder.pushNotificationBody;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        var hardwareChannel = ctx.channel();
        var state = getHardState(hardwareChannel);
        if (state != null) {
            var session = sessionDao.userSession.get(state.userKey);
            if (session != null) {
                var device = state.device;
                log.trace("Hardware channel disconnect for {}, dashId {}, deviceId {}, token {}.",
                          state.userKey, state.dash.id, device.id, device.token);
                int delay;
                if (device.hardwareInfo != null && device.hardwareInfo.heartbeatInterval != 10) {
                    delay = 50;
                } else {
                    delay = offlineDelay;
                }
                ctx.executor().schedule(new DelayedOfflineMessage(ctx, session, state),
                                        delay, TimeUnit.MILLISECONDS);
            }
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            log.trace("State handler. Hardware timeout disconnect. Event : {}. Closing.",
                      ((IdleStateEvent) evt).state());
            ctx.close();
        } else {
            ctx.fireUserEventTriggered(evt);
        }
    }

    private void sentOfflineMessage(ChannelHandlerContext ctx, Session session, DashBoard dashBoard, Device device) {
        //this is special case.
        //in case hardware quickly reconnects we do not mark it as disconnected
        //as it is already online after quick disconnect.
        //https://github.com/blynkkk/blynk-server/issues/403
        boolean isHardwareConnected = session.isHardwareConnected(dashBoard.id, device.id);
        if (!isHardwareConnected) {
            log.trace("Changing device status. DeviceId {}, dashId {}", device.id, dashBoard.id);
            device.disconnected();

            if (!dashBoard.isActive) {
                return;
            }

            Notification notification = dashBoard.getNotificationWidget();

            if (notification != null && notification.notifyWhenOffline) {
                sendPushNotification(ctx, notification, dashBoard.id, device);
            } else if (!dashBoard.isNotificationsOff) {
                session.sendOfflineMessageToApps(dashBoard.id, device.id);
            }
        } else {
            log.trace("Hardware was reconnected. Device id {}, token {}. No notifications.",
                      device.id, device.token);
        }
    }

    private void sendPushNotification(ChannelHandlerContext ctx,
                                      Notification notification, int dashId, Device device) {
        var deviceName = ((device == null || device.name == null) ? "device" : device.name);
        var message = pushNotificationBody.replace(Placeholders.DEVICE_NAME, deviceName);
        if (notification.notifyWhenOfflineIgnorePeriod == 0 || device == null) {
            notification.push(gcmWrapper,
                              message,
                              dashId
            );
        } else {
            //delayed notification
            //https://github.com/blynkkk/blynk-server/issues/493
            ctx.executor().schedule(new DelayedPush(device, notification, message, dashId),
                                    notification.notifyWhenOfflineIgnorePeriod, TimeUnit.MILLISECONDS);
        }
    }

    private final class DelayedPush implements Runnable {

        private final Device device;
        private final Notification notification;
        private final String message;
        private final int dashId;

        DelayedPush(Device device, Notification notification, String message, int dashId) {
            this.device = device;
            this.notification = notification;
            this.message = message;
            this.dashId = dashId;
        }

        @Override
        public void run() {
            final long now = System.currentTimeMillis();
            if (device.status == Status.OFFLINE
                    && now - device.disconnectTime >= notification.notifyWhenOfflineIgnorePeriod) {
                notification.push(gcmWrapper,
                                  message,
                                  dashId
                );
            }
        }
    }

    private final class DelayedOfflineMessage implements Runnable {

        private final ChannelHandlerContext ctx;
        private final Session session;
        private final HardwareStateHolder state;
        private final long submittedTime;

        DelayedOfflineMessage(ChannelHandlerContext ctx, Session session, HardwareStateHolder state) {
            this.ctx = ctx;
            this.session = session;
            this.state = state;
            this.submittedTime = System.currentTimeMillis();
        }

        @Override
        public void run() {
            if (submittedTime > state.device.connectTime) {
                sentOfflineMessage(ctx, session, state.dash, state.device);
            } else {
                log.trace("Hardware was logged. Delayed task skipped. Device id {}, token {}.",
                          state.device.id, state.device.token);
            }
        }
    }

}
