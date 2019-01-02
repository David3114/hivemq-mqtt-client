/*
 * Copyright 2018 The MQTT Bee project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.mqttbee.internal.mqtt.handler;

import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.ScheduledFuture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mqttbee.annotations.CallByThread;
import org.mqttbee.internal.mqtt.MqttClientConfig;
import org.mqttbee.internal.mqtt.MqttClientConnectionConfig;
import org.mqttbee.internal.mqtt.MqttServerConnectionConfig;
import org.mqttbee.internal.mqtt.codec.decoder.MqttDecoder;
import org.mqttbee.internal.mqtt.handler.publish.incoming.MqttIncomingQosHandler;
import org.mqttbee.internal.mqtt.handler.publish.outgoing.MqttOutgoingQosHandler;
import org.mqttbee.internal.mqtt.handler.subscribe.MqttSubscriptionHandler;
import org.mqttbee.internal.mqtt.ioc.ClientScope;
import org.mqttbee.internal.mqtt.message.connect.MqttConnect;
import org.mqttbee.internal.mqtt.message.connect.connack.MqttConnAck;
import org.mqttbee.mqtt.exceptions.MqttSessionExpiredException;
import org.mqttbee.mqtt.mqtt5.exceptions.Mqtt5ConnAckException;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

/**
 * @author Silvio Giebl
 */
@ClientScope
public class MqttSession {

    private final @NotNull MqttClientConfig clientConfig;
    private final @NotNull MqttSubscriptionHandler subscriptionHandler;
    private final @NotNull MqttIncomingQosHandler incomingQosHandler;
    private final @NotNull MqttOutgoingQosHandler outgoingQosHandler;
    private boolean hasSession;
    private @Nullable ScheduledFuture<?> expireFuture;

    @Inject
    MqttSession(
            final @NotNull MqttClientConfig clientConfig, final @NotNull MqttSubscriptionHandler subscriptionHandler,
            final @NotNull MqttIncomingQosHandler incomingQosHandler,
            final @NotNull MqttOutgoingQosHandler outgoingQosHandler) {

        this.clientConfig = clientConfig;
        this.subscriptionHandler = subscriptionHandler;
        this.incomingQosHandler = incomingQosHandler;
        this.outgoingQosHandler = outgoingQosHandler;
    }

    @CallByThread("Netty EventLoop")
    public void startOrResume(
            final @NotNull MqttConnAck connAck, final @NotNull ChannelPipeline pipeline,
            final @NotNull MqttClientConnectionConfig clientConnectionConfig,
            final @NotNull MqttServerConnectionConfig serverConnectionConfig) {

        if (hasSession && !connAck.isSessionPresent()) {
            final String message = "Session expired as CONNACK did not contain the session present flag.";
            end(new MqttSessionExpiredException(message, new Mqtt5ConnAckException(connAck, message)));
        }
        hasSession = true;

        if (expireFuture != null) {
            expireFuture.cancel(false);
            expireFuture = null;
        }

        pipeline.addAfter(MqttDecoder.NAME, MqttSubscriptionHandler.NAME, subscriptionHandler);
        pipeline.addAfter(MqttDecoder.NAME, MqttIncomingQosHandler.NAME, incomingQosHandler);
        pipeline.addAfter(MqttDecoder.NAME, MqttOutgoingQosHandler.NAME, outgoingQosHandler);
        subscriptionHandler.onSessionStartOrResume(clientConnectionConfig, serverConnectionConfig);
        incomingQosHandler.onSessionStartOrResume(clientConnectionConfig, serverConnectionConfig);
        outgoingQosHandler.onSessionStartOrResume(clientConnectionConfig, serverConnectionConfig);
    }

    @CallByThread("Netty EventLoop")
    public void expire(final @NotNull Throwable cause, final @NotNull EventLoop eventLoop) {
        final MqttClientConnectionConfig clientConnectionConfig = clientConfig.getRawClientConnectionConfig();
        if (clientConnectionConfig != null) {
            final long expiryInterval = clientConnectionConfig.getSessionExpiryInterval();

            if (expiryInterval == 0) {
                end(new MqttSessionExpiredException("Session expired as connection was closed.", cause));
            } else if (expiryInterval != MqttConnect.NO_SESSION_EXPIRY) {
                expireFuture = eventLoop.schedule(() -> {
                    if (expireFuture != null) {
                        expireFuture = null;
                        end(new MqttSessionExpiredException("Session expired after expiry interval", cause));
                    }
                }, (long) (TimeUnit.SECONDS.toMillis(expiryInterval) * 1.1), TimeUnit.MILLISECONDS);
            }
        }
    }

    @CallByThread("Netty EventLoop")
    private void end(final @NotNull Throwable cause) {
        if (hasSession) {
            hasSession = false;
            outgoingQosHandler.onSessionEnd(cause);
            incomingQosHandler.onSessionEnd(cause);
            subscriptionHandler.onSessionEnd(cause);
        }
    }
}