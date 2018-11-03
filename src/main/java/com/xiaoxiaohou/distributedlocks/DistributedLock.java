package com.xiaoxiaohou.distributedlocks;

import com.sun.org.apache.xpath.internal.operations.Bool;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.UUID;

public class DistributedLock {
    //获得锁（带有超时时间属性）

    /**
     * @param lockName    锁的名称
     * @param acquireTime 获得锁过程的超时时间，超过这个时间就可以做其他事情避免程序在这个地方卡死，死等
     * @param lockTime    锁的超时时间，当超过该时间的时候，该锁可以自动释放。
     * @return
     */
    public String acquireLock(String lockName, long acquireTime, long lockTime) {
        String identifier = UUID.randomUUID().toString();
        String lockKey = "lock:" + lockName;
        int lockExpire = (int) lockTime / 1000;
        Jedis jedis = null;
        jedis = JedisUtils.getJedisConnection();
        //那么这个acquireTime 怎么处理呢？换句话说就是怎样让程序在这个时间段内重复向redis索要锁呢？ 首先这里我使用了while循环
        // 其次怎么判断获取超时（这个要在while循环里进行），因为指令在堆栈里的压入和弹出也是消耗时间的，虽然差别不大但是为了严谨性 我还是这样做了
        try {
            while (System.currentTimeMillis() < System.currentTimeMillis() + acquireTime) {
                //增强锁
                if (jedis.setnx(lockKey, identifier) == 1) {
                    /**
                     *  jedis.setnx(String key,String value) 这个命令我会在readme.txt文件里进行说明；
                     *  这里值等于1 说明redis里没有原来没有这个key  现在我们设置成功了 ----既我们成功的获得了锁
                     *   获得成功则返回该锁的唯一标识
                     */
                    return identifier;
                } else {
                    //TODO

                }
                /**
                 * 还有就是程序健壮性的考虑
                 * 意思就是说如果对lockKey超时时间的设置不成功在这里重新设置一下
                 *
                 */
                if (jedis.ttl(lockKey) == -1) {//ttl命令用于判断key的超时时间情况判定  也会在readme.txt中给出其详细解释
                    jedis.expire(lockKey, lockExpire);
                }
                //等待片刻后重试获取锁的过程，因为马上重试是没有意义的
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        } finally {
            jedis.close();//进行资源回收
        }
        return null;
    }
//使用lua脚本释放锁  因为lua本身是原子性的  所以更加简单
    public boolean releaseLockWithLua(String lockName, String identifier) {
        Jedis jedis = JedisUtils.getJedisConnection();
        String lua = "if redis.call(\"get\",KEYS[1])==ARGV[1] then " +
                "return redis.call(\"del\",KEYS[1]) " +
                "else return  0 end";
        boolean isrelease=false;
        Long rs =(Long) jedis.eval(lua,1,new String[]{
                lockName,identifier
        });
        if(rs.intValue()>0){
            isrelease=true;
            System.out.println(identifier + "  开始释放  " + lockName);
        }
        return isrelease;
    }

    //释放锁  使用java释放锁
    public boolean releaseLock(String lockName, String identifier) {
        System.out.println(identifier + "  开始释放  " + lockName);
        String lockKey = "lock:" + lockName;
        Jedis jedis = null;
        boolean isRelease = false;
        try {
            jedis = JedisUtils.getJedisConnection();
            //拿到连接以后我们需要做的就是进行删除，但是在删除过程当中为了保证lockName不会被修改，这是我们必须要保证的；那么手段就是用这个命令
            while (true) {
                jedis.watch(lockKey);//对于watch这个命令也会在readme.txt当中给出
                if (identifier.equals(jedis.get(lockKey))) {
                    Transaction transaction = jedis.multi();
                    transaction.del(lockKey);//基于事务的删除key操作。之所以基于事务，原因是尽最大可能确保删除了该key
                    if (transaction.exec().isEmpty()) {
                        continue;
                    }
                    isRelease = true;
                } else if (!identifier.equals(jedis.get(lockKey))) {
                    //TODO  thorw Exception   就是说到redis释放锁的线程不是加锁的线程
                }
                jedis.unwatch();
                break;
            }

        } finally {
            jedis.close();
        }
        return isRelease;
    }
}
