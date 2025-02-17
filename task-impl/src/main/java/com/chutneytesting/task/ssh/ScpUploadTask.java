package com.chutneytesting.task.ssh;

import static com.chutneytesting.task.spi.validation.TaskValidatorsUtils.durationValidation;
import static com.chutneytesting.task.spi.validation.TaskValidatorsUtils.notBlankStringValidation;
import static com.chutneytesting.task.spi.validation.TaskValidatorsUtils.targetValidation;
import static com.chutneytesting.task.spi.validation.Validator.getErrorsFrom;
import static com.chutneytesting.task.ssh.SshClientFactory.DEFAULT_TIMEOUT;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import com.chutneytesting.task.spi.Task;
import com.chutneytesting.task.spi.TaskExecutionResult;
import com.chutneytesting.task.spi.injectable.Input;
import com.chutneytesting.task.spi.injectable.Logger;
import com.chutneytesting.task.spi.injectable.Target;
import com.chutneytesting.task.spi.time.Duration;
import com.chutneytesting.task.ssh.scp.ScpClient;
import com.chutneytesting.task.ssh.scp.ScpClientImpl;
import java.util.List;

public class ScpUploadTask implements Task {

    private final Target target;
    private final Logger logger;
    private final String local;
    private final String remote;
    private final String timeout;

    public ScpUploadTask(Target target, Logger logger, @Input("source") String source, @Input("destination") String destination, @Input("timeout") String timeout) {
        this.target = target;
        this.logger = logger;
        this.local = source;
        this.remote = destination;
        this.timeout = defaultIfEmpty(timeout, DEFAULT_TIMEOUT);
    }

    @Override
    public List<String> validateInputs() {
        return getErrorsFrom(
            notBlankStringValidation(local, "source"),
            notBlankStringValidation(remote, "destination"),
            durationValidation(timeout, "timeout"),
            targetValidation(target)
        );
    }

    @Override
    public TaskExecutionResult execute() {
        try (ScpClient client = ScpClientImpl.buildFor(target, Duration.parseToMs(timeout))) {
            client.upload(local, remote);
            return TaskExecutionResult.ok();
        } catch (Exception e) {
            logger.error(e.getMessage());
            return TaskExecutionResult.ko();
        }
    }

}

