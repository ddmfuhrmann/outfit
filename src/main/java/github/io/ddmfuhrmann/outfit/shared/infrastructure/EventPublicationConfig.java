package github.io.ddmfuhrmann.outfit.shared.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

/**
 * Makes Spring Modulith's event publication synchronous.
  * Spring Modulith's PersistentApplicationEventMulticaster picks up the available TaskExecutor
 * from the context. Providing SyncTaskExecutor here:
 *   1. Prevents Spring Boot from auto-configuring ThreadPoolTaskExecutor
 *      (@ConditionalOnMissingBean(Executor.class) backs off).
 *   2. Forces event listeners (@ApplicationModuleListener) to run in the same thread,
 *      after the business transaction commits, before the HTTP response is sent.
  * This gives read-your-writes consistency: a GET immediately after a POST/PUT/DELETE
 * will always see the indexed state in Elasticsearch.
  * If a future module needs async (eventual consistency) replication, replace this bean
 * with a ThreadPoolTaskExecutor and document eventual consistency in those endpoints.
 */
@Configuration
class EventPublicationConfig {

    @Bean
    TaskExecutor applicationTaskExecutor() {
        return new SyncTaskExecutor();
    }
}
