package com.ning.service;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

/**
 * Created by bestree007 on 2015/9/4.
 */
public class Utils
{
    public static String getUniqueID(ServiceInstance instance)
    {
        return instance.getName() + "#" + instance.getLocation();
    }

    public static ServiceInstance getServiceInstance(String uniqueID)
    {
        String[] values = uniqueID.split("#");
        ServiceInstance instance = null;
        if (values.length == 2) {
            instance = new ServiceInstance();
            instance.setName(values[0]);
            instance.setLocation(values[1]);
        }
        return instance;
    }

    public static ServiceInstance deserialize(byte[] bytes)
    {
        try {
            String jsonStr = new String(bytes, "UTF-8");
            JSONObject jsonObject = new JSONObject(jsonStr);
            ServiceInstance instance = new ServiceInstance();
            Object name = jsonObject.get("name");
            if (name instanceof String) {
                instance.setName((String) name);
            }
            Object location = jsonObject.get("location");
            if (location instanceof String) {
                instance.setLocation((String) location);
            }
            return instance;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static  byte[] serialize(ServiceInstance serviceInstance)
    {
        // serialized to json string
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", serviceInstance.getName());
        jsonObject.put("location", serviceInstance.getLocation());
        String json = jsonObject.toString();
        try {
            return json.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

    }

}
