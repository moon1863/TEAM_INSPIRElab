package team.tool;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class PriorityQueueWithIndex<T extends Comparable<T>> {

	private PriorityQueue<Item<T>> pbq;
	
	static public class Item<T extends Comparable<T>> implements Comparable<Item<T>>{
		T t;
		int index;
		public Item(T t, int index) {
			this.t = t;
			this.index = index;
		}

		public int getIndex() {
			return index;
		}
		public T getT() {
			return t;
		}
		@Override
		public int compareTo(Item<T> o) {
			// TODO Auto-generated method stub
			return t.compareTo(o.t);
		}
	}

	public PriorityQueueWithIndex() {
		pbq = new PriorityQueue<Item<T>>();
	}
	public PriorityQueueWithIndex(Comparator<? super Item<T>> comparator) {
		pbq = new PriorityQueue<Item<T>>(comparator);
	}
	public void add(T t) {
		pbq.add(new Item<T>(t, pbq.size()));
	}
	public PriorityQueueWithIndex(T[] items, Comparator<? super Item<T>> comparator) {
		pbq = new PriorityQueue<Item<T>>(comparator);
		for(int i = 0; i < items.length; i ++)
			pbq.add(new Item<T>(items[i], i));
	}
	
	public PriorityQueueWithIndex(T[] items) {
		pbq = new PriorityQueue<Item<T>>();
		for(int i = 0; i < items.length; i ++)
			pbq.add(new Item<T>(items[i], i));
	}
	
	public PriorityQueueWithIndex(List<T> items, Comparator<? super Item<T>> comparator) {
		pbq = new PriorityQueue<Item<T>>(comparator);
	    for(int i = 0;i < items.size(); i ++)
	    	pbq.add(new Item<T>(items.get(i), i));
	        
	}
	
	public PriorityQueueWithIndex(List<T> items) {
		pbq = new PriorityQueue<Item<T>>();
	    for(int i = 0;i < items.size(); i ++)
	    	pbq.add(new Item<T>(items.get(i), i));
	        
	}
	
	public Item<T> removeFirst() {
		return pbq.remove();
	}
	
	public boolean hasFirst() {
		return !pbq.isEmpty();
	}
	
	public int size() {
		return pbq.size();
	}
}
