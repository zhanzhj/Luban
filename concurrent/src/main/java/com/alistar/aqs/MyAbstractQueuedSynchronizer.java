package com.alistar.aqs;

import sun.misc.Unsafe;

import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractOwnableSynchronizer;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.LockSupport;

/**
 * 提供一个框架，用于实现依赖先进先出(FIFO)等待队列的阻塞锁和相关同步器
 *
 * 可以把同步队列和条件队列看成排队区，每个节点看成是排队区的座位，将线程看成是排队的客人。
 * 客人刚进来时会先敲敲门，看看锁有没有开，如果锁没开它就会去排队区领取一个号码牌，
 * 声明自己想要一件什么样的方式来持有锁，最后在到队列的末尾进行排队。
 *
 */
public abstract class MyAbstractQueuedSynchronizer
		extends AbstractOwnableSynchronizer
		implements Serializable {

	public MyAbstractQueuedSynchronizer() {
	}

	/**
	 * 同步队列的节点
	 */
	static final class Node{

		/**
		 * 表示当前线程以共享模式持有锁
		 */
		static final Node SHARED = new Node();
		/**
		 * 表示当前线程以独占模式持有锁
		 */
		static final Node EXCLUSIVE = null;

		/**
		 * 表示当前节点已经取消获取锁
		 */
		static final int CANCELLED = 1;

		/**
		 * 表示后继节点的线程需要运行
		 */
		static final int SIGNAL = -1;

		/**
		 * 表示当前节点在条件队列中排队
		 */
		static final int CONDITION = -2;

		/**
		 * 表示后继节点可以直接获取锁
		 */
		static final int PROPAGATE = -3;

		/**
		 * 当前节点的等待状态
		 */
		volatile int waitStatus;

		/**
		 * 表示同步队列的前继节点
		 */
		volatile Node prev;

		/**
		 * 同步队列的后继节点
		 */
		volatile Node next;

		/**
		 * 当前节点持有的线程引用
		 */
		volatile Thread thread;

		/**
		 * 表示条件队列中的后继节点
		 */
		Node nextWaiter;

		/**
		 * 当前节点是否是共享模式
		 */
		final boolean isShared(){
			return nextWaiter == SHARED;
		}

		/**
		 * 返回当前节点的前继节点
		 * @return
		 * @throws NullPointerException
		 */
		final Node predecessor() throws NullPointerException{
			Node p = prev;
			if(p == null){
				throw new NullPointerException();
			}else {
				return p;
			}
		}

		Node(){}

		/**
		 * Used by addWaiter
		 * @param thread
		 * @param mode
		 */
		Node(Thread thread, Node mode){
			this.nextWaiter = mode;
			this.thread = thread;
		}

		/**
		 * Used by Condition 只在条件队列中使用
		 * @param thread
		 * @param waitStatus
		 */
		Node(Thread thread, int waitStatus){
			this.thread = thread;
			this.waitStatus = waitStatus;
		}
	}

	/**
	 * 同步队列的头结点
	 */
	private transient volatile Node head;
	/**
	 * 同步队列的尾节点
	 */
	private transient volatile Node tail;

	/**
	 * 同步状态
	 */
	private volatile int state;

	/**
	 * 获取同步状态
	 * @return
	 */
	protected final int getState(){
		return state;
	}

	/**
	 * 设置同步状态
	 * @param newState
	 */
	protected final void setState(int newState){
		state = newState;
	}

	/**
	 * 以CAS方式设置同步状态
	 * @param expect
	 * @param update
	 * @return
	 */
	protected final boolean compareAndSetState(int expect, int update){
		return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
	}

	static final long spinForTimeoutThreshold = 1000L;

	/**
	 * 节点入队操作，返回前一个节点
	 * 添加尾节点的顺序分为三步： 指向尾节点，CAS更改尾节点，将旧的尾节点的后继指向当前节点
	 * 在并发环境张这三步操作不一定能保证完成，所以在清空同步队列所有已取消的节点这一操作中，
	 * 为了寻找非取消状态的节点，不是从前向后遍历而是从后向前遍历的。
	 * 还有就是每个节点进入队列中时他的等待状态是0，只有后继节点的线程需要挂起时才会将前面节点的等待状态修改为SIGNAL
	 * @param node
	 * @return
	 */
	private Node enq(final Node node){
		for(;;){
			//获取同步队列的尾节点引用
			Node t = tail;
			//如果尾节点为空，说明同步队列还没有初始化
			if(t == null){
				//初始化同步队列
				if(compareAndSetHead(new Node())){
					tail = head;
				}
			}else {
				//1. 指向当前尾节点
				node.prev = t;
				//2. 设置当前节点为尾节点
				if(compareAndSetTail(t, node)){
					//3. 将旧的尾节点的后继指向新的尾节点
					t.next = node;
					//for循环的唯一出口
					return t;
				}
			}
		}
	}

	protected  boolean compareAndSetTail(Node expect, Node update){
		return unsafe.compareAndSwapObject(this, tailOffset, expect, update);
	}

	protected  boolean compareAndSetHead(Node update){
		return unsafe.compareAndSwapObject(this, headOffset, null, update);
	}

	private static final boolean compareAndSetWaitStatus(Node node, int expect, int update) {
		return unsafe.compareAndSwapInt(node, waitStatusOffset, expect, update);
	}

	private void setHead(Node node){
		head = node;
		node.thread = null;
		node.prev = null;
	}




	/**
	 * 以独占模式获取，忽略中断。通过调用至少一次tryAcquire(int)实现，成功返回。
	 * 否则线程排队，可能会重复阻塞和解除阻塞，直到成功才调用tryAcquire(int).
	 * 该方法用于实现方法Lock.lock()
	 * @param arg
	 */
	public final void acquire(int arg){
		if(!tryAcquire(arg) && acquireQueued(addWaiter(Node.EXCLUSIVE), arg)){
			selfInterrupt();
		}
	}

	/**
	 * 尝试以独占模式获取。该方法应该查询对象的状态是否允许以独占模式获取，如果是，则获取它。
	 * 该方法总是由执行获取的线程调用。如果此方法报告失败，则获取方法可能将线程排队(如果尚未排队)，
	 * 直到被其他线程释放为止。这可以用于实现方法Lock.tryLock()
	 * @param arg
	 * @return
	 */
	private boolean tryAcquire(int arg) {
		throw new UnsupportedOperationException();
	}

	/**
	 * 将当前线程包装成节点并添加到同步队列尾部
	 * @param mode Node.EXCLUSIVE for exclusive, Node.SHARED for shared
	 * @return
	 */
	private Node addWaiter(Node mode){
		//指定持有锁的模式
		Node node = new Node(Thread.currentThread(), mode);
		//获取同步队列的尾节点引用
		Node pred = tail;
		//尾节点不为空，表明同步队列已存在节点
		if(pred != null){
			//1.指向当前尾节点
			node.prev = pred;
			//2.设置当前节点为尾节点
			if(compareAndSetTail(pred, node)){
				//3.将旧的尾节点的后继指向新的尾节点
				pred.next = node;
				return node;
			}
		}
		//表明同步队列还没有进行初始化
		enq(node);
		return node;
	}

	private void selfInterrupt() {
		Thread.currentThread().interrupt();
	}

	/**
	 *以不可中断方式获取锁
	 */
	private boolean acquireQueued(Node node, int arg) {
		boolean failed = true;
		try {
			boolean interrupted = false;
			for (;;){
				//获取给定节点的前继节点的引用
				final Node p = node.predecessor();
				//如果当前节点是同步队列的第一个节点，就尝试去获取锁
				if(p == head && tryAcquire(arg)){
					//将给定的节点设置为head节点
					setHead(node);
					//help GC
					p.next = null;
					//设置获取成功状态
					failed = false;
					//返回中断的状态，整个程序的出口
					return interrupted;
				}
				//否则说明锁的状态还是不可获取，这时判断是否可以挂起当前线程
				//如果判断结果为真则挂起当前线程，否则继续循环，
				//在这期间线程不响应中断
				if(shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt()){
					interrupted = true;
				}
			}
		}finally {
			//在最后确保如果获取失败就取消获取
			if(failed){
				cancelAcquire(node);
			}
		}
	}

	/**
	 * 判断是否可以将当前节点挂起
	 * @param pred
	 * @param node
	 * @return
	 */
	private boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
		//获取前继节点的等待状态
		int ws = pred.waitStatus;
		//如果前继节点状态为SIGNAL，表明前继节点会唤醒当前节点，
		//所以当前节点可以安心的挂起了
		if(ws == Node.SIGNAL){
			return true;
		}
		if(ws > 0){
			//下面的操作是清理同步队列中所有已取消的前继节点
			do{
				node.prev = pred = pred.prev;
			}while (pred.waitStatus > 0);
			pred.next = node;
		}else {
			//到这里表示前继节点状态不是SIGNAL，很可能还是等于0，
			//这样的话前继节点就不会去唤醒当前节点
			//所以当前节点必须确保前继节点的状态未SIGNAL才能安心的挂起自己
			compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
		}
		return false;
	}

	/**
	 * 挂起当前线程
	 * @return
	 */
	private boolean parkAndCheckInterrupt() {
		LockSupport.park(this);
		return Thread.interrupted();
	}

	private void cancelAcquire(Node node) {
		if(node == null){
			return;
		}
		node.thread = null;
	}

	/**
	 * 以可中断模式获取锁(独占模式)
	 */
	private void doAcquireInterruptibly(int arg)
		throws InterruptedException{
		final Node node = addWaiter(Node.EXCLUSIVE);
		boolean fialed = true;
		try {
			for(;;){
				//获取当前节点的前继节点
				final Node p = node.predecessor();
				//如果p是head节点，那么当前线程就再次尝试获取锁
				if(p == head && tryAcquire(arg)){
					setHead(node);
					//help GC
					p.next = null;
					fialed = false;
					//获取锁成功返回
					return;
				}
				//如果满足条件就挂起当前线程，此时响应中断并抛出异常
				if(shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt()){
					//线程被唤醒后如果发现中断请求就抛出异常
					throw new  InterruptedException();
				}
			}
		}finally {
			if(fialed){
				cancelAcquire(node);
			}
		}
	}

	public final boolean tryAcquireNanos(int arg, long nanosTimeout)
			throws InterruptedException {
		if (Thread.interrupted())
			throw new InterruptedException();
		return tryAcquire(arg) ||
				doAcquireNanos(arg, nanosTimeout);
	}

	/**
	 * 以限定超时时间获取锁
	 * @param arg
	 * @param nanosTimeOut
	 * @return
	 */
	private boolean doAcquireNanos(int arg, long nanosTimeOut)
			throws InterruptedException{
		if(nanosTimeOut <= 0L){
			return false;
		}
		final long deadline = System.nanoTime() + nanosTimeOut;
		final Node node = addWaiter(Node.EXCLUSIVE);
		boolean failed = true;
		try {
			for(;;){
				final Node p = node.predecessor();
				if(p == head && tryAcquire(arg)){
					//更新头结点
					setHead(node);
					p.next = null;
					failed = false;
					return true;
				}
				nanosTimeOut = deadline - System.nanoTime();
				//超时时间用完了就直接退出循环
				if(nanosTimeOut <= 0L){
					return false;
				}
				//如果超时时间大于自旋时间，那么等判断可以挂起线程之后
				//就会将线程挂起一段时间
				if(shouldParkAfterFailedAcquire(p, node)
						&& nanosTimeOut > spinForTimeoutThreshold){
					//将当前线程挂起一段时间，之后在自己醒来
					LockSupport.parkNanos(this, nanosTimeOut);
				}
				if(Thread.interrupted()){
					throw new InterruptedException();
				}
			}
		}finally {
			if(failed){
				cancelAcquire(node);
			}
		}
	}

	/**
	 * 释放锁的操作(独占模式)
	 * @param arg
	 * @return
	 */
	public final boolean release(int arg){
		//拨动密码锁，看看是否能够开锁
		if(tryRelease(arg)){
			//获取head节点
			Node h = head;
			//如果head节点不为空且等待状态不等于0就去唤醒后继结点
			if(h != null && h.waitStatus != 0){
				//唤醒后继节点
				unparkSuccessor(h);
			}
			return true;
		}
		return false;
	}

	protected boolean tryRelease(int arg) {
		throw new UnsupportedOperationException();
	}

	/**
	 * 唤醒节点的后继者
	 * @param node
	 */
	private void unparkSuccessor(Node node){
		int ws = node.waitStatus;
		//将等待状态更新为0
		if(ws < 0) {
			compareAndSetWaitStatus(node, ws, 0);
		}
		//获取给定节点的后继节点
		Node s = node.next;
		//后继节点为空或者等待状态为取消状态
		if(s != null || s.waitStatus > 0){
			s = null;
			//从后向前遍历队列找到第一个不是取消状态的节点
			for(Node t = tail; t != null && t != node; t = t.prev){
				if(t.waitStatus <= 0){
					s = t;
				}
			}
		}
		//唤醒给定节点后面首个不是取消状态的节点
		if(s != null){
			LockSupport.unpark(s.thread);
		}
	}

	public final void acquireInterruptibly(int arg)
			throws InterruptedException {
		if (Thread.interrupted())
			throw new InterruptedException();
		if (!tryAcquire(arg))
			doAcquireInterruptibly(arg);
	}

	/**
	 * 以不可中断模式获取锁(共享模式)
	 * @param arg
	 */
	public final void acquireShared(int arg){
		//1. 尝试获取锁
		if(tryAcquireShared(arg) < 0){
			//2. 如果获取失败就进入这个方法
			doAcquireShared(arg);
		}
	}

	/**
	 * 在同步队列中获取锁(共享模式)
	 * @param arg
	 */
	private void doAcquireShared(int arg) {
		//添加到同步队列中
		final Node node = addWaiter(Node.SHARED);
		boolean failed = true;
		try {
			boolean interrupted = false;
			for (;;){
				//获取当前节点的前继节点
				final Node p = node.predecessor();
				//如果前继节点为head节点，就再次尝试获取锁
				if(p == head){
					//再次尝试获取锁并返回获取状态
					//r < 0, 表示获取失败
					// r = 0,表示当前节点获取成功，但是后继节点不能再获取
					// r > 0,表示当前节点获取成功，并且后继节点同样可以获取成功
					int r = tryAcquireShared(arg);
					if(r >= 0){
						//到达这里说明当前节点已经获取锁成功了，此时它会将锁的状态信息传播给后继节点
						setHeadAndPropagate(node, r);
						//help GC
						p.next = null;
						//如果在线程阻塞期间收到中断请求, 就在这一步响应该请求
						if(interrupted){
							selfInterrupt();
						}
						failed = false;
						return;
					}
				}
				//每次获取锁失败后都会判断是否可以将线程挂起, 如果可以的话就会在parkAndCheckInterrupt方法里将线程挂起
				if(shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt()){
					interrupted = true;
				}
			}
		}finally {
			if(failed){
				cancelAcquire(node);
			}
		}
	}

	/**
	 * 尝试获取锁(共享模式)
	 * 负数：表示获取失败
	 * 零值：表示当前节点获取成功，但是后继节点不能再获取了
	 * 正数：表示当前节点获取成功，并且后继节点同样可以获取成功
	 * @param arg
	 * @return
	 */
	private int tryAcquireShared(int arg) {
		throw new UnsupportedOperationException();
	}


	/**
	 * 设置head节点并传播锁的状态(共享模式)
	 * @param node
	 * @param propagate
	 */
	private void setHeadAndPropagate(Node node, int propagate){
		Node h = head;
		//将给定节点设置为head节点
		setHead(node);
		//如果propagate大于0表明锁可以获取了
		if(propagate > 0 || h == null || h.waitStatus < 0 ||
				(h = head) == null || h.waitStatus < 0){
			//获取给定节点的后继节点
			Node s = node.next;
			//如果给定节点的后继节点为空，或者它的状态是共享状态
			if(s != null || s.isShared()){
				//唤醒后继节点
				doReleaseShared();
			}
		}
	}

	/**
	 * 释放锁的操作(共享模式)
	 */
	private void doReleaseShared(){
		for(;;){
			//获取同步队列的head节点
			Node h = head;
			if(h != null && h != tail){
				//获取head节点的等待状态
				int ws = h.waitStatus;
				//如果head节点的状态为SIGNAL,表明后面有人在排队
				if(ws == Node.SIGNAL){
					//先把head节点等待状态更新为0
					if(!compareAndSetWaitStatus(h, Node.SIGNAL,0)){
						continue;
					}
					//再去唤醒后继节点
					unparkSuccessor(h);
					//如果head结点的状态为0, 表明此时后面没人在排队, 就只是将head状态修改为PROPAGATE
				}else if(ws == 0 && !compareAndSetWaitStatus(h, 0, Node.PROPAGATE)){
					continue;
				}
				//只有保证期间head节点没有被修改过才能跳出循环
				if(h == head){
					break;
				}
			}
		}
	}

	/**
	 * 以可中断模式获取锁(共享模式)
	 * @param arg
	 * @throws InterruptedException
	 */
	public final void acquireSharedInterruptibly(int arg)
		throws InterruptedException{
		if(Thread.interrupted()){
			throw new InterruptedException();
		}
		if(tryAcquireShared(arg) < 0){
			doAcquireSharedInterruptibly(arg);
		}
	}

	private void doAcquireSharedInterruptibly(int arg)
			throws InterruptedException{
		final Node node = addWaiter(Node.SHARED);
		boolean failed = true;
		try {
			for (;;){
				final Node p = node.predecessor();
				if(p == head){
					int r = tryAcquireShared(arg);
					if(r >= 0){
						setHeadAndPropagate(node, r);
						p.next = null;
						failed = false;
						return;
					}
				}
				//如果线程在阻塞过程中收到过中断请求, 那么就会立马在这里抛出异常
				if(shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt()){
					throw new InterruptedException();
				}
			}
		}finally {
			if(failed){
				cancelAcquire(node);
			}
		}
	}
	
	public final boolean tryAcquireShareNanos(int arg, long nanosTimeout)
		throws InterruptedException{
		if(Thread.interrupted()){
			throw new InterruptedException();
		}
		return tryAcquireShared(arg) >= 0 || doAcquireSharedNanos(arg, nanosTimeout);
	}

	/**
	 * 以限定超时时间获取锁(共享模式)
	 * @param arg
	 * @param nanosTimeout
	 * @return
	 * @throws InterruptedException
	 */
	private boolean doAcquireSharedNanos(int arg, long nanosTimeout)
			throws InterruptedException{
		if(nanosTimeout <= 0L){
			return false;
		}
		final long deadline = System.nanoTime() + nanosTimeout;
		final Node node = addWaiter(Node.SHARED);
		boolean failed = true;
		try {
			for(;;){
				//获取当前节点的前继节点
				final Node p = node.predecessor();
				if(p == head){
					int r = tryAcquireShared(arg);
					if(r >= 0){
						setHeadAndPropagate(node, r);
						p.next = null;
						failed = false;
						return true;
					}
				}
				nanosTimeout = deadline  - System.nanoTime();
				//如果超时时间用完了就结束获取, 并返回失败信息
				if(nanosTimeout <= 0L){
					return false;
				}
				//1.检查是否满足将线程挂起要求(保证前继结点状态为SIGNAL)
				//2.检查超时时间是否大于自旋时间
				if(shouldParkAfterFailedAcquire(p, node) &&
						nanosTimeout > spinForTimeoutThreshold){
					//若满足上面两个条件就将当前线程挂起一段时间
					LockSupport.parkNanos(this, nanosTimeout);
				}
				//如果在阻塞时收到中断请求就立马抛出异常
				if(Thread.interrupted()){
					throw new InterruptedException();
				}
			}
		}finally {
			if(failed){
				cancelAcquire(node);
			}
		}
	}

	//释放锁的操作(共享模式)
	public final boolean releaseShared(int arg){
		//1.尝试释放锁
		if(tryReleaseShared(arg)){
			//2.如果释放成功，则唤醒其他线程
			doReleaseShared();
			return true;
		}
		return false;
	}

	private boolean tryReleaseShared(int arg) {
		throw new UnsupportedOperationException();
	}

	public class ConditionObject implements Condition{
		/** 条件队列头结点 */
		private transient Node firstWaiter;
		/** 条件队列尾节点 */
		private transient Node lastWaiter;
		
		private static final int REINTERRUPT = 1;
		
		private static final int THROW_IE = -1;
		
		public ConditionObject() {
		}

		/**
		 * 将当前线程添加到条件队列尾部
		 * @return
		 */
		private Node addConditionWaiter(){
			Node t = lastWaiter;
			if(t != null && t.waitStatus != Node.CONDITION){
				unlinkCancellWaiters();
				t = lastWaiter;
			}
			Node node = new Node(Thread.currentThread(), Node.CONDITION);
			if(t == null){
				firstWaiter = node;
			}else {
				t.nextWaiter = node;
			}
			lastWaiter = node;
			return node;
		}

		private void unlinkCancellWaiters() {
		}

		/**
		 * 响应中断的条件等待
		 * @throws InterruptedException
		 */
		@Override
		public void await() throws InterruptedException {
			if(Thread.interrupted()){
				throw new InterruptedException();
			}
			Node node = addConditionWaiter();
			//进入条件等待之前先完全释放锁
			int savedState = fullyRelease(node);
			int interruptMode = 0;
			//线程一直在while循环里面进行条件等待
			while (!isOnSyncQueue(node)){
				//进行条件等待的线程都在这里被挂起，线程被唤醒的情况有以下几种
				//1.同步队列的前继节点已取消
				//2.设置同步队列的前继节点的状态为SIGNAL失败
				//3.前继节点释放锁后唤醒当前节点
				LockSupport.park(this);
				//当前线程醒来后立马检查是否被中断，如果是则代表节点取消条件等待，此时需要将节点移除条件队列
				if((interruptMode = checkInterruptWhileWaiting(node)) != 0){
					break;
				}
			}
			//线程醒来后就会以独占模式获取锁
			if(acquireQueued(node, savedState) && interruptMode != THROW_IE){
				interruptMode = REINTERRUPT;
			}
			//这步操作主要是为防止线程在signal之前中断而导致没有与条件队列断绝联系
			if(node.nextWaiter != null){
				unlinkCancellWaiters();
			}
			//根据中断模式进行相应的中断处理
			if(interruptMode != 0){
				reportInterruptAfterWait(interruptMode);
			}
		}

		/**
		 * 结束条件等待后根据中断情况做出相应处理
		 */
		private void reportInterruptAfterWait(int interruptMode)
				throws InterruptedException {
			if (interruptMode == THROW_IE){
				throw new InterruptedException();
			}
			else if (interruptMode == REINTERRUPT){
				selfInterrupt();
			}
		}

		/**
		 * 检查条件等待时的线程中断情况
		 * 1. 中断请求在signal操作之前： THROW_ID
		 * 2. 中断请求在signal操作之后：REINTERRUPT
		 * 3. 期间没有收到任何中断请求： 0
		 * @param node
		 * @return
		 */
		private int checkInterruptWhileWaiting(Node node) {
			return Thread.interrupted() ? (transferAfterCancelledWait(node) ? THROW_IE : REINTERRUPT) : 0;
		}

		/**
		 * 将取消条件等待的节点从条件队列转移到同步队列中
		 * @param node
		 * @return
		 */
		private boolean transferAfterCancelledWait(Node node) {
			//如果这步CAS操作成功的话就表明中断发生在signal方法之前
			if(compareAndSetWaitStatus(node, Node.CONDITION, 0)){
				//状态修改成功后就将该节点放入同步队列尾部
				enq(node);
				return true;
			}
			//到这里表明CAS操作失败, 说明中断发生在signal方法之后
			while (!isOnSyncQueue(node)){
				//如果sinal方法还没有将结点转移到同步队列, 就通过自旋等待一下
				Thread.yield();
			}
			return false;
		}

		private boolean isOnSyncQueue(Node node) {
			if(node.waitStatus == Node.CONDITION || node.prev == null){
				return false;
			}
			if(node.next != null){
				return true;
			}
			return findNodeFromTail(node);
		}


		//释放锁
		private int fullyRelease(Node node) {
			boolean failed = true;
			try{
				//获取当前的同步状态
				int savedStae = getState();
				//使用当前的同步状态去释放锁
				if(release(savedStae)){
					failed = false;
					//如果释放锁成功就返回当前同步状态
					return savedStae;
				}else {
					//如果释放锁失败就抛出运行时异常
					throw new IllegalMonitorStateException();
				}
			}finally {
				//保证没有成功释放锁就将该结点设置为取消状态
				if(failed){
					node.waitStatus = Node.CANCELLED;
				}
			}
		}

		@Override
		public void awaitUninterruptibly() {

		}

		@Override
		public long awaitNanos(long nanosTimeout) throws InterruptedException {
			return 0;
		}

		@Override
		public boolean await(long time, TimeUnit unit) throws InterruptedException {
			return false;
		}

		@Override
		public boolean awaitUntil(Date deadline) throws InterruptedException {
			return false;
		}

		/**
		 * 唤醒条件队列中的下一个节点
		 */
		@Override
		public void signal() {
			//判断当前线程是否持有锁
			if(!isHeldExclusively()){
				throw new IllegalMonitorStateException();
			}
			Node first = firstWaiter;
			//如果条件队列中有排队者
			if(first != null){
				//唤醒条件队列中的头节点
				doSignal(first);
			}
		}

		/**
		 * 唤醒条件队列中的头节点
		 * @param first
		 */
		private void doSignal(Node first) {
			do {
				//1.将firstWaiter引用向后移动一位
				if((firstWaiter = first.nextWaiter) == null){
					lastWaiter = null;
				}
				//2.将头节点的后继节点引用置空
				first.nextWaiter = null;
				//3.将头结点转移到同步队列，转移完成后有可能唤醒线程
				//4.如果transferForSignal操作失败就去唤醒下一个节点
			}while (!transferForSignal(first) && (first = firstWaiter) != null);
		}

		/**
		 * 将指定节点从条件队列转移到同步队列中
		 * @param node
		 * @return
		 */
		private boolean transferForSignal(Node node) {
			//将等待状态从CONDITION设置为0
			if(!compareAndSetWaitStatus(node, Node.CONDITION, 0)){
				//如果更新状态的操作失败就直接返回false
				//可能是transferAfterCancelledWait方法先将状态改变了, 导致这步CAS操作失败
				return false;
			}
			//将该结点添加到同步队列尾部
			Node p = enq(node);
			int ws = p.waitStatus;
			if(ws > 0 || !compareAndSetWaitStatus(p, ws, Node.SIGNAL)){
				//出现以下情况就会唤醒当前线程
				//1.前继结点是取消状态
				//2.更新前继结点的状态为SIGNAL操作失败
				LockSupport.unpark(node.thread);
			}
			return true;
		}


		@Override
		public void signalAll() {

		}
	}

	private boolean isHeldExclusively() {
		throw new UnsupportedOperationException();
	}

	private boolean findNodeFromTail(Node node) {
		Node t = tail;
		for(;;){
			if(t == null){
				return true;
			}
			if(t == null){
				return false;
			}
			t = t.prev;
		}
	}

	public final boolean hasQueuedPredecessors() {
		Node t = tail;
		Node h = head;
		Node s;
		return h != t &&
				((s = h.next) == null || s.thread != Thread.currentThread());
	}

	private static final boolean compareAndSetNext(Node node,
												   Node expect,
												   Node update) {
		return unsafe.compareAndSwapObject(node, nextOffset, expect, update);
	}

	private static final Unsafe unsafe = Unsafe.getUnsafe();
	private static final long stateOffset;
	private static final long headOffset;
	private static final long tailOffset;
	private static final long waitStatusOffset;
	private static final long nextOffset;

	static {
		try {
			stateOffset = unsafe.objectFieldOffset
					(AbstractQueuedSynchronizer.class.getDeclaredField("state"));
			headOffset = unsafe.objectFieldOffset
					(AbstractQueuedSynchronizer.class.getDeclaredField("head"));
			tailOffset = unsafe.objectFieldOffset
					(AbstractQueuedSynchronizer.class.getDeclaredField("tail"));
			waitStatusOffset = unsafe.objectFieldOffset
					(Node.class.getDeclaredField("waitStatus"));
			nextOffset = unsafe.objectFieldOffset
					(Node.class.getDeclaredField("next"));

		} catch (Exception ex) { throw new Error(ex); }
	}
}
