package github.io.ddmfuhrmann.outfit.query.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

// AsyncConfigurer.getAsyncExecutor() returns SyncTaskExecutor as the default for @Async
// without an explicit executor name (used by Spring Modulith's @ApplicationModuleListener).
// @Async("stockProjectionExecutor") bypasses this default and uses the thread pool directly.
@Configuration
@EnableAsync
class StockProjectionExecutorConfig implements AsyncConfigurer {

    @Bean("stockProjectionExecutor")
    public TaskExecutor stockProjectionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("stock-proj-");
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return new SyncTaskExecutor();
    }
}
