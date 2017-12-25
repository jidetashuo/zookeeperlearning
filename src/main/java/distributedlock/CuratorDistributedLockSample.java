package distributedlock;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.junit.Test;

/************************************************************************************
 * 功能描述：
 *
 * 虽然zookeeper原生客户端暴露的API已经非常简洁了，
 * 但是实现一个分布式锁还是比较麻烦的
 * 所以，我们可以直接使用curator这个开源项目提供的zookeeper分布式锁实现。
 *
 * 参考链接：http://www.dengshenyu.com/java/%E5%88%86%E5%B8%83%E5%BC%8F%E7%B3%BB%E7%BB%9F/2017/10/23/zookeeper-distributed-lock.html
 *
 *
 *
 * ZooKeeper 3.5.x
 * Curator 4.0 has a hard dependency on ZooKeeper 3.5.x
 * If you are using ZooKeeper 3.5.x there's nothing additional to do - just use Curator 4.0
 * ZooKeeper 3.4.x
 * Curator 4.0 supports ZooKeeper 3.4.x ensembles in a soft-compatibility mode. To use this mode you must exclude ZooKeeper when adding Curator to your dependency management tool.
 * 详情见链接：http://curator.apache.org/zk-compatibility.html
 *
 * 创建人：岳增存  yuezc@seentao.com
 * 创建时间： 2017年12月25日 --  下午3:03 
 * 其他说明：
 * 修改时间：
 * 修改人：
 *************************************************************************************/
public class CuratorDistributedLockSample {

    @Test
    public void test(){

        String connectString = "47.93.173.81:2181";

        for(int i = 0; i < 10; i++){
            Thread thread = new Thread(()->{
                //创建zookeeper的客户端
                RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
                CuratorFramework client = CuratorFrameworkFactory.newClient(connectString, retryPolicy);
                client.start();

                //创建分布式锁, 锁空间的根节点路径为/curator/lock
                InterProcessMutex mutex = new InterProcessMutex(client, "/curator/lock");
                try {
                    mutex.acquire();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                System.out.println(Thread.currentThread().getName() + "获取到了锁，开始干活");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(Thread.currentThread().getName() + "干活完毕");

                try {
                    System.out.println(Thread.currentThread().getName() + "释放锁");
                    mutex.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                //关闭客户端
                client.close();
            });
            thread.setName("【线程 "+ i +"】");
            thread.start();
        }

        while (true){}
    }

}
