package com.ning.service.discovery;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

/**
 * Created by bestree007 on 2015/8/28.
 */
public class Test
{
    public static void main(String[] args)
    {
        CuratorFramework curatorFramework = CuratorFrameworkFactory.builder().
                connectionTimeoutMs(20000).sessionTimeoutMs(40000).
                connectString("localhost:2181").retryPolicy(new ExponentialBackoffRetry(100, 3))
                .build();
        try {
            curatorFramework.start();
            curatorFramework.blockUntilConnected();
            NodeChangedListenerDemo nodeChangedListenerDemo = new NodeChangedListenerDemo();
            PathMonitor monitor = new PathMonitor("/ning", curatorFramework, nodeChangedListenerDemo);
            monitor.monitor();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            Thread.currentThread().sleep(4000000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
