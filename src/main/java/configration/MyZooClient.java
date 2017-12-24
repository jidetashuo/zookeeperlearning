package configration;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
/************************************************************************************
 * 功能描述：
 * 创建人：岳增存  yuezc@seentao.com
 * 创建时间： 2017年12月24日 --  上午10:50 
 * 其他说明：
 * 修改时间：
 * 修改人：
 *************************************************************************************/

/**
 * 创建属于自己的Watcher，该Watcher监控 zk上的/jfconf 节点， 当该节点下的文件发生变化重新加载缓存
 * ZooKeeper通过Auth和ACL完成节点的权限控制
 * Auth表示某种认证，由于一个ZooKeeper集群可能被多个项目使用，各个项目属于不同的项目组，
 * 他们在进行开发时肯定不想其他项目访问与自己相关的节点，这时可以通过为每个项目组分配一个Auth， 然后每个项目组先通过
 * Auth认证以后再继续相关的操作，这样甲Auth认证的用户就不能操作其他
 * Auth认证后创建的节点，从而实现各个项目之间的隔离。ZooKeeper提供了如下方法完成认证
 */
public class MyZooClient implements Watcher {

    //zk连接地址
    private final static String CONNECT_ADDR = "47.93.173.81:2181";
    // 连接zk的超时时间
    private static final int SESSION_TIMEOUT = 30000;

    //client获取的数据库信息
    private String url;
    private String username;
    private String password;

    private ZooKeeper zooKeeper;

    public MyZooClient(){
        //初始化连接
        try {
            zooKeeper = new ZooKeeper(CONNECT_ADDR, SESSION_TIMEOUT, this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //设置配置信息
        setProperties();
    }


    /**
     * 从zookeeper中获取配置信息，并设置到本地
     *
     * @throws Exception
     */
    private void setProperties() {
        try {
            //获取数据，并设置观察的操作，
            //即每次获取数据时，都再次设置观察，表示：以后该节点数据发生变化，要通知我
            this.url = new String(zooKeeper.getData("/configcenter/url", true, null));
            this.username = new String(zooKeeper.getData("/configcenter/username", true, null));
            this.password = new String(zooKeeper.getData("/configcenter/password", true, null));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 如果zookeeper中配置信息发生变化，则会通知process方法，
     *
     * 客户端则重新获取配置信息
     */
    @Override
    public void process(WatchedEvent event) {
        System.out.println("ZK事件回调");
        Event.EventType type = event.getType();
        if (type.equals(Watcher.Event.EventType.None)) {
            System.out.println("connect zk sucess!!!");
            setProperties();
        } else if (type.equals(Event.EventType.NodeDataChanged)) {
            System.out.println("znode update success!!!,reload configration information");
            //重新设置属性
            setProperties();
        }
        System.out.println("获取url配置，值：" + getUrl());
        System.out.println("获取username配置，值：" + getUsername());
        System.out.println("获取password配置，值：" + getPassword());
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}

