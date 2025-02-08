package com.lch275.springBatchMonitor.controller;

import com.lch275.springBatchMonitor.service.RedisService;
import com.lch275.springBatchMonitor.util.BatchUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/job")
public class JobLauncherController {

    private final JobLauncher jobLauncher;
    private final JobRegistry jobRegistry;
    private final JobExplorer jobExplorer;
    private final JobOperator jobOperator;
    private final RedisService redisService;

    @PostMapping("/{jobName}")
    public ResponseEntity<?> lanchJob(@PathVariable String jobName, @RequestBody Map<String, Object> params) {
        try {
            Map<String, Object> res = new HashMap<>();
            if(!redisService.acquireLock(jobName)) throw new JobExecutionAlreadyRunningException("acquire lock fail");
            Job job = jobRegistry.getJob(jobName);
            JobParameters jobParameters = BatchUtil.createJobParameters(params);
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

    @DeleteMapping("/{jobName}")
    public ResponseEntity<?> stopJob(@PathVariable String jobName, @RequestBody Map<String, Object> params) {
        Set<JobExecution> jobExecutions = jobExplorer.findRunningJobExecutions(jobName);
        List<JobExecution> candidates = jobExecutions.stream()
                .filter(el -> BatchUtil.compareJobParameters(el.getJobParameters(), BatchUtil.createJobParameters(params)))
                .toList();
        for(JobExecution jobExecution : candidates) {
            try {
                Map<String, Object> res = new HashMap<>();
                jobOperator.stop(jobExecution.getId());
                res.put("id", jobExecution.getJobId());
                res.put("status", jobExecution.getStatus().toString());
                return ResponseEntity.ok(res);
            } catch (NoSuchJobExecutionException e) {
                throw new RuntimeException(e);
            } catch (JobExecutionNotRunningException e) {
                throw new RuntimeException(e);
            }
        }
        redisService.releaseLock(jobName);
        return null;
    }
}
