package com.dubbo.registry;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class Zookeeper_MultiOp implements Watcher {
    private ZooKeeper zk;//Zookeeper连接对象
    private CountDownLatch countDownLatch = new CountDownLatch(1);//用于阻塞建立连接后执行后续阻塞操作

    @Test
    public void transactionTest() throws Exception{
        //Transaction只是对multi的简单封装，很easy
        test();
        Transaction transaction = zk.transaction();
        transaction.create("/BBB","".getBytes(), ZooDefs.Ids.READ_ACL_UNSAFE,CreateMode.PERSISTENT);
        transaction.setData("/Optest","operation".getBytes(),0);
        transaction.delete("/Optest",1);
        transaction.commit();//事务提交，内部也是调用zk.multi
    }



    @Test
    public void opTest() throws Exception{
        test();
        // Op提拱了改变节点状态的静态方法：create、delete、setData；返回Op对象；
        //利用Zookeeper对象的multi方法，将多个操作封装到一个list中，就可以实现原子事务操作。
        try {
           List<OpResult> opResults =  zk.multi(Arrays.asList(Op.create("/Optest","".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.PERSISTENT),
                    Op.setData("/Optest","operation".getBytes(),0),
                    Op.delete("/Optest",1)  ));
           OpResult.CreateResult createResult = (OpResult.CreateResult)opResults.get(0);
           OpResult.SetDataResult setDataResult = (OpResult.SetDataResult)opResults.get(1);
           OpResult.DeleteResult deleteResult = (OpResult.DeleteResult)opResults.get(2);
            System.out.println("------------------------------------------------------------");
            System.out.println(createResult.getPath());
            System.out.println(setDataResult.getStat());
            System.out.println(deleteResult);
        }catch (Exception ke){
            System.out.println("ke = " + ke);
        }
    }






    @Test
    public void test() throws Exception {
        // Zookeeper连接的建立是异步的，借助CountDownLatch等待建立完成之后再做操作
        this.zk = new ZooKeeper("39.106.30.172:2181,39.106.30.172:2182," +
                "39.106.30.172:2183,39.106.30.172:2184,39.106.30.172:2185",
                80000, this);
        // 建立连接的第一个参数:connectString表示Zookeeper集群的所有的服务hostport,
        // 如果传递一个，Zookeeper会在超时时间内重新连接该主机，直到超时；
        // 如果传递多个，Zookeeper会随机选择一个连接，在超时时间内，若连接失败，则会重新选择一个再次连接。
        // 这里采用了最简单的负载均衡策略：随机顺序选择。
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
//        System.in.read();
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        if (watchedEvent.getState() == Event.KeeperState.Disconnected){
            System.out.println("---watchedEvent--- = " + watchedEvent);
            return;
        }
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
