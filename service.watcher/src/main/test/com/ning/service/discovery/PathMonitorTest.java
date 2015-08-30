package com.ning.service.discovery;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingCluster;
import org.junit.*;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by bestree007 on 2015/8/30.
 */
public class PathMonitorTest
{

    private PathMonitor pathMonitor;

    private String connectString;

    private CuratorFramework curatorFramework;

    @Before
    public void setUp() throws Exception
    {

        TestingCluster testingCluster = new TestingCluster(3);
        testingCluster.start();
        connectString = testingCluster.getConnectString();

    }

    @After
    public void tearDown() throws Exception
    {
        pathMonitor.shutdown();
        connectString = null;
        curatorFramework.close();
    }

    @org.junit.Test
    public void testMonitor() throws Exception
    {

        curatorFramework = CuratorFrameworkFactory.builder().
                connectionTimeoutMs(20000).sessionTimeoutMs(40000).
                connectString(connectString).retryPolicy(new ExponentialBackoffRetry(100, 3))
                .build();

        // prepare the path
        curatorFramework.start();
        curatorFramework.blockUntilConnected();
        curatorFramework.create().creatingParentsIfNeeded().
                forPath("/ning/p1", "p1".getBytes("UTF-8"));
        try {
            final ConcurrentHashMap<String, String> instances = new ConcurrentHashMap<String, String>();
            NodeChangedListener nodeChangedListenerDemo = new NodeChangedListener()
            {
                public void updateData(ChangedData changedData)
                {
                    if (changedData.getType() == OperationType.DELETE) {
                        instances.remove(changedData.getNodeName());
                    } else if (changedData.getType() == OperationType.UPDATE) {
                        instances.put(changedData.getNodeName(), new String(changedData.getData()));
                    }
                }
            };

            pathMonitor = new PathMonitor("/ning", curatorFramework, nodeChangedListenerDemo);
            pathMonitor.monitor();

            curatorFramework.create().creatingParentsIfNeeded().forPath("/ning/p2", "p2".getBytes("UTF-8"));
            Thread.currentThread().sleep(1000);
            Assert.assertNotNull(instances.get("p1"));
            Assert.assertNotNull(instances.get("p2"));

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}