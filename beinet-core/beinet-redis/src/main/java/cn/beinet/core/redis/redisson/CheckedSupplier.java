package cn.beinet.core.redis.redisson;

/**
 * 受检的 Supplier
 */
@FunctionalInterface
public interface CheckedSupplier<T> {

	/**
	 * Run the Supplier
	 *
	 * @return T
	 * @throws Throwable CheckedException
	 */
	T get() throws Throwable;
}
