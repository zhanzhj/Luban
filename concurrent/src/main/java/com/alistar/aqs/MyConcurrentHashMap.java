package com.alistar.aqs;

import sun.misc.Unsafe;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;


public class MyConcurrentHashMap<K, V> extends AbstractMap<K, V>{

	private static final int MAXIMUN_CAPACITY = 1 << 30;
	private static final int DEFAULT_CAPACITY = 16;
	private transient volatile int sizeCtl;

	static final int MOVED = -1;
	private static final int HASH_BITS = 0x7fffffff;

	private transient volatile Node<K, V>[] table;

	public MyConcurrentHashMap() {
	}

	public MyConcurrentHashMap(int initialCapacity) {
		if(initialCapacity < 0){
			throw new IllegalArgumentException();
		}
		int cap = ((initialCapacity > MAXIMUN_CAPACITY >>> 1)) ?
					MAXIMUN_CAPACITY : tableSizeFor(initialCapacity + (initialCapacity >>> 1) + 1);
		this.sizeCtl = cap;
	}

	/**
	 * 返回给定2的冥次方
	 * @param c
	 * @return
	 */
	private int tableSizeFor(int c) {
		int n = c - 1;
		n |= n >>> 1;
		n |= n >>> 2;
		n |= n >>> 4;
		n |= n >>> 8;
		n |= n >>> 16;
		return (n < 0) ? 1 : (n >= MAXIMUN_CAPACITY) ? MAXIMUN_CAPACITY : n + 1;
	}

	/**
	 * 放入元素
	 * @param key
	 * @param value
	 * @return
	 */
	@Override
	public V put(K key, V value) {
		return putVal(key, value, false);
	}

	/**
	 * 
	 * @param key
	 * @param value
	 * @param onlyIfAbsent 元素是否存在
	 * @return
	 */
	private V putVal(K key, V value, boolean onlyIfAbsent) {
		if(key == null || value == null){
			throw new NullPointerException();
		}
		int hash = spread(key.hashCode());
		int binCount = 0;
		for(Node<K,V>[] tab = table;;){
			Node<K,V> f;
			int n, i, fh;
			if(tab == null || (n = tab.length) == 0){
				tab = initTable();
			}else if((f = tabAt(tab, i = (n -1) & hash))  == null){
				if(casTabAt(tab, i, null, new Node<K,V>(hash, key, value,null))){
					break;
				}
			}else if((fh = f.hash) == MOVED){
				tab = helpTransfer(tab, f);
			}else {
				V oldVal = null;
				synchronized (f){
					if(tabAt(tab, i) == f){
						if(fh >= 0){
							binCount = 1;
							for(Node<K,V> e = f; ; ++binCount){
								K ek;
								if(e.hash == hash &&
										((ek = e.key) == key ||
												(ek != null && key.equals(ek)))){
									oldVal = e.val;
									if(!onlyIfAbsent){
										e.val = value;
									}
									break;
								}
								Node<K,V> pred = e;
								if((e = e.next) == null){
									pred.next = new Node<K,V>(hash, key, value, null);
									break;
								}
							}
						}else if (f instanceof TreeBin){

						}
					}
				}
			}
		}
		return null;
	}


	private static final sun.misc.Unsafe U = Unsafe.getUnsafe();
	private static final long SIZECTL;
	private static final long ABASE;
	private static final int ASHIFT;
	static {
		try {
			Class<?> k = MyConcurrentHashMap.class;
			SIZECTL = U.objectFieldOffset(k.getDeclaredField("sizeCtl"));
			Class<?> ak = Node[].class;
			ABASE = U.arrayBaseOffset(ak);
			int scale = U.arrayIndexScale(ak);
			if ((scale & (scale - 1)) != 0){
				throw new Error("data type scale not a power of two");
			}
			ASHIFT = 31 - Integer.numberOfLeadingZeros(scale);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		}
	}

	private Node<K,V>[] initTable() {
		Node<K,V>[] tab = table;
		int sc = sizeCtl;
		while (tab == null || tab.length == 0){
			if(sc < 0){
				Thread.yield();
			}else if(U.compareAndSwapInt(this, SIZECTL, sc, -1)){
				try {
					if(tab == null || tab.length == 0){
						int n = (sc > 0) ? sc : DEFAULT_CAPACITY;
						Node<K,V>[] nt = new Node[n];
						table = tab = nt;
						sc = n - (n >>> 2);
					}
				}finally {
					sizeCtl = sc;
				}
			}
		}
		return tab;
	}

	private Node<K,V> tabAt(Node<K,V>[] tab, int i) {
		return (Node<K,V>) U.getObjectVolatile(tab, ((long)i << ASHIFT) + ABASE);
	}

	private boolean casTabAt(Node<K,V>[] tab, int i, Node<K,V> c, Node<K,V> v) {
		return U.compareAndSwapObject(tab, ((long)i << ASHIFT) + ABASE, c, v);
	}

	private Node<K,V>[] helpTransfer(Node<K,V>[] tab, Node<K,V> f) {
		return null;
	}


	/**
	 * 计算key的hash
	 * @param h
	 * @return
	 */
	private int spread(int h) {
		return (h ^ (h >>> 16))& HASH_BITS;
	}


	static class Node<K, V> implements Map.Entry<K, V>{
		final int hash;
		final K key;
		volatile V val;
		volatile Node<K,V> next;

		public Node(int hash, K key, V val, Node<K, V> next) {
			this.hash = hash;
			this.key = key;
			this.val = val;
			this.next = next;
		}

		@Override
		public final K getKey() {
			return key;
		}

		@Override
		public final V getValue() {
			return val;
		}

		@Override
		public final V setValue(V value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public final int hashCode(){
			return key.hashCode() ^ val.hashCode();
		}

		@Override
		public boolean equals(Object o) {
			Object k, v, u;
			Map.Entry<?,?> e;
			return (o instanceof Map.Entry<?,?>) &&
					(k = (e = (Map.Entry<?,?> )o).getKey()) != null &&
					(v = e.getValue()) != null &&
					(k == key || k.equals(key)) &&
					(v == (u = val) || v.equals(u));
		}

		Node<K,V> find(int h, Object k){
			Node<K,V> e = this;
			if(k != null){
				do {
					K ek = e.key;
					if(e.hash == h && (ek == k || (ek != null && k.equals(ek)))){
						return e;
					}
				}while ((e = e.next) != null);
			}
			return null;
		}
	}

	static final class TreeNode<K,V> extends Node<K,V>{
		TreeNode<K,V> parent;
		TreeNode<K,V> left;
		TreeNode<K,V> right;
		TreeNode<K,V> prev;
		boolean red;

		public TreeNode(int hash, K key, V val, Node<K, V> next, TreeNode<K, V> parent) {
			super(hash, key, val, next);
			this.parent = parent;
		}

		@Override
		Node<K, V> find(int h, Object k){
			return findTreeNode(h, k, null);
		}

		private TreeNode<K,V> findTreeNode(int h, Object k, Class<?> kc) {
			if(k != null){
				TreeNode<K,V> p = this;
				do{
					int ph, dir;
					K pk;
					TreeNode<K,V> q;
					TreeNode<K,V> pl = p.left, pr = p.right;
					if((ph = p.hash) > h){
						p = pl;
					}else if(ph < h){
						p = pr;
					}else if((pk = p.key) == k || (pk != null && k.equals(pk))){
						return p;
					}else if(pl == null){
						p = pr;
					}else if(pr == null){
						p = pl;
					}else if((q = pr.findTreeNode(h,k,kc)) != null){
						return q;
					}else {
						p = pl;
					}
 				}while (p != null);
			}
			return null;
		}
	}

	static final class TreeBin<K, V> extends Node<K,V> {
		TreeNode<K,V> root;
		volatile TreeNode<K,V> first;
		volatile Thread waiter;
		volatile int lockState;

		// values for lockState
		static final int WRITER = 1; // set while holding write lock
		static final int WAITER = 2; // set when waiting for write lock
		static final int READER = 4; // increment value for setting read lock


		public TreeBin(int hash, K key, V val, Node<K, V> next) {
			super(hash, key, val, next);
		}
	}


	@Override
	public Set<Entry<K, V>> entrySet() {
		return null;
	}


	public static void main(String[] args) {
		MyConcurrentHashMap obj = new MyConcurrentHashMap();
		int i = obj.tableSizeFor(5);
		System.out.println(i);
		System.out.println(Integer.toBinaryString(HASH_BITS));
	}
}
