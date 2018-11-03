目录说明：
在哪里配置redis连接信息？
JedisUtils.java
在哪里做锁的获取和释放？
DistributedLock.java  这里有简单的获取锁（acquireLock）和释放锁(releaseLock)，以及使用lua释放锁(releaseLockWithLua),其实可以用lua获取锁的
在哪里测试代码？
Test.java



以下是DistributedLock.java 中用到的几个方法的解释（建议先看一下代码  再来看这个文件）

1、在第29行  有一个 if (jedis.setnx(lockKey, identifier) == 1)；这里的setnx（）方法：
setnx  是SET IF NOT EXISTS 的缩写，特性是：只有在redis中不存在该key的时候在进行添加。
这里就是利用了这一特性来实现的，对比set（）方法，set在第二次用同样的key保存数据的时候会对原有的数据进行覆盖（更新）。
这是锁机制中不希望看到的
2、在第45行   有一个 if (jedis.ttl(lockKey) == -1)；这里的ttl（）方法，
这个方法返回该key的剩余时间（就是说如果对该key设置了过期时间，调用这个方法会得到该key还有多长时间会被redis从内存中删除），单位是秒。
那么返回-1是怎么回事呢？这里有一个redis版本的区分
在redis2.6 或者更老的版本中--------如果没有对这个key设置过期时间，或者key不存在，则返回-1；
在redis2.8 或者更新的版本中--------如果没有设置过期时间返回-1，如果key不存在，则返回-2。
3、在第89行开始的if块中watch 和 事务 配合使用
首先：watch的作用是，当对某一个key进行watch以后如果有其他线程来修改这个key，那么它是修改不成功的，这是对数据安全性做的努力。
其次：配合事务（Transaction）是为了保证原子性，因为我的代码仅仅专注于这一个key。