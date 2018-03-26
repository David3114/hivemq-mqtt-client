package org.mqttbee.mqtt.codec.encoder.mqtt5;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mqttbee.api.mqtt.mqtt5.message.publish.pubrel.Mqtt5PubRelReasonCode;
import org.mqttbee.mqtt.datatypes.MqttUTF8StringImpl;
import org.mqttbee.mqtt.datatypes.MqttUserPropertiesImpl;
import org.mqttbee.mqtt.datatypes.MqttVariableByteInteger;
import org.mqttbee.mqtt.message.publish.pubrel.MqttPubRel;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.mqttbee.api.mqtt.mqtt5.message.publish.pubrel.Mqtt5PubRelReasonCode.SUCCESS;
import static org.mqttbee.mqtt.datatypes.MqttVariableByteInteger.MAXIMUM_PACKET_SIZE_LIMIT;

/**
 * @author David Katz
 */
class Mqtt5PubRelEncoderTest extends AbstractMqtt5EncoderWithUserPropertiesTest {

    Mqtt5PubRelEncoderTest() {
        super(true);
    }

    @Test
    void encode_simple() {
        final byte[] expected = {
                // fixed header
                //   type, flags
                0b0110_0010,
                //   remaining length
                3,
                // variable header
                //   packet identifier
                0, 5,
                // reason code
                (byte) 0x92
        };

        final Mqtt5PubRelReasonCode reasonCode = Mqtt5PubRelReasonCode.PACKET_IDENTIFIER_NOT_FOUND;
        final MqttUTF8StringImpl reasonString = null;
        final MqttUserPropertiesImpl userProperties = MqttUserPropertiesImpl.NO_USER_PROPERTIES;
        final MqttPubRel pubRel =
                new MqttPubRel(5, reasonCode, reasonString, userProperties, Mqtt5PubRelEncoder.PROVIDER);

        encode(expected, pubRel);
    }

    @Test
    void encode_reasonCodeOmittedWhenSuccessWithoutProperties() {
        final byte[] expected = {
                // fixed header
                //   type, flags
                0b0110_0010,
                //   remaining length
                2,
                // variable header
                //   packet identifier
                0, 5
        };

        final MqttPubRel pubRel = new MqttPubRel(5, SUCCESS, null, MqttUserPropertiesImpl.NO_USER_PROPERTIES,
                Mqtt5PubRelEncoder.PROVIDER);

        encode(expected, pubRel);
    }

    @ParameterizedTest
    @EnumSource(value = Mqtt5PubRelReasonCode.class, mode = EXCLUDE, names = {"SUCCESS"})
    void encode_reasonCodes(final Mqtt5PubRelReasonCode reasonCode) {
        final byte[] expected = {
                // fixed header
                //   type, flags
                0b0110_0010,
                //   remaining length
                3,
                // variable header
                //   packet identifier
                6, 5,
                //   reason code placeholder
                (byte) 0xFF
        };

        expected[4] = (byte) reasonCode.getCode();
        final MqttPubRel pubRel = new MqttPubRel(0x0605, reasonCode, null, MqttUserPropertiesImpl.NO_USER_PROPERTIES,
                Mqtt5PubRelEncoder.PROVIDER);

        encode(expected, pubRel);
    }

    @Test
    void encode_reasonString() {
        final byte[] expected = {
                // fixed header
                //   type, flags
                0b0110_0010,
                //   remaining length
                13,
                // variable header
                //   packet identifier
                0, 9,
                //   reason code
                (byte) 0x92,
                //   properties
                9,
                // reason string
                0x1F, 0, 6, 'r', 'e', 'a', 's', 'o', 'n'
        };

        final Mqtt5PubRelReasonCode reasonCode = Mqtt5PubRelReasonCode.PACKET_IDENTIFIER_NOT_FOUND;
        final MqttUTF8StringImpl reasonString = MqttUTF8StringImpl.from("reason");
        final MqttUserPropertiesImpl userProperties = MqttUserPropertiesImpl.NO_USER_PROPERTIES;
        final MqttPubRel pubRel =
                new MqttPubRel(9, reasonCode, reasonString, userProperties, Mqtt5PubRelEncoder.PROVIDER);

        encode(expected, pubRel);
    }

    @Test
    void encode_userProperty() {
        final byte[] expected = {
                // fixed header
                //   type, flags
                0b0110_0010,
                //   remaining length
                21,
                // variable header
                //   packet identifier
                0, 5,
                //   reason code
                (byte) 0x92,
                //   properties
                17,
                // user Property
                0x26, 0, 4, 'u', 's', 'e', 'r', 0, 8, 'p', 'r', 'o', 'p', 'e', 'r', 't', 'y'
        };

        final Mqtt5PubRelReasonCode reasonCode = Mqtt5PubRelReasonCode.PACKET_IDENTIFIER_NOT_FOUND;
        final MqttUserPropertiesImpl userProperties = getUserProperties(1);
        final MqttPubRel pubRel = new MqttPubRel(5, reasonCode, null, userProperties, Mqtt5PubRelEncoder.PROVIDER);

        encode(expected, pubRel);
    }

    @Test
    void encode_maximumPacketSizeExceeded_omitUserProperties() {
        final byte[] expected = {
                // fixed header
                //   type, flags
                0b0110_0010,
                //   remaining length
                3,
                // variable header
                //   packet identifier
                0, 5,
                // reason code
                (byte) 0x92
        };

        createServerConnectionData(expected.length + 2);
        final Mqtt5PubRelReasonCode reasonCode = Mqtt5PubRelReasonCode.PACKET_IDENTIFIER_NOT_FOUND;
        final MqttUTF8StringImpl reasonString = null;
        final MqttUserPropertiesImpl userProperties = getUserProperties(1);
        final MqttPubRel pubRel =
                new MqttPubRel(5, reasonCode, reasonString, userProperties, Mqtt5PubRelEncoder.PROVIDER);

        encode(expected, pubRel);
    }

    @Test
    void encode_maximumPacketSizeExceeded_omitReasonString() {
        final byte[] expected = {
                // fixed header
                //   type, flags
                0b0110_0010,
                //   remaining length
                21,
                // variable header
                //   packet identifier
                0, 5,
                //   reason code
                (byte) 0x92,
                //   properties
                17,
                // user Property
                0x26, 0, 4, 'u', 's', 'e', 'r', 0, 8, 'p', 'r', 'o', 'p', 'e', 'r', 't', 'y'
        };
        createServerConnectionData(expected.length + 2);
        final Mqtt5PubRelReasonCode reasonCode = Mqtt5PubRelReasonCode.PACKET_IDENTIFIER_NOT_FOUND;
        final MqttUserPropertiesImpl userProperties = getUserProperties(1);
        final MqttPubRel pubRel = new MqttPubRel(5, reasonCode, null, userProperties, Mqtt5PubRelEncoder.PROVIDER);

        encode(expected, pubRel);
    }

    @Test
    void encode_propertyLengthExceeded_omitUserProperties() {
        final byte[] expected = {
                // fixed header
                //   type, flags
                0b0110_0010,
                //   remaining length
                3,
                // variable header
                //   packet identifier
                0, 5,
                // reason code
                (byte) 0x92
        };

        createServerConnectionData(expected.length + 2);
        final Mqtt5PubRelReasonCode reasonCode = Mqtt5PubRelReasonCode.PACKET_IDENTIFIER_NOT_FOUND;
        final MqttUTF8StringImpl reasonString = null;
        final MqttUserPropertiesImpl userProperties =
                getUserProperties((VARIABLE_BYTE_INTEGER_FOUR_BYTES_MAX_VALUE / userPropertyBytes) + 1);

        final MqttPubRel pubRel =
                new MqttPubRel(5, reasonCode, reasonString, userProperties, Mqtt5PubRelEncoder.PROVIDER);

        encode(expected, pubRel);
    }

    @Test
    void encode_propertyLengthExceeded_omitReasonString() {
        final MaximumPacketBuilder maxPacket = new MaximumPacketBuilder().build();
        final MqttUserPropertiesImpl maxUserProperties = maxPacket.getMaxPossibleUserProperties();

        final ByteBuf expected = Unpooled.buffer(MAXIMUM_PACKET_SIZE_LIMIT - maxPacket.getRemainingPropertyBytes(),
                MAXIMUM_PACKET_SIZE_LIMIT - maxPacket.getRemainingPropertyBytes());

        // fixed header
        // type, reserved
        expected.writeByte(0b0110_0010);
        // remaining length (2 + 1 + 4 + (userPropertyBytes * maxPossibleUserPropertiesCount) = 268435447
        expected.writeByte(0xf7);
        expected.writeByte(0xff);
        expected.writeByte(0xff);
        expected.writeByte(0x7f);
        // packet identifier
        expected.writeByte(0);
        expected.writeByte(5);
        // reason code
        expected.writeByte(0x92);
        // properties length
        expected.writeByte(0xf0);
        expected.writeByte(0xff);
        expected.writeByte(0xff);
        expected.writeByte(0x7f);
        // user properties
        maxUserProperties.encode(expected);

        final Mqtt5PubRelReasonCode reasonCode = Mqtt5PubRelReasonCode.PACKET_IDENTIFIER_NOT_FOUND;
        final MqttUTF8StringImpl reasonString = maxPacket.getPaddedUtf8StringTooLong();
        final MqttPubRel pubRel =
                new MqttPubRel(5, reasonCode, reasonString, maxUserProperties, Mqtt5PubRelEncoder.PROVIDER);
        encode(expected.array(), pubRel);
        expected.release();
    }


    private void encode(final byte[] expected, final MqttPubRel pubRel) {
        channel.writeOutbound(pubRel);
        final ByteBuf byteBuf = channel.readOutbound();

        final byte[] actual = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(actual);
        byteBuf.release();

        assertArrayEquals(expected, actual);
    }

    @Override
    int getMaxPropertyLength() {
        return MqttVariableByteInteger.MAXIMUM_PACKET_SIZE_LIMIT - 1  // type, reserved
                - 4  // remaining length
                - 4  // property length
                - 2  // packet identifier
                - 1; // reason code;
    }
}