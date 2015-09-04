package com.ning.service;

import com.ning.service.discovery.NodeChangedListener;
import com.ning.service.discovery.PathMonitor;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The service registry impl
 * Created by bestree007 on 2015/9/3.
 */
public class ZooKeeperServiceRegistryImpl implements ServiceRegistry, NodeChangedListener
{
    Logger logger = LoggerFactory.getLogger(ZooKeeperServiceRegistryImpl.class);

    private CuratorFramework curatorFramework;

    private ConcurrentHashMap<String, Set<ServiceInstance>> instanceCache = new ConcurrentHashMap<String, Set<ServiceInstance>>();

    private ConcurrentHashMap<String, PathMonitor> pathMonitorConcurrentHashMap = new ConcurrentHashMap<String, PathMonitor>();

    public ZooKeeperServiceRegistryImpl(CuratorFramework curatorClient)
    {
        this.curatorFramework = curatorClient;
    }

    public boolean registerServiceInstance(ServiceInstance instance)
    {
        ZooKeeperServiceInstance zkInstance = new ZooKeeperServiceInstance(instance);
        try {
            curatorFramework.create().creatingParentsIfNeeded().forPath("/" + instance.getName() + "/" + zkInstance.getUniqueID(), Utils.serialize(instance));
        } catch (Exception ex) {
            logger.error("Exception occurs during the service instance:" + String.valueOf(instance.getName()));
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

}
