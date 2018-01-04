package org.mqttbee.mqtt5.codec.decoder;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.mqttbee.mqtt5.codec.Mqtt5DataTypes;
import org.mqttbee.mqtt5.message.Mqtt5Message;
import org.mqttbee.mqtt5.message.disconnect.Mqtt5DisconnectReasonCode;

import javax.inject.Inject;
import java.util.List;

import static org.mqttbee.mqtt5.codec.Mqtt5CodecUtil.checkMaximumPacketSize;
import static org.mqttbee.mqtt5.codec.decoder.Mqtt5MessageDecoderUtil.disconnect;

/**
 * @author Silvio Giebl
 */
public class Mqtt5Decoder extends ByteToMessageDecoder {

    private final Mqtt5MessageDecoders decoders;

    @Inject
    public Mqtt5Decoder(final Mqtt5MessageDecoders decoders) {
        this.decoders = decoders;
    }

    @Override
    protected void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out) throws Exception {
        if (in.readableBytes() < 2) {
            return;
        }
        in.markReaderIndex();
        final int readerIndexBeforeFixedHeader = in.readerIndex();

        final byte fixedHeader = in.readByte();
        final int remainingLength = Mqtt5DataTypes.decodeVariableByteInteger(in);

        if (remainingLength == Mqtt5DataTypes.VARIABLE_BYTE_INTEGER_NOT_ENOUGH_BYTES) {
            in.resetReaderIndex();
            return;
        }

        if (remainingLength == Mqtt5DataTypes.VARIABLE_BYTE_INTEGER_TOO_LARGE ||
                remainingLength == Mqtt5DataTypes.VARIABLE_BYTE_INTEGER_NOT_MINIMUM_BYTES) {

            disconnect(Mqtt5DisconnectReasonCode.MALFORMED_PACKET, "malformed remaining length", ctx.channel(), in);
            return;
        }

        final int readerIndexAfterFixedHeader = in.readerIndex();
        final int fixedHeaderLength = readerIndexAfterFixedHeader - readerIndexBeforeFixedHeader;
        final int packetSize = fixedHeaderLength + remainingLength;

        if (!checkMaximumPacketSize(packetSize, ctx.channel())) {
            disconnect(Mqtt5DisconnectReasonCode.PACKET_TOO_LARGE, null, ctx.channel(), in);
            return;
        }

        if (in.readableBytes() < remainingLength) {
            in.resetReaderIndex();
            return;
        }

        final int messageType = fixedHeader >> 4;
        final int flags = fixedHeader & 0xF;
        final ByteBuf messageBuffer = in.readSlice(remainingLength);
        in.markReaderIndex();

        final Mqtt5MessageDecoder decoder = decoders.get(messageType);
        if (decoder == null) {
            disconnect(Mqtt5DisconnectReasonCode.PROTOCOL_ERROR, "wrong packet", ctx.channel(), in);
            return;
        }

        final Mqtt5Message message = decoder.decode(flags, ctx.channel(), messageBuffer);

        if (message != null) {
            out.add(message);
        }
    }

}
