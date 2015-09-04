package com.ning.service;

import junit.framework.Assert;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingCluster;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by bestree007 on 2015/9/4.
 */
public class ZooKeeperServiceRegistryImplTest
{

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

    }

    @Test
    public void testRegisterServiceInstance() throws Exception
    {
        curatorFramework = CuratorFrameworkFactory.builder().
                connectionTimeoutMs(20000).sessionTimeoutMs(40000).
                connectString(connectString).retryPolicy(new ExponentialBackoffRetry(100, 3))
                .build();

        // prepare the path
        curatorFramework.start();
        curatorFramework.blockUntilConnected();
        ZooKeeperServiceRegistryImpl registry = new ZooKeeperServiceRegistryImpl(curatorFramework);

        ServiceInstance instance = new ServiceInstance();
        instance.setName("mockService1");
        instance.setLocation("url1");
        registry.registerServiceInstance(instance);

        Thread.currentThread().sleep(3000);
        List<ServiceInstance> results = registry.findInstanceList(instance.getName());
        assertEquals(1, results.size());
    }

    @Test
    public void testFindInstanceList() throws Exception
    {

    }
}