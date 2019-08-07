package com.alistar.aqs;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class MyReentrantLock implements Lock {

	private final Sync sync;

	public MyReentrantLock() {
		sync = new NonfairSync();
	}

	public MyReentrantLock(boolean fair) {
		sync = fair ? new FariSync() : new NonfairSync();
	}

	abstract static class Sync extends MyAbstractQueuedSynchronizer{
		abstract void lock();

		final boolean nonfairTryAcquire(int acquires){
			final Thread current = Thread.currentThread();
			int c = getState();
			if(c == 0){
				if(compareAndSetState(0, acquires)){
					setExclusiveOwnerThread(current);
					return true;
				}
			}else if(current == getExclusiveOwnerThread()){
				int nextc = c + acquires;
				//overflow
				if(c < 0){
					throw new Error("Maximum lock count exceeded");
				}
				setState(nextc);
				return true;
			}
			return false;
		}

		@Override
		protected final boolean tryRelease(int releases){
			int c = getState() - releases;
			if(Thread.currentThread() != getExclusiveOwnerThread()){
				throw new IllegalMonitorStateException();
			}
			boolean free = false;
			if(c == 0){
				free = true;
				setExclusiveOwnerThread(null);
			}
			setState(c);
			return free;
		}

		protected final boolean isHeldExclusively(){
			return getExclusiveOwnerThread() == Thread.currentThread();
		}

		final ConditionObject newCondition(){
			return new ConditionObject();
		}

		final Thread getOwner(){
			return getState() == 0 ? null : getExclusiveOwnerThread();
		}

		final int getHoldCount(){
			return isHeldExclusively() ? getState() : 0;
		}

		final boolean isLocked(){
			return  getState() != 0;
		}
	}

	//实现非公平锁的同步器
	static final class NonfairSync extends Sync{

		@Override
		final void lock() {
			if(compareAndSetState(0, 1)){
				setExclusiveOwnerThread(Thread.currentThread());
			}else {
				acquire(1);
			}
		}

		protected final boolean tryAcquire(int acquires){
			return nonfairTryAcquire(acquires);
		}
	}

	//实现公平锁的同步器
	static final class FariSync extends Sync{

		@Override
		final void lock() {
			acquire(1);
		}

		protected final boolean tryAcquire(int acquires){
			final Thread current = Thread.currentThread();
			int c = getState();
			if(c == 0){
				//判断同步队列是否有前继结点
				if(!hasQueuedPredecessors() && compareAndSetState(0, acquires)){
					//如果没有前继结点且设置同步状态成功就表示获取锁成功
					setExclusiveOwnerThread(current);
					return true;
				}
			} else if (current == getExclusiveOwnerThread()) {
				int nextc = c + acquires;
				if (nextc < 0){
					throw new Error("Maximum lock count exceeded");
				}
				setState(nextc);
				return true;
			}
			return false;
		}
	}

	@Override
	public void lock() {
		sync.lock();
	}

	@Override
	public void lockInterruptibly() throws InterruptedException {
		sync.acquireInterruptibly(1);
	}

	@Override
	public boolean tryLock() {
		return sync.nonfairTryAcquire(1);
	}

	@Override
	public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
		return false;
	}

	@Override
	public void unlock() {
		sync.release(1);
	}

	@Override
	public Condition newCondition() {
		return sync.newCondition();
	}



}
