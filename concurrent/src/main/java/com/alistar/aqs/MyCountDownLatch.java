package com.alistar.aqs;

public class MyCountDownLatch {

	private static final class Sync extends MyAbstractQueuedSynchronizer{
		Sync(int count){
			setState(count);
		}

		int getCount(){
			return getState();
		}

		/**
		 * 尝试获取锁
		 * 返回负数：表示当前线程获取失败
		 * 返回零值：表示当前线程获取成功, 但是后继线程不能再获取了
		 * 返回正数：表示当前线程获取成功, 并且后继线程同样可以获取成功
		 * @param acquires
		 * @return
		 */
		protected int tryAcquireShared(int acquires){
			return  (getState() == 0) ? 1 : -1;
		}

		protected boolean tryReleaseShared(int releases){
			for(;;){
				int c = getState();
				if(c == 0 ){
					return false;
				}
				int nextc = c - 1;
				if(compareAndSetState(c, nextc)){
					return nextc == 0;
				}
			}
		}
	}

	private final Sync sync;

	public MyCountDownLatch(int count) {
		if(count < 0){
			throw new IllegalArgumentException("count < 0");
		}
		this.sync = new Sync(count);
	}

	public void await() throws InterruptedException{
		sync.acquireSharedInterruptibly(1);
	}

	public void countDown(){
		sync.releaseShared(1);
	}

	public int getCount(){
		return sync.getCount();
	}
}

