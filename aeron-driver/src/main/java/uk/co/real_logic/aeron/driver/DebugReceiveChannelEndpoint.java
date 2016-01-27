/*
 * Copyright 2016 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.real_logic.aeron.driver;

import uk.co.real_logic.aeron.driver.event.EventLogger;
import uk.co.real_logic.aeron.driver.media.ReceiveChannelEndpoint;
import uk.co.real_logic.aeron.driver.media.UdpChannel;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class DebugReceiveChannelEndpoint extends ReceiveChannelEndpoint
{
    private final InetSocketAddress connectAddres;
    private final EventLogger logger;
    private final LossGenerator dataLossGenerator;
    private final LossGenerator controlLossGenerator;

    public DebugReceiveChannelEndpoint(
        final UdpChannel udpChannel,
        final DataPacketDispatcher dispatcher,
        final MediaDriver.Context context)
    {
        super(udpChannel, dispatcher, context);

        connectAddres = udpChannel.remoteData();
        logger = context.eventLogger();
        dataLossGenerator = context.dataLossGenerator();
        controlLossGenerator = context.controlLossGenerator();
    }

    public int sendTo(final ByteBuffer buffer, final InetSocketAddress remoteAddress)
    {
        logger.logFrameOut(buffer, remoteAddress);

        // TODO: call controlLossGenerator and drop (call log to inform) - need a shouldDropAllFrame() method

        return super.sendTo(buffer, remoteAddress);
    }

    public int send(final ByteBuffer buffer)
    {
        logger.logFrameOut(buffer, connectAddres);

        // TODO: call controlLossGenerator and drop (call log to inform) - need a shouldDropAllFrame() method

        return super.send(buffer);
    }

    protected int dispatch(final UnsafeBuffer buffer, final int length, final InetSocketAddress srcAddress)
    {
        int result = 0;
        final ByteBuffer receiveByteBuffer = receiveByteBuffer();

        if (dataLossGenerator.shouldDropFrame(srcAddress, buffer, length))
        {
            logger.logFrameInDropped(receiveByteBuffer, 0, length, srcAddress);
        }
        else
        {
            logger.logFrameIn(receiveByteBuffer, 0, length, srcAddress);

            if (isValidFrame(buffer, length))
            {
                result = super.dispatch(buffer, length, srcAddress);
            }
        }

        return result;
    }
}
