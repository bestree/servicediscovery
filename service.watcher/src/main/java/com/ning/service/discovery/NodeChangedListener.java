package com.ning.service.discovery;

import java.util.Arrays;

/**
 * Created by bestree007 on 2015/8/28.
 */
public interface NodeChangedListener
{
    enum OperationType
    {
        DELETE,
        UPDATE
    }

    /**
     * The changedData used to transfer the data change.
     */
    class ChangedData
    {

        private OperationType type;
        private byte[] data;
        private String nodeName;

        public ChangedData(OperationType type, byte[] data, String nodeName)
        {
            this.type = type;
            this.data = data;
        }

        public byte[] getData()
        {
            return data;
        }

        public OperationType getType()
        {
            return type;
        }

        public String getNodeName()
        {
            return nodeName;
        }

        @Override
        public String toString()
        {
            return "ChangedData{" +
                    "type=" + type +
                    ", data=" + Arrays.toString(data) +
                    ", nodeName='" + nodeName + '\'' +
                    '}';
        }
    }

    // the callback method, which will be called the path monitor
    void updateData(ChangedData changedData);
}