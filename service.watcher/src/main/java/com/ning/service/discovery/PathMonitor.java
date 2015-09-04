package com.ning.service.discovery;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.BackgroundCallback;
import org.apache.curator.framework.api.CuratorEvent;
import org.apache.curator.framework.api.CuratorEventType;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Created by bestree007 on 2015/8/28.
 */
public class PathMonitor
{
    // the logger
    Logger logger = LoggerFactory.getLogger(PathMonitor.class);

    // the base path we will watch.
    private String basePath;

    private String baseNodeName;

    // the client
    private CuratorFramework client;

    // the data change listener

    private NodeChangedListener dataChangeListener;

    // path separator

    private char PATH_SEPARATOR = '/';

    // cache the children set
    private Set<String> childrenNameCache = Collections.synchronizedSet(new HashSet<String>());

    private AtomicBoolean started = new AtomicBoolean(false);

    public PathMonitor(String basePath, CuratorFramework client, NodeChangedListener listener)
    {
        this.basePath = basePath;
        this.baseNodeName = basePath.substring(basePath.lastIndexOf(PATH_SEPARATOR) + 1);
        this.client = client;
        this.dataChangeListener = listener;
        // the listener which listen the connection;
        ConnectionStateListener connectionStateListener = new ConnectionStateListener()
        {
            public void stateChanged(CuratorFramework curatorFramework, ConnectionState connectionState)
            {
                if (connectionState == ConnectionState.RECONNECTED) {
                    logger.error("The connection reconnected successfully.");
                    childrenNameCache.clear();
                    monitor();
                } else if (connectionState == ConnectionState.LOST) {
                    logger.error("The connection is lost.");
                }
            }
        };
        client.getConnectionStateListenable().addListener(connectionStateListener);
    }


    // the watcher watch the base node
    private Watcher baseNodeWatcher = new Watcher()
    {
        public void process(WatchedEvent watchedEvent)
        {
            if (watchedEvent.getType() == Event.EventType.NodeChildrenChanged) {
                // It means that the children node added of deleted
                monitorChildrenNodes(watchedEvent.getPath());
            }
        }
    };

    // the watcher watch the child node
    private Watcher childNodeWatcher = new Watcher()
    {
        public void process(WatchedEvent watchedEvent)
        {
            if (watchedEvent.getType() == Event.EventType.NodeDeleted) {
                String path = watchedEvent.getPath();
                String childrenName = path.substring(path.lastIndexOf(PATH_SEPARATOR) + 1);
                childrenNameCache.remove(childrenName);
                dataChangeListener.updateData(new NodeChangedListener.
                        ChangedData(NodeChangedListener.OperationType.DELETE, null, childrenName, baseNodeName));
            } else if (watchedEvent.getType() == Event.EventType.NodeDataChanged) {
                monitorChildrenNode(watchedEvent.getPath());
            }
        }
    };

    /**
     * monitor single child data change
     *
     * @param path the child path
     */
    private void monitorChildrenNode(String path)
    {
        try {
            byte[] nodeData = client.getData().usingWatcher(childNodeWatcher).forPath(path);
            String childrenName = path.substring(path.lastIndexOf(PATH_SEPARATOR) + 1);
            childrenNameCache.add(childrenName);
            dataChangeListener.updateData(new NodeChangedListener.
                    ChangedData(NodeChangedListener.OperationType.UPDATE, nodeData, childrenName, baseNodeName));
        } catch (Exception e) {
            logger.error("error occurs when monitor children nodes.", e);
        }
    }

    /**
     * monitor
     *
     * @param path the child path
     */
    private void monitorChildrenNodes(String path)
    {
        try {
            List<String> childrenNames = client.getChildren().
                    usingWatcher(baseNodeWatcher).forPath(basePath);
            //extracted the new set we need set watcher
            childrenNames.removeAll(childrenNameCache);
            for (String childrenName : childrenNames) {
                String childrenPath = basePath + PATH_SEPARATOR + childrenName;
                byte[] nodeData = client.getData().usingWatcher(childNodeWatcher).forPath(childrenPath);
                childrenNameCache.add(childrenName);
                dataChangeListener.updateData(new NodeChangedListener.
                        ChangedData(NodeChangedListener.OperationType.UPDATE, nodeData, childrenName, basePath));
            }

        } catch (Exception e) {
            logger.error("error occurs when monitor children nodes.", e);
        }
    }

    /**
     * starting the path monitoring
     */
    public void monitor()
    {
        if (started.get()) {
            return;
        }
        synchronized (this) {
//            BackgroundCallback backgroundCallback = new BackgroundCallback()
//            {
//                public void processResult(CuratorFramework curatorFramework, CuratorEvent curatorEvent) throws Exception
//                {
//                    if (curatorEvent.getType() == CuratorEventType.CHILDREN) {
//                        List<String> children = curatorEvent.getChildren();
//                        for (String childrenName : children) {
//                            String childrenPath = curatorEvent.getPath() + PATH_SEPARATOR + childrenName;
//                            byte[] childrenData = curatorFramework.getData().
//                                    usingWatcher(childNodeWatcher).forPath(childrenPath);
//                            childrenNameCache.add(childrenName);
//                            dataChangeListener.updateData(new NodeChangedListener.
//                                    ChangedData(NodeChangedListener.OperationType.UPDATE, childrenData, childrenName, basePath));
//
//                        }
//                    }
//                }
//            };
            try {
                List<String> children = client.getChildren().usingWatcher(baseNodeWatcher).
                        forPath(basePath);
                for (String childrenName : children) {
                    String childrenPath = basePath + PATH_SEPARATOR + childrenName;
                    byte[] childrenData = client.getData().
                            usingWatcher(childNodeWatcher).forPath(childrenPath);
                    childrenNameCache.add(childrenName);
                    dataChangeListener.updateData(new NodeChangedListener.
                            ChangedData(NodeChangedListener.OperationType.UPDATE, childrenData, childrenName, baseNodeName));

                }

            } catch (Exception e) {
                logger.error("Fail to get children of the path" +
                        String.valueOf(basePath), e);
                throw new RuntimeException(e);
            }
            started.set(true);
        }
    }

    public void shutdown()
    {
        // do something clean up
    }

}
