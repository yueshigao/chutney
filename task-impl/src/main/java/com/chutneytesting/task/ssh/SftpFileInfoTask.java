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
import com.chutneytesting.task.ssh.sftp.ChutneySftpClient;
import com.chutneytesting.task.ssh.sftp.SftpClientImpl;
import java.util.List;
import java.util.Map;

public class SftpFileInfoTask implements Task {

    private final Target target;
    private final Logger logger;
    private final String file;
    private final String timeout;

    public SftpFileInfoTask(Target target, Logger logger, @Input("file") String file, @Input("timeout") String timeout) {
        this.target = target;
        this.logger = logger;
        this.file = file;
        this.timeout = defaultIfEmpty(timeout, DEFAULT_TIMEOUT);
    }

    @Override
    public List<String> validateInputs() {
        return getErrorsFrom(
            notBlankStringValidation(file, "file"),
            durationValidation(timeout, "timeout"),
            targetValidation(target)
        );
    }

    @Override
    public TaskExecutionResult execute() {
        try (ChutneySftpClient client = SftpClientImpl.buildFor(target, Duration.parseToMs(timeout), logger)) {
            Map<String, Object> info = client.getAttributes(file);
            return TaskExecutionResult.ok(info);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return TaskExecutionResult.ko();
        }
    }

}
