package com.my.project;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;

import static org.apache.zookeeper.Watcher.Event.KeeperState;

/**
 * Hello ZooKeeper
 * @author yang
 *
 */
public class HelloZooKeeper implements Watcher {

	private static CountDownLatch connected = new CountDownLatch(1);
	private static final String CONN = "192.168.56.10:2181,192.168.56.11:2181,192.168.56.12:2181";

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

		if (zoo.exists("/tank/master", false) != null) {
			zoo.delete("/tank/master", -1);
		}

		zoo.create("/tank/master", "192.168.56.1:2000".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
		System.out.println("ok");

		byte[] data = zoo.getData("/tank/master", false, null);
		System.out.println(new String(data));

		zoo.create("/e", "e".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
		Thread.sleep(10000);

		zoo.close();

	}

	public static void main(String[] args) throws Exception {
		new HelloZooKeeper().test();
	}

	@Override
	public void process(WatchedEvent event) {
		System.out.println("event:" + event);
		if (event.getState() == KeeperState.SyncConnected) {
			connected.countDown();
			System.out.println("connected");
		}
	}

}
