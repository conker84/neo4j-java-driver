/*
 * Copyright (c) 2002-2018 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
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
 */
package org.neo4j.driver.internal.handlers;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPromise;

import java.util.Map;

import org.neo4j.driver.internal.spi.ResponseHandler;
import org.neo4j.driver.internal.util.ServerVersion;
import org.neo4j.driver.v1.Value;

import static org.neo4j.driver.internal.async.ChannelAttributes.setConnectionId;
import static org.neo4j.driver.internal.async.ChannelAttributes.setServerVersion;

public class HelloResponseHandler implements ResponseHandler
{
    private static final String SERVER_METADATA_KEY = "server";
    private static final String CONNECTION_ID_METADATA_KEY = "connection_id";

    private final ChannelPromise connectionInitializedPromise;
    private final Channel channel;

    public HelloResponseHandler( ChannelPromise connectionInitializedPromise )
    {
        this.connectionInitializedPromise = connectionInitializedPromise;
        this.channel = connectionInitializedPromise.channel();
    }

    @Override
    public void onSuccess( Map<String,Value> metadata )
    {
        try
        {
            ServerVersion serverVersion = ServerVersion.version( extractMetadataValue( SERVER_METADATA_KEY, metadata ) );
            setServerVersion( channel, serverVersion );

            String connectionId = extractMetadataValue( CONNECTION_ID_METADATA_KEY, metadata );
            setConnectionId( channel, connectionId );

            connectionInitializedPromise.setSuccess();
        }
        catch ( Throwable error )
        {
            onFailure( error );
            throw error;
        }
    }

    @Override
    public void onFailure( Throwable error )
    {
        channel.close().addListener( future -> connectionInitializedPromise.setFailure( error ) );
    }

    @Override
    public void onRecord( Value[] fields )
    {
        throw new UnsupportedOperationException();
    }

    private static String extractMetadataValue( String key, Map<String,Value> metadata )
    {
        Value value = metadata.get( key );
        if ( value == null || value.isNull() )
        {
            throw new IllegalStateException( "Unable to extract " + key + " from a response to HELLO message. Metadata: " + metadata );
        }
        return value.asString();
    }
}
