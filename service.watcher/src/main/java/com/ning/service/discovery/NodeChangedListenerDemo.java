package com.ning.service.discovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by bestree007 on 2015/8/28.
 */
public class NodeChangedListenerDemo implements NodeChangedListener
{
    Logger logger = LoggerFactory.getLogger(NodeChangedListenerDemo.class);

    public void updateData(ChangedData changedData)
    {
        logger.debug(changedData.toString());
    }
}
