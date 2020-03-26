package com.hesiyue.miaosha.redis;

import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Service
public class RedisService {

    @Autowired
    JedisPool jedisPool;



    public <T> T get(KeyPrefix prefix ,String key,Class<T>class1){
        Jedis jedis =  null;
        try {
          jedis = jedisPool.getResource();
          String realKey = prefix.getPrefix() + key;
          String str = jedis.get(realKey);
          T t = stringToBean(str,class1);
          return t;
        }finally {
            returnToPool(jedis);
        }
    }


    public <T> boolean set(KeyPrefix prefix,String key,T value){
        Jedis jedis =  null;
        try {
            jedis = jedisPool.getResource();
            String str = beanToString(value);
            if(str == null || str.length() <= 0){
                return false;
            }
            String realKey = prefix.getPrefix()+key;
            int seconds = prefix.expireSeconds();
            if(seconds <= 0 ){
                jedis.set(realKey,str);
            }else{
                jedis.setex(realKey,seconds,str);
            }
            return true;
        }finally {
           returnToPool(jedis);
        }
    }

    public static  <T> String beanToString(T value) {
        if(value == null){
            return  null;
        }
        Class<?> class1 = value.getClass();
        if(class1 == int.class || class1 == Integer.class){
             return ""+value;
        }else if(class1 == String.class){
            return (String)value;
        }else if(class1 == long.class || class1 == Long.class){
            return ""+value;
        }else {
            return JSON.toJSONString(value);
        }

    }

    public static  <T> T stringToBean(String str,Class<T> class1){
         if(str == null || str.length() <= 0|| class1 == null){
             return null;
         }

        if(class1 == int.class || class1 == Integer.class) {
            return (T)Integer.valueOf(str);
        }else if(class1 == String.class) {
            return (T)str;
        }else if(class1 == long.class || class1 == Long.class) {
            return  (T)Long.valueOf(str);
        }else {
            return JSON.toJavaObject(JSON.parseObject(str), class1);
        }
    }

    public   <T> boolean exists(KeyPrefix prefix,String key){
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            String realKey = prefix.getPrefix() + key;
            return jedis.exists(realKey);
        }finally {
            returnToPool(jedis);
        }
    }

    public <T> Long incr(KeyPrefix prefix,String key){
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            String realKey = prefix.getPrefix() + key;
            return jedis.incr(realKey);
        }finally {
            returnToPool(jedis);
        }
    }

    public <T> Long decr(KeyPrefix prefix,String key){
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            String realKey = prefix.getPrefix() + key;
            return  jedis.decr(realKey);
        }finally {
            returnToPool(jedis);
        }
    }

    public boolean delete(KeyPrefix prefix,String key){
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            String realKey = prefix.getPrefix()+key;
            long ret = jedis.del(realKey);
            return ret>0;
        }finally {
            returnToPool(jedis);
        }
    }

    private void returnToPool(Jedis jedis){
        if(jedis != null){
            jedis.close();
        }
    }


}
