package com.github.wuzhudexiaopacai.oj.judge.config;

import com.github.wuzhudexiaopacai.oj.judge.job.ResultOutboxResendJob;
import com.github.wuzhudexiaopacai.oj.judge.job.StuckSubmissionScanJob;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Quartz 配置（JDBC 集群存储已在 nacos 配置，此处仅注册 JobDetail + Trigger）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Configuration
public class QuartzConfig {

    @Value("${ai-oj.judge.stuck-scan-cron:0 * * * * ?}")
    private String stuckScanCron;

    @Value("${ai-oj.judge.outbox-resend-cron:0/20 * * * * ?}")
    private String outboxResendCron;

    /**
     * 卡死提交扫描 JobDetail（durability：无触发器时也保留定义）
     */
    @Bean
    public JobDetail stuckSubmissionScanJobDetail() {
        return JobBuilder.newJob(StuckSubmissionScanJob.class)
                .withIdentity("stuckSubmissionScanJob", "judge")
                .storeDurably()
                .build();
    }

    /**
     * 卡死提交扫描 CronTrigger
     */
    @Bean
    public Trigger stuckSubmissionScanTrigger(JobDetail stuckSubmissionScanJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(stuckSubmissionScanJobDetail)
                .withIdentity("stuckSubmissionScanTrigger", "judge")
                .withSchedule(CronScheduleBuilder.cronSchedule(stuckScanCron))
                .build();
    }

    /**
     * 结果 Outbox 补投 JobDetail
     */
    @Bean
    public JobDetail resultOutboxResendJobDetail() {
        return JobBuilder.newJob(ResultOutboxResendJob.class)
                .withIdentity("resultOutboxResendJob", "judge")
                .storeDurably()
                .build();
    }

    /**
     * 结果 Outbox 补投 CronTrigger（默认每 20 秒扫一次待投递记录）
     */
    @Bean
    public Trigger resultOutboxResendTrigger(JobDetail resultOutboxResendJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(resultOutboxResendJobDetail)
                .withIdentity("resultOutboxResendTrigger", "judge")
                .withSchedule(CronScheduleBuilder.cronSchedule(outboxResendCron))
                .build();
    }
}
