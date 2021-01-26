package team.tool;

public interface TriFunction<T, R, E, K> {
	K apply(T t, R r, E e);
}
