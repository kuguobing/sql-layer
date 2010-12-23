package com.akiban.admin;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.akiban.message.Message;
import com.akiban.message.Sendable;

public class Address implements Sendable
{
    // Object interface

    @Override
    public String toString()
    {
        return String.format("%s:%s", host.getHostName(), port);
    }

    @Override
    public boolean equals(Object o)
    {
        Address that = (Address) o;
        return this.host.equals(that.host) && this.port == that.port;
    }

    // Sendable interface

    @Override
    public void read(ByteBuffer payload)
    {
        String hostAddress = null;
        try {
            hostAddress = Message.readString(payload);
            host = InetAddress.getByName(hostAddress);
        } catch (UnknownHostException e) {
            logger.error(String.format("Unable to create InetAddress for %s", hostAddress));
        }
        port = payload.getInt();
    }

    @Override
    public void write(ByteBuffer payload)
    {
        Message.writeString(payload, host.getHostAddress());
        payload.putInt(port);
    }

    // Address interface

    public Address(String hostAndPort)
    {
        String[] tokens = hostAndPort.split(":");
        try {
            this.host = InetAddress.getByName(tokens[0]);
        } catch (UnknownHostException e) {
            logger.error(String.format("Caught UnknownHostException trying to resolve host name %s", tokens[0]));
        }
        this.port = Integer.parseInt(tokens[1]);
    }

    public Address()
    {}

    public InetAddress host()
    {
        return host;
    }

    public int port()
    {
        return port;
    }

    // State

    private static final Log logger = LogFactory.getLog(Address.class);

    private InetAddress host;
    private int port;
}
