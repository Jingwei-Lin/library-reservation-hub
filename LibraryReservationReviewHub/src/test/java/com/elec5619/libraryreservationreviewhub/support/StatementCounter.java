package com.elec5619.libraryreservationreviewhub.support;

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Counts the mapper queries run, by statement ("BookMapper.selectById"), for tests that check how many queries a
 * request needs. A test registers it as a bean (MyBatis-Plus picks up Interceptor beans) and calls
 * {@link #reset()} before the request it measures.
 */
@Intercepts({
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class})
})
public class StatementCounter implements Interceptor {

    private final Map<String, AtomicInteger> counts = new ConcurrentHashMap<>();

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        String id = ms.getId();
        String shortId = id.substring(id.lastIndexOf('.', id.lastIndexOf('.') - 1) + 1);   // Mapper.method
        counts.computeIfAbsent(shortId, k -> new AtomicInteger()).incrementAndGet();
        return invocation.proceed();
    }

    public void reset() {
        counts.clear();
    }

    /** How often one statement ran, e.g. "UserMapper.selectById" */
    public int count(String statement) {
        AtomicInteger n = counts.get(statement);
        return n == null ? 0 : n.get();
    }

    /** How many queries one mapper ran, e.g. "BookMapper" */
    public int countFor(String mapper) {
        return counts.entrySet().stream().filter(e -> e.getKey().startsWith(mapper + "."))
                .mapToInt(e -> e.getValue().get()).sum();
    }

    @Override
    public String toString() {
        return counts.toString();
    }
}
