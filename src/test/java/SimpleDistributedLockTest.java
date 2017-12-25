import distributedlock.SimpleDistributedLock;
import org.junit.Test;

/************************************************************************************
 * 功能描述：
 * 创建人：岳增存  yuezc@seentao.com
 * 创建时间： 2017年12月24日 --  下午3:51 
 * 其他说明：
 * 修改时间：
 * 修改人：
 *************************************************************************************/
public class SimpleDistributedLockTest {

    @Test
    public void test(){
        final String addr = "47.93.173.81:2181";
        final String lockName = "mysqlConnection";


        for(int i = 0; i < 10; i++){
            Thread thread = new Thread(()->{
                SimpleDistributedLock lock = new SimpleDistributedLock(addr, lockName);
                lock.lock();

                System.out.println(Thread.currentThread().getName() + "开始干活");
                //模拟获取到锁之后要处理的任务
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(Thread.currentThread().getName() + "干活完毕");
                lock.unlock();
            });
            thread.setName("【线程 "+ i +"】");
            thread.start();
        }

        while (true){

        }
    }


}
