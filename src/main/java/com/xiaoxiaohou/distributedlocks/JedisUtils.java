package com.xiaoxiaohou.distributedlocks;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class JedisUtils {
    public static JedisPool pool = null;
    static{
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxIdle(100);
        //new JedisPool(JedisPoolConfig config,String host,int port, int timeout,String password)
        pool = new JedisPool();
    }
    public static Jedis getJedisConnection(){
        return pool.getResource();
    }
}
