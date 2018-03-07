package core.framework.async;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * @author neo
 */
public interface Executor {
    <T> Future<T> submit(String action, Callable<T> task);
}
