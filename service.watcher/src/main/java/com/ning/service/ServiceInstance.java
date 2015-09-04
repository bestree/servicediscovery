package com.ning.service;

/**
 * The data model of the service instance
 * Created by bestree007 on 2015/9/3.
 */

public class ServiceInstance
{
    /**
     * the service name
     */
    private String name;

    /**
     * the location of the instance
     */
    private String location;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getLocation()
    {
        return location;
    }

    public void setLocation(String location)
    {
        this.location = location;
    }


    @Override
    public int hashCode()
    {
        int result = 0;
        if (this.name != null) {
            result = 31 * result + this.name.hashCode();
        }
        if (this.location != null) {
            result = 31 * result + this.location.hashCode();
        }
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof ServiceInstance)) {

            return false;
        }
        ServiceInstance serviceInstance = (ServiceInstance) obj;
        return name.equals((serviceInstance).getName()) &&
                (location.equals(serviceInstance.getLocation()));
    }
}
