### Zookeeper介绍
Zookeeper是一个开放源码的分布式服务协调组件，是Google Chubby的开源实现。
是一个高性能的分布式数据一致性解决方案。
他将那些复杂的、容易出错的分布式一致性服务封装起来，构成一个高效可靠的原语集，
并提供一系列简单易用的接口给用户使用。

它解决了分布式数据一致性问题，提供了顺序一致性、原子性、单一视图、可靠性、实时性等。
>顺序一致性：客户端的更新顺序与他们被发送的顺序相一致；
原子性：更新操作要么全部成功，要么全部失败；
单一试图：无论客户端连接到哪一个服务器，都可以看到相同的ZooKeeper视图；
可靠性：一旦一个更新操作被应用，那么在客户端再次更新它之前，其值将不会被改变；
实时性：在特定的一段时间内，系统的任何变更都将被客户端检测到；

工作过程：
在整个集群刚刚启动的时候，会进行Leader选举，当Leader确定之后，其他机器自动成为Follower，
并和Leader建立长连接，用于数据同步和请求转发等。
当有客户端机器的写请求落到follower机器上的时候，
follower机器会把请求转发给Leader，由Leader处理该请求，比如数据的写操作，
在请求处理完之后再把数据同步给所有的follower。


CAP理论的核心观点是任何软件系统都无法同时满足一致性、可用性以及分区容错性。
ZooKeeper是个CP（一致性+分区容错性）的，
即任何时刻对ZooKeeper的访问请求能得到一致的数据结果，同时系统对网络分割具备容错性;
但是它不能保证每次服务请求的可用性(注：也就是在极端环境下，ZooKeeper可能会丢弃一些请求，消费者程序需要重新请求才能获得结果)。
但是别忘了，ZooKeeper是分布式协调服务，
它的职责是保证数据(如配置数据，状态数据)在其管辖下的所有服务之间保持同步、一致;
所以就不难理解为什么ZooKeeper被设计成CP而不是AP特性的了，
如果是AP的，那么将会带来恐怖的后果
而且， 作为ZooKeeper的核心实现算法 Zab，就是解决了分布式系统下数据如何在多个服务之间保持同步问题的。




### 安装单机版zookeeper

```
# 下载：
cd /usr/local
mkdir zookeeper
wget http://mirrors.hust.edu.cn/apache/zookeeper/zookeeper-3.3.6/zookeeper-3.3.6.tar.gz
tar xzf zookeeper-3.3.6.tar.gz

# 设置环境变量
export ZOOKEEPER_HOME=/usr/local/zookeeper/zookeeper-3.3.6
export PATH=$PATH:$ZOOKEEPER_HOME/bin

cd /etc
mkdir zookeeper
cd zookeeper
vim zoo.cfg
export ZOOCFGDIR=/etc/zookeeper

# 运行并测试
[root@iZ2zeap997asuc4yr0bw77Z zookeeper]# zkServer.sh start
JMX enabled by default
Using config: /etc/zookeeper/zoo.cfg
Starting zookeeper ... STARTED

[root@iZ2zeap997asuc4yr0bw77Z zookeeper]# echo ruok | nc localhost 2181
imok

```


---
### zookeeper重要概念

该部分内容主要参考：[Zookeeper介绍（四）——Zookeeper中的基本概念](http://www.hollischuang.com/archives/1280)

###### 数据模型

ZK中数据是以目录结构的形式存储的。
其中的每一个存储数据的节点都叫做Znode，每个Znode都有一个唯一的路径标识。
和目录结构类似，每一个节点都可以可有子节点（临时节点除外）。
节点中可以存储数据和状态信息，每个Znode上可以配置监视器（watcher），用于监听节点中的数据变化。
节点不支持部分读写，而是一次性完整读写（即原子读写）。


###### 节点类型
Znode有四种类型
PERSISTENT（持久节点）
PERSISTENT_SEQUENTIAL（持久的连续节点）、
EPHEMERAL（临时节点）、
EPHEMERAL_SEQUENTIAL（临时的连续节点）

Znode的类型在创建时确定并且之后不能再修改

* 临时节点
临时节点的生命周期和客户端会话绑定。也就是说，如果客户端会话失效，那么这个节点就会自动被清除掉。
临时节点不能有子节点

* 持久节点
在节点创建后，就一直存在，直到有删除操作来主动清除这个节点——不会因为创建该节点的客户端会话失效而消失。

* 临时顺序节点
临时节点的生命周期和客户端会话绑定。也就是说，如果客户端会话失效，那么这个节点就会自动被清除掉。
注意创建的节点会自动加上编号。

* 持久顺序节点
这类节点的基本特性和持久节点类型是一致的。
额外的特性是，在ZooKeeper中，每个父节点会为他的第一级子节点维护一份时序，会记录每个子节点创建的先后顺序。
基于这个特性，在创建子节点的时候，可以设置这个属性，那么在创建节点过程中，ZooKeeper会自动为给定节点名加上一个数字后缀，作为新的节点名。
这个数字后缀的范围是整型的最大值。
```
createdPath = /computer
createdPath = /computer/node0000000000
createdPath = /computer/node0000000001
createdPath = /computer/node0000000002
createdPath = /computer/node0000000003
createdPath = /computer/node0000000004
结果中的0000000000~0000000004都是自动添加的序列号
```

###### 角色

领导者（leader）
负责进行投票的发起和决议，更新系统状态。为客户端提供读和写服务。

跟随者（follower）
用于接受客户端请求并想客户端返回结果，在选主过程中参与投票。为客户端提供读服务。

观察者（observer）
可以接受客户端连接，将写请求转发给leader，但observer不参加投票过程，只同步leader的状态，observer的目的是为了扩展系统，提高读取速度

客户端（client）
请求发起方

要注意的是，领导者、跟随者、观察者都是zookeeper集群中的概念，不是客户端集群的概念，注意区分



###### 观察（watcher）
Watcher 在 ZooKeeper 是一个核心功能，
Watcher 可以监控目录节点的数据变化以及子目录的变化，
一旦这些状态发生变化，服务器就会通知所有设置在这个目录节点上的 Watcher，
从而每个客户端都很快知道它所关注的目录节点的状态发生变化，而做出相应的反应

可以设置观察的操作：exists,getChildren,getData
可以触发观察的操作：create,delete,setData

znode以某种方式发生变化时，“观察”（watch）机制可以让客户端得到通知。
可以针对ZooKeeper服务的操作来设置观察，该服务的其他操作可以触发观察。
比如，客户端可以对某个节点调用exists操作，
同时在它上面设置一个观察，如果此时这个znode不存在，则exists返回 false，
如果一段时间之后，这个znode被其他客户端创建，则这个观察会被触发，
之前的那个客户端就会得到通知。

要注意的是：
zookeeper中的观察是一次性触发器，即client在一个节点上设置watch，随后节点内容改变，client将获取事件。
但当节点内容再次改变，client不会获取这个事件，除非它又执行了一次读操作并设置watch

