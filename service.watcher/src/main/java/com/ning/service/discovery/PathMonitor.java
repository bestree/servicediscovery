package com.ning.service.discovery;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.BackgroundCallback;
import org.apache.curator.framework.api.CuratorEvent;
import org.apache.curator.framework.api.CuratorEventType;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


/**
 * Created by bestree007 on 2015/8/28.
 */
public class PathMonitor
{
    // the logger
    Logger logger = LoggerFactory.getLogger(PathMonitor.class);

    // the base path we will watch.
    private String basePath;

    // the client
    private CuratorFramework client;

    // the listener which listen the connection;

    private ConnectionStateListener listener;

    // the data change listener

    private NodeChangedListener dataChangeListener;

    // path separator

    private char PATH_SEPARATOR = '/';

    public PathMonitor(String basePath, CuratorFramework client, NodeChangedListener listener)
    {
        this.basePath = basePath;
        this.client = client;
        this.dataChangeListener = listener;
    }

    public String getBasePath()
    {
        return basePath;
    }

    public void setBasePath(String basePath)
    {
        this.basePath = basePath;
    }


    public NodeChangedListener getDataChangeListener()
    {
        return dataChangeListener;
    }

    public void setDataChangeListener(NodeChangedListener dataChangeListener)
    {
        this.dataChangeListener = dataChangeListener;
    }

    // the watcher watch the base node
    private Watcher baseNodeWatcher = new Watcher()
    {
        public void process(WatchedEvent watchedEvent)
        {
            if (watchedEvent.getType() == Event.EventType.NodeChildrenChanged) {
                // It means that the children node added of deleted
                monitorChildrenNodes(watchedEvent.getPath());
            } else if (watchedEvent.getType() == Event.EventType.NodeCreated) {
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
                String childrenName = path.substring(path.lastIndexOf(PATH_SEPARATOR));
                dataChangeListener.updateData(new NodeChangedListener.
                        ChangedData(NodeChangedListener.OperationType.DELETE, null, childrenName));
            } else if (watchedEvent.getType() == Event.EventType.NodeDataChanged) {
                monitorChildrenNodes(watchedEvent.getPath());
            }
        }
    };

    /**
     * @param path the child path
     */
    private void monitorChildrenNodes(String path)
    {
        try {
            client.getChildren().usingWatcher(baseNodeWatcher).forPath(basePath);
            byte[] nodeData = client.getData().usingWatcher(childNodeWatcher).forPath(path);
            String childrenName = path.substring(path.lastIndexOf(PATH_SEPARATOR));
            dataChangeListener.updateData(new NodeChangedListener.
                    ChangedData(NodeChangedListener.OperationType.UPDATE, nodeData, childrenName));
        } catch (Exception e) {
            logger.error("error occurs when monitor children nodes.", e);
        }
    }

    public void monitor()
    {
        BackgroundCallback backgroundCallback = new BackgroundCallback()
        {
            public void processResult(CuratorFramework curatorFramework, CuratorEvent curatorEvent) throws Exception
            {
                if (curatorEvent.getType() == CuratorEventType.CHILDREN) {
                    List<String> children = curatorEvent.getChildren();
                    for (String childrenName : children) {
                        String childrenPath = curatorEvent.getPath() + "/" + childrenName;
                        byte[] childrenData = curatorFramework.getData().
                                usingWatcher(childNodeWatcher).forPath(childrenPath);
                        dataChangeListener.updateData(new NodeChangedListener.
                                ChangedData(NodeChangedListener.OperationType.UPDATE, childrenData, childrenName));

                    }
                }
            }
        };
        try {
            client.getChildren().usingWatcher(baseNodeWatcher).
                    inBackground(backgroundCallback).forPath(basePath);

        } catch (Exception e) {
            logger.error("Fail to get children of the path" +
                    String.valueOf(basePath), e);
        }
    }

    public void shutdown()
    {
        // do something clean up
    }

}
