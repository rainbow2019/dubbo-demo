package com.dubbo.registry;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

public class ZookeeperDemo implements Watcher {
    private ZooKeeper zk;//Zookeeper连接对象
    private CountDownLatch countDownLatch = new CountDownLatch(1);//用于阻塞建立连接后执行后续阻塞操作

    @Test
    public void test() throws Exception {
        // Zookeeper连接的建立是异步的，借助CountDownLatch等待建立完成之后再做操作
        this.zk = new ZooKeeper("39.106.30.172:2181,39.106.30.172:2182," +
                "39.106.30.172:2183,39.106.30.172:2184,39.106.30.172:2185",
                80000, this);
        System.out.println("等待Zookeeper建立连接......");
        countDownLatch.await();
        System.out.println("Zookeeper建立连接成功!!!!!!");
        System.out.println("zk = " + zk);
        Stat stat = zk.exists("/AAA",true);
        System.out.println("stat = " + stat);
        if(stat == null){
            zk.create("/AAA","raw".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }else{
            //获取data
            String res = new String(zk.getData("/AAA", this, new Stat()));
            System.out.println(res);
            //修改data
            zk.setData("/AAA", "88888".getBytes(), stat.getVersion());
        }
        System.in.read();
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        if (watchedEvent.getState() == Event.KeeperState.SyncConnected) {//保持着连接
            System.out.println("watchedEvent = " + watchedEvent);
        }else{
            return;//连接已经断开，直接退出
        }
        Event.EventType eventType = watchedEvent.getType();
        String watchPath = watchedEvent.getPath();
        try {
            /**
             * 重新设置watcher
             *  利用exists方法设置的watcher：当该节点被创建、被删除或数据更新时触发；
             *  利用getData方法设置的watcher：当该节点被删除或数据更新时触发；
             *  利用getChildren方法设置的watcher：当该节点自身被删除、或有子节点被创建或删除时触发。
             *  (可以看出：exists设置的watcher触发情况比getData多一个被创建，因此需要exists设置watcher就不需要getData再次设置了；
             *  getData之所以不能在节点创建时触发，是因为getData必需要求节点是已经存在的，否则操作失败)
             *  (如果要监视子节点的创建或删除必需通过getChildren方法设置监视器）
             */
            switch (eventType) {
                case None:
                    //连接服务器成功时，发回来一个eventType为None的WatchedEvent
                    System.out.println("连接成功，计数锁存器减一");
                    countDownLatch.countDown();//表示连接上
                    break;
                case NodeCreated:
                    System.out.println("事件监听:节点被创建NodeCreated");
                    this.zk.exists(watchPath, this);
                    this.zk.getChildren(watchPath, this);
                    break;
                case NodeDataChanged:
                    String newData = new String(this.zk.getData(watchPath, this, new Stat()));//重新设置监听
                    System.out.println("事件监听路径:" + watchPath + ";监听事件:节点数据被更改NodeDataChanged,新的数据："
                            + newData);
                    this.zk.getChildren(watchPath, this);
                    break;
                case NodeDeleted:
                    System.out.println("事件监听:节点被删除NodeDeleted");
                    this.zk.exists(watchPath, this);
                    this.zk.getChildren(watchPath, this);
                    break;
                case NodeChildrenChanged:
                    System.out.println("事件监听:子节点被改变NodeChildrenChanged");
                    this.zk.exists(watchPath, this);
                    this.zk.getChildren(watchPath, this);
                    break;
                default:
                    System.out.println("事件监听:其他事件发生，" + eventType);
            }
        } catch (Exception e) {
            System.out.println("再次设置监听失败!!!!!!,异常信息：" + e);
        }
    }
}