package br.com.emr.emrfinancas.config;

import java.lang.management.ManagementFactory;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/** Bounded, inclusive startup intervals; never logs arbitrary bean tags or values. */
@Component
public class StartupMetricsReporter implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StartupMetricsReporter.class);
    private static final Set<String> BEANS = Set.of("dataSource", "flywayInitializer", "entityManagerFactory");
    private final boolean enabled;

    public StartupMetricsReporter(@Value("${app.startup.metrics.enabled:true}") boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (event.getApplicationContext().getApplicationStartup() instanceof BufferingApplicationStartup startup) {
            var timeline = startup.drainBufferedTimeline();
            if (enabled) {
                for (var interval : timeline.getEvents()) {
                    var step = interval.getStartupStep();
                    String stage = null;
                    if ("spring.context.refresh".equals(step.getName())) {
                        stage = "spring.context.refresh";
                    } else if ("spring.beans.instantiate".equals(step.getName())) {
                        for (var tag : step.getTags()) {
                            if ("beanName".equals(tag.getKey()) && BEANS.contains(tag.getValue())) {
                                stage = "bean." + tag.getValue();
                            }
                        }
                    }
                    if (stage != null) {
                        LOGGER.info("startup stage={} durationMs={} inclusive=true", stage,
                                Math.max(0, interval.getDuration().toMillis()));
                    }
                }
            }
        }
        if (enabled) {
            var heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
            LOGGER.info("startup stage=ready durationMs={} jvmUptimeMs={} heapUsedBytes={} heapMaxBytes={}",
                    event.getTimeTaken() == null ? 0 : Math.max(0, event.getTimeTaken().toMillis()),
                    ManagementFactory.getRuntimeMXBean().getUptime(), heap.getUsed(), heap.getMax());
        }
    }
}
