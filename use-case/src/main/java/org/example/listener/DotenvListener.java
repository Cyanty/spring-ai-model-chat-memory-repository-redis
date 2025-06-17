package org.example.listener;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;

/** 可通过设置 --dev.dotenv.enable=true 开启（默认关闭）读取 .my_env 文件 */
@Slf4j
public class DotenvListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    public static final String DOTENV_PROPERTY = "dev.dotenv.enable";

    /** 监听 ApplicationEnvironmentPreparedEvent 事件，在 Environment 准备完成后执行 */
    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        if (!event.getEnvironment().getProperty(DOTENV_PROPERTY, Boolean.class, false)) {
            return;
        }

        log.info("Set the dev environment variable");
        Dotenv.configure()
                .directory(System.getProperty("user.dir") + "/config/env")
                .filename(".my_env")
                .systemProperties()
                .ignoreIfMalformed()
                .ignoreIfMissing()
                .load();
    }
}
