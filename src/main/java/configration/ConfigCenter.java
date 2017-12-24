package configration;

import org.apache.zookeeper.*;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/************************************************************************************
 * 功能描述：
 *
 * 配置管理中心：
 * 主要负责：
 * （1）：从持久化存储中（如DB、git或svn）获取原始配置
 * （2）：允许对各应用的配置进行修改，并反馈到持久化存储中
 * （3）：将各应用的配置的修改变化，同步到zookeeper中（之后客户端可获取到配置的修改，更新自己的配置）
 *
 * 创建人：岳增存  yuezc@seentao.com
 * 创建时间： 2017年12月24日 --  上午10:55 
 * 其他说明：
 * 修改时间：
 * 修改人：
 *************************************************************************************/
public class ConfigCenter{

    //模拟持久化存储，可以把该map看成是DB、git或svn

    private static Map<String, String> configMap = new HashMap<>();

    //zk连接地址
    private final static String CONNECT_ADDR = "47.93.173.81:2181";
    // 连接zk的超时时间
    private static final int SESSION_TIMEOUT = 30000;

    private static ZooKeeper zooKeeper;

    static {
        try {
            zooKeeper = new ZooKeeper(CONNECT_ADDR, SESSION_TIMEOUT, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    //do nothing for now
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 初始化配置，模拟从持久化存储中读取配置文件
     */
    public static void initProperties(){
        configMap.put("username", "laoyue");
        configMap.put("url", "10.10.10.10");
        configMap.put("password", "123456");
    }

    /**
     * 初始化节点
     * 并把初始配置同步到zookeeper中
     */
    public static void initZKNodes(){

        ZooKeeper zk = zooKeeper;
        try {
            //初始化节点
            if (zk.exists("/configcenter", true) == null) {
                zk.create("/configcenter", CONNECT_ADDR.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                System.out.println("初始化节点：configcenter");
            }

            for(String key : configMap.keySet()){
                if (zk.exists("/configcenter/" + key, true) == null) {
                    zk.delete("/configcenter/" + key, -1);
                    zk.create("/configcenter/" + key, configMap.get(key).getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                    System.out.println("初始化节点" + key + "，并设置初始数据为：" + configMap.get(key));
                }else {
                    zk.delete("/configcenter/" + key, -1);
                    zk.create("/configcenter/" + key, configMap.get(key).getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                    System.out.println("初始化节点" + key + "，并设置初始数据为：" + configMap.get(key));
                }
            }

            List<String> children = zk.getChildren("/configcenter", false);
            System.out.println("zookeeper中configcenter节点下的子节点：" + children.toString());

        }catch (InterruptedException e){
            e.printStackTrace();
        }catch (KeeperException e){
            e.printStackTrace();
        }
    }


    public static void main(String[] args){

        initProperties();

        initZKNodes();

        //保持配置中心一直运行
        while (true){

        }
    }

    /**
     * 更新配置
     *
     * @param key
     * @param value
     */
    public static void updateConfig(String key, String value){
        //更新持久化存储中的配置
        configMap.put(key, value);
        //将配置更新写入到zookeeper中
        try {
            zooKeeper.setData("/configcenter/" + key, value.getBytes(), -1);
            System.out.println("操作配置中心，更新" + key + "的值为：" + value);
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 更新配置
     *
     * @param map
     */
    public static void updateConfig(Map<String, String > map){
        if(map != null){
            for (Map.Entry<String, String > entry : map.entrySet()){
                //更新持久化存储中的配置
                configMap.put(entry.getKey(), entry.getValue());
                //将配置更新写入到zookeeper中
                try {
                    zooKeeper.setData("/configcenter/" + entry.getKey(), entry.getValue().getBytes(), -1);
                    System.out.println("操作配置中心，更新" + entry.getKey() + "的值为：" + entry.getValue());
                } catch (KeeperException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    @Test
    public void test(){
        //测试步骤：
        //①启动配置中心应用，即运行本类中的main方法
        //②启动具体应用，即执行Application中的main方法
        //执行本测试方法，观察控制台输出

        //updateConfig("url", "11.11.11.00");

        Map<String , String > map = new HashMap<>();
        //map.put("url", "11.22.66.11");
        map.put("username", "5656565");
        //map.put("password", "33333");

        updateConfig(map);
    }


}
