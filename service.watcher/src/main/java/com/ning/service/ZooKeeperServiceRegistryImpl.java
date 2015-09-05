package com.ning.service;

import com.ning.service.discovery.NodeChangedListener;
import com.ning.service.discovery.PathMonitor;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The service registry impl
 * Created by bestree007 on 2015/9/3.
 */
public class ZooKeeperServiceRegistryImpl implements ServiceRegistry, NodeChangedListener, ConnectionStateListener
{
    Logger logger = LoggerFactory.getLogger(ZooKeeperServiceRegistryImpl.class);

    private CuratorFramework curatorFramework;

    private ConcurrentHashMap<String, Set<ServiceInstance>> instanceCache = new ConcurrentHashMap<String, Set<ServiceInstance>>();

    private ConcurrentHashMap<String, PathMonitor> pathMonitorConcurrentHashMap = new ConcurrentHashMap<String, PathMonitor>();

    // this cache used when the connection loss and then reconnected
    private Set<ServiceInstance> serviceInstancesRegistryCache = Collections.synchronizedSet(new HashSet<ServiceInstance>());

    public ZooKeeperServiceRegistryImpl(CuratorFramework curatorClient)
    {
        this.curatorFramework = curatorClient;
        this.curatorFramework.getConnectionStateListenable().addListener(this);
    }

    public boolean registerServiceInstance(ServiceInstance instance)
    {
        serviceInstancesRegistryCache.add(instance);
        ZooKeeperServiceInstance zkInstance = new ZooKeeperServiceInstance(instance);
        try {
            String path = "/" + instance.getName() + "/" + zkInstance.getUniqueID();
            // create the ephemeral node
            Stat stat = curatorFramework.checkExists().forPath(path);
            if (stat == null) {
                curatorFramework.create().creatingParentsIfNeeded()
                        .withMode(CreateMode.EPHEMERAL).forPath(path, Utils.serialize(instance));
            } else {
                long ownerID = stat.getEphemeralOwner();
                long currentSessionID = curatorFramework.getZookeeperClient().getZooKeeper().getSessionId();
                if (ownerID == currentSessionID) {
                    curatorFramework.setData().forPath(path, Utils.serialize(instance));
                } else {
                    try {
                        curatorFramework.delete().forPath(path);
                    } catch (KeeperException.NoNodeException ex) {

                    } finally {
                        curatorFramework.create().creatingParentsIfNeeded()
                                .withMode(CreateMode.EPHEMERAL).forPath(path, Utils.serialize(instance));
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("Exception occurs during the service instance registry:" + String.valueOf(instance.getName()));
            return false;
        }
        return true;
    }

    public List<ServiceInstance> findInstanceList(String serviceName)
    {
        PathMonitor pathMonitor = pathMonitorConcurrentHashMap.get(serviceName);
        if (null == pathMonitor) {
            PathMonitor currentPathMonitor = new PathMonitor("/" + serviceName, curatorFramework, this);
            pathMonitor = pathMonitorConcurrentHashMap.putIfAbsent(serviceName, currentPathMonitor);
            if (null == pathMonitor) {
                pathMonitor = currentPathMonitor;
            }
        }

        pathMonitor.monitor();
        instanceCache.get(serviceName);
        return new ArrayList<ServiceInstance>(instanceCache.get(serviceName));
    }

    public void updateData(ChangedData changedData)
    {
        String name = changedData.getBaseName();
        Set<ServiceInstance> instances = instanceCache.get(name);
        if (null == instances) {
            Set<ServiceInstance> currentInstances = new HashSet<ServiceInstance>();
            Set<ServiceInstance> previousInstances = instanceCache.putIfAbsent(name, currentInstances);
            if (previousInstances == null) {
                instances = currentInstances;
            } else {
                instances = previousInstances;
            }
        }
        OperationType operationType = changedData.getType();

        synchronized (instances) {
            switch (operationType) {
                case DELETE: {
                    String nodeName = changedData.getNodeName();
                    ServiceInstance instance = Utils.getServiceInstance(nodeName);
                    if (instance == null) {
                        logger.error("Error happens when updating the data from zookeeper registry."
                                + String.valueOf(changedData));
                    } else {
                        instances.remove(instance);
                    }
                    break;
                }
                case UPDATE: {
                    ServiceInstance instance = Utils.deserialize(changedData.getData());
                    instances.add(instance);
                    break;
                }
                default: {
                    logger.error("Unexpected operation type :" + String.valueOf(operationType));
                }

            }

        }

    }

    public void cleanup()
    {
        this.instanceCache.clear();
        this.pathMonitorConcurrentHashMap.clear();
    }

    public void stateChanged(CuratorFramework client, ConnectionState newState)
    {
        switch (newState) {
            case RECONNECTED: {
                for (ServiceInstance instance : serviceInstancesRegistryCache) {
                    try {
                        registerServiceInstance(instance);
                    } catch (Throwable e) {
                        logger.error("Error occurs during the recovering.", e);
                    }
                }
            }
        }
    }
}
