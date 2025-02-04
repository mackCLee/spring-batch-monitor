package com.lch275.springBatchMonitor.controller;

import com.lch275.springBatchMonitor.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/job")
public class JobLauncherController {

    private final JobLauncher jobLauncher;
    private final JobRegistry jobRegistry;
    private final RedisService redisService;

    @PostMapping("/{jobName}")
    public ResponseEntity<?> lanchJob(@PathVariable String jobName, @RequestBody Map<String, Object> params) {
        try {
            Map<String, Object> res = new HashMap<>();
            if(!redisService.acquireLock(jobName)) throw new JobExecutionAlreadyRunningException("acquire lock fail");
            Job job = jobRegistry.getJob(jobName);
            JobParameters jobParameters = createJobParameters(params);
            JobExecution jobExecution = jobLauncher.run(job, jobParameters);
            redisService.releaseLock(jobName);
            res.put("id", jobExecution.getJobId());
            res.put("status", jobExecution.getStatus().toString());
            return ResponseEntity.ok(res);
        } catch(NoSuchJobException | JobInstanceAlreadyCompleteException | JobExecutionAlreadyRunningException |
                JobParametersInvalidException | JobRestartException e) {
            throw new RuntimeException(e);
        }
    }

    private JobParameters createJobParameters(Map<String, Object> params) {
        JobParametersBuilder builder = new JobParametersBuilder();
        params.forEach((key, value) -> {
            if (value instanceof String) {
                builder.addString(key, (String) value);
            } else if (value instanceof Long) {
                builder.addLong(key, (Long) value);
            } else if (value instanceof Double) {
                builder.addDouble(key, (Double) value);
            }
        });
        return builder.toJobParameters();
    }
}
