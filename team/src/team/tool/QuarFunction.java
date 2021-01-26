package team.tool;

public interface QuarFunction<T, R, E, K, M> {
	M apply(T t, R r, E e, K k);
}
