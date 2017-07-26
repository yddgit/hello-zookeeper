package com.my.project;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;

/**
 * 使用ZooKeeper实现双机热备的示例
 * @author yang
 *
 */
public class HAExample implements Watcher {

	/** 活动状态 */
	private static final String STATUS_ACTIVE = "ACTIVE";
	/** 等待状态 */
	private static final String STATUS_STANDBY = "STANDBY";
	/** Master的znode结点路径 */
	private static final String MASTER_NODE = "/tank/master";
	/** zookeeper连接信息 */
	private static final String CONN = "192.168.56.10:2181,192.168.56.11:2181,192.168.56.12:2181";
	/** 当前服务器状态 */
	private static String status = STATUS_ACTIVE;
	private static CountDownLatch connected = new CountDownLatch(1);

	public void test() throws Exception {

		ZooKeeper zoo = null;

		try {
			zoo = new ZooKeeper(CONN, 5000, this);
			System.out.println("zookeeper state: " + zoo.getState());
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			connected.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		String hostInfo = "192.168.56.1:" + new Random().nextInt(500);
		System.out.println(hostInfo);

		if (zoo.exists(MASTER_NODE, new MasterWatcher(zoo, hostInfo)) != null) {
			status = STATUS_STANDBY; //节点已经被创建
		} else {
			zoo.create(MASTER_NODE, hostInfo.getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
			status = STATUS_ACTIVE;
			//启动server
			System.out.println("启动就是active");
		}
		System.out.println("server status: " + status);

		Thread.sleep(Integer.MAX_VALUE);

	}

	public static void main(String[] args) throws Exception {
		new HAExample().test();
	}

	static class MasterWatcher implements Watcher {

		private String hostInfo;
		private ZooKeeper zoo;

		public MasterWatcher(ZooKeeper zoo, String hostInfo) {
			this.hostInfo = hostInfo;
			this.zoo = zoo;
		}

		@Override
		public void process(WatchedEvent event) {
			if(event.getType() == EventType.NodeDeleted) {
				try {
					zoo.create(MASTER_NODE, hostInfo.getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
					status = STATUS_ACTIVE;
					System.out.println("切换为active");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void process(WatchedEvent event) {
		if (event.getState() == KeeperState.SyncConnected) {
			connected.countDown();
			System.out.println("connected");
		}
	}

}
