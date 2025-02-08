package com.lch275.springBatchMonitor.util;

import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;

import java.util.Map;
import java.util.Objects;

public class BatchUtil {
    public static JobParameters createJobParameters(Map<String, Object> parameters) {
        JobParametersBuilder builder = new JobParametersBuilder();
        parameters.forEach((key, value) -> {
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

    public static boolean compareJobParameters(JobParameters p1, JobParameters p2) {
        if(p1 == null || p2 == null) {
            return p1 == p2;
        }
        Map<String, JobParameter<?>> params1 = p1.getParameters();
        Map<String, JobParameter<?>> params2 = p2.getParameters();

        // 파라미터 개수 비교
        if (params1.size() != params2.size()) {
            return false;
        }
        for (Map.Entry<String, JobParameter<?>> entry : params1.entrySet()) {
            String key = entry.getKey();
            JobParameter<?> param1 = entry.getValue();
            JobParameter<?> param2 = params2.get(key);

            // 키가 존재하는지 확인
            if (param2 == null) {
                return false;
            }

            // 타입 비교
            if (!Objects.equals(param1.getType(), param2.getType())) {
                return false;
            }

            // 값 비교
            if (!Objects.equals(param1.getValue(), param2.getValue())) {
                return false;
            }
        }

        return true;
    }
}
