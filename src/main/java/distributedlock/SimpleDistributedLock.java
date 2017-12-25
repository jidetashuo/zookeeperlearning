package distributedlock;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/************************************************************************************
 * 功能描述：
 *
 * ZooKeeper是Apache软件基金会的一个软件项目，他为大型分布式计算提供开源的分布式配置服务、同步服务和命名注册。
 * 在 ZooKeeper 中，节点类型可以分为持久节点（PERSISTENT ）、临时节点（EPHEMERAL），以及时序节点（SEQUENTIAL ），
 * 具体在节点创建过程中，一般是组合使用，可以生成 4 种节点类型：
 * 持久节点（PERSISTENT），
 * 持久顺序节点（PERSISTENT_SEQUENTIAL），
 * 临时节点（EPHEMERAL），
 * 临时顺序节点（EPHEMERAL_SEQUENTIAL）；
 *
 * 当很多进程需要访问共享资源时，我们可以通过zk来实现分布式锁。主要步骤是：
 * 1.建立一个节点，假如名为：lock 。节点类型为持久节点（PERSISTENT）
 * 2.每当进程需要访问共享资源时，会调用分布式锁的lock()或tryLock()方法获得锁，这个时候会在第一步创建的lock节点下建立相应的顺序子节点，节点类型为临时顺序节点（EPHEMERAL_SEQUENTIAL），通过组成特定的名字name+lock+顺序号。
 * 3.在建立子节点后，对lock下面的所有以name开头的子节点进行排序，判断刚刚建立的子节点顺序号是否是最小的节点，假如是最小节点，则获得该锁对资源进行访问。
 * 4.假如不是该节点，就获得该节点的上一顺序节点，并给该节点是否存在注册监听事件。同时在这里阻塞。等待监听事件的发生，获得锁控制权。
 * 5.当调用完共享资源后，调用unlock（）方法，关闭zk，进而可以引发监听事件，释放该锁。
 * 实现的分布式锁是严格的按照顺序访问的并发锁。
 *
 *
 *
 * 创建人：岳增存  yuezc@seentao.com
 * 创建时间： 2017年12月25日 --  上午9:31 
 * 其他说明：
 * 修改时间：
 * 修改人：
 *************************************************************************************/
public class SimpleDistributedLock implements Lock, Watcher {

    private ZooKeeper zk;

    private String root = "/distributedlock";//分布式锁的根节点（即多个分布式锁都在该节点下）
    private String lockName;//锁，即具体的某个分布式锁的名称

    private String waitNode;//当前结点的前一个结点
    private String curNode;//当前结点：即该SimpleDistributedLock实例对应的zookeeper上的结点

    /**
     * //用来等待获取锁的计数器
     */
    private CountDownLatch lockLatch;
    /**
     * 用来保证zk连接建立的计数器，
     * 因为new zookeeper()这种方式建立连接是异步的
     */
    private CountDownLatch connLatch = new CountDownLatch(1);



    private int sessionTimeout = 30000;

    private SimpleDistributedLock() {
    }

    public SimpleDistributedLock(String connAddress, String lockName) {
        this.lockName = lockName;
        // 创建zk连接
        try {
            zk = new ZooKeeper(connAddress, sessionTimeout, this);
            //等待连接完全建立
            connLatch.await();
            Stat statRoot = zk.exists(root, false);
            if (statRoot == null) {
                // 创建根节点
                zk.create(root, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    /**
     * 获取锁
     * <p>
     * 获取不到，则阻塞当前线程，直到获取到锁才返回
     */
    @Override
    public void lock() {
        try {
            if(this.tryLock()){
                return;
            } else{
                Stat stat = zk.exists(root + "/" + waitNode,true);
                if(stat != null){
                    System.out.println(Thread.currentThread().getName() + "等待前面的节点释放锁（或等待并释放锁）：" + root + "/" + waitNode);
                    this.lockLatch = new CountDownLatch(1);
                    //一直等待，直到获取锁，即前一个节点被删除
                    lockLatch.await();
                    System.out.println(Thread.currentThread().getName() + "获取了锁，对应节点为：" + curNode);
                }
            }
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void lockInterruptibly() throws InterruptedException {


    }

    /**
     * 尝试获取锁
     * <p>
     * 非阻塞
     *
     * @return 获取到锁，返回true；否则返回false
     */
    @Override
    public boolean tryLock() {
        try {
            String splitStr = "_lock_";
            if (lockName.contains(splitStr)) {
                throw new RuntimeException("lockName can not contains '_lock_'");
            }

            //创建临时子节点
            curNode = zk.create(root + "/" + lockName + splitStr, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            System.out.println(Thread.currentThread().getName() + "创建临时节点" + curNode);

            //取出所有子节点
            List<String> subNodes = zk.getChildren(root , false);

            //取出所有lockName的锁
            List<String> lockObjNodes = new ArrayList<String>();
            for (String node : subNodes) {
                String _node = node.split(splitStr)[0];
                if (_node.equals(lockName)) {
                    lockObjNodes.add(node);
                }
            }

            Collections.sort(lockObjNodes);
            if (curNode.equals(root + "/" + lockObjNodes.get(0))) {
                //如果是最小的节点，则表示取得锁
                System.out.println(Thread.currentThread().getName() + "获取了锁，对应节点为：" + curNode);
                return true;
            }

            //如果不是最小的节点，找到比自己小1的节点
            String subCurNode = curNode.substring(curNode.lastIndexOf("/") + 1);
            waitNode = lockObjNodes.get(Collections.binarySearch(lockObjNodes, subCurNode) - 1);
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;

    }

    /**
     * 尝试获取锁
     * <p>
     * 至多阻塞time这么久的时间
     *
     * @param time
     * @param unit
     * @return
     * @throws InterruptedException
     */
    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        try {
            if(this.tryLock()){
                return true;
            }
            //判断比自己小一个数的节点是否存在，如果不存在则无需等待锁，同时注册监听
            Stat stat = zk.exists(root + "/" + waitNode,true);
            if(stat != null){
                System.out.println(Thread.currentThread().getName() + "等待前面的节点释放锁（或等待并释放锁）：" + root + "/" + waitNode);
                this.lockLatch = new CountDownLatch(1);
                //等待有限时间
                if(this.lockLatch.await(time, TimeUnit.MILLISECONDS)){
                    System.out.println(Thread.currentThread().getName() + "获取了锁，对应节点为：" + curNode);
                    return true;
                }
            }else {
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 释放锁
     */
    @Override
    public void unlock() {
        try {
            System.out.println(Thread.currentThread().getName() + "释放了锁，并删除临时节点：" + curNode);
            zk.delete(curNode,-1);
            curNode = null;
            zk.close();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Condition newCondition() {
        return null;
    }

    /**
     * zk监听器
     *
     * @param event
     */
    @Override
    public void process(WatchedEvent event) {
        if(event.getState() == Event.KeeperState.SyncConnected){
            connLatch.countDown();
        }

        //节点删除，说明其他线程放弃锁
        if(event.getType().equals(Event.EventType.NodeDeleted)){
            if(this.lockLatch != null) {
                this.lockLatch.countDown();
            }
        }
    }

}
