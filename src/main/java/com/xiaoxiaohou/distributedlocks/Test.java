package com.xiaoxiaohou.distributedlocks;

public class Test extends Thread{
    public void run(){
        while(true){
            DistributedLock distributedLock = new DistributedLock();
            String rs = distributedLock.acquireLock("updateorder",2000,6000);
            if(rs!=null){//成功获得锁
                System.out.println(Thread.currentThread().getName()+"acquireLock successfully"+rs);
                try{
                    Thread.sleep(1000);
                    distributedLock.releaseLockWithLua("updateorder",rs);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        Test test = new Test();
        for(int i=0;i<10;i++){
            new Thread(test,"Tname"+i).start();
        }
    }
}
