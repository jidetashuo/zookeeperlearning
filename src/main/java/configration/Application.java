package configration;

/************************************************************************************
 * 功能描述：
 * 创建人：岳增存  yuezc@seentao.com
 * 创建时间： 2017年12月24日 --  上午10:48 
 * 其他说明：
 * 修改时间：
 * 修改人：
 *************************************************************************************/
public class Application {

    public static void main(String[] args) throws InterruptedException {

        System.out.println("程序启动，并初始化数据！");

        final MyZooClient client = new MyZooClient();

        //模拟应用中，使用配置的地方：一直从MyZooClient中获取配置
        //只要MyZooClient中配置改变，则应用也会马上开始使用新的配置
        new Thread() {
            @Override
            public void run() {
                while (true) {
                    System.out.println("应用现在使用的配置为：");
                    System.out.println("url=" + client.getUrl());
                    System.out.println("username=" + client.getUsername());
                    System.out.println("password=" + client.getPassword());
                    try {
                        Thread.sleep(10 * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
        }.start();
    }
}
