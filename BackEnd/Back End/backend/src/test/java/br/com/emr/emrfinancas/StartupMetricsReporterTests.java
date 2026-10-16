package br.com.emr.emrfinancas;

import br.com.emr.emrfinancas.config.StartupMetricsReporter;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.support.GenericApplicationContext;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class StartupMetricsReporterTests {
    @Test
    void reportsOnlyApprovedStagesAndDrainsSensitiveTags(CapturedOutput output) {
        var startup = new BufferingApplicationStartup(512);
        var context = new GenericApplicationContext();
        context.setApplicationStartup(startup);
        var refresh = startup.start("spring.context.refresh");
        refresh.tag("url", "sensitive-jdbc-url").end();
        var datasource = startup.start("spring.beans.instantiate");
        datasource.tag("beanName", "dataSource").tag("secret", "sensitive-token").end();
        startup.start("unapproved.stage").tag("beanName", "user-email@example.invalid").end();

        new StartupMetricsReporter(true).onApplicationEvent(ready(context));

        assertThat(output).contains("stage=spring.context.refresh", "stage=bean.dataSource", "stage=ready")
                .doesNotContain("sensitive-jdbc-url", "sensitive-token", "user-email@example.invalid", "unapproved.stage");
        assertThat(startup.getBufferedTimeline().getEvents()).isEmpty();
        assertThat(output).doesNotContain("durationMs=-");
        context.close();
    }

    @Test
    void disabledReportingStillReleasesTheBuffer(CapturedOutput output) {
        var startup = new BufferingApplicationStartup(512);
        var context = new GenericApplicationContext();
        context.setApplicationStartup(startup);
        startup.start("spring.context.refresh").end();
        new StartupMetricsReporter(false).onApplicationEvent(ready(context));
        assertThat(startup.getBufferedTimeline().getEvents()).isEmpty();
        assertThat(output).doesNotContain("stage=ready", "stage=spring.context.refresh");
        context.close();
    }

    private ApplicationReadyEvent ready(GenericApplicationContext context) {
        return new ApplicationReadyEvent(new SpringApplication(EmrFinancasApplication.class),
                new String[0], context, Duration.ofMillis(1200));
    }
}
