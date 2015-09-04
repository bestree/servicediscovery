package com.ning.service;

import java.util.List;

/**
 * Created by bestree007 on 2015/9/3.
 */
public interface ServiceRegistry
{
    boolean registerServiceInstance(ServiceInstance instance);

    List<ServiceInstance> findInstanceList(String serviceName);
}
