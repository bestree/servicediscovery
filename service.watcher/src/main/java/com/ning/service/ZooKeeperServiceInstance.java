package com.ning.service;

/**
 * wrapper the service instance
 * Created by bestree007 on 2015/9/3.
 */
public class ZooKeeperServiceInstance
{
    private ServiceInstance instance;

    public ZooKeeperServiceInstance(ServiceInstance instance)
    {
        this.instance = instance;
    }

    public String getUniqueID()
    {
        return Utils.getUniqueID(this.instance);
    }

}
