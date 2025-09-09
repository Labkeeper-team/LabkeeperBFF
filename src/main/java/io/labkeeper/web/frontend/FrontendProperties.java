package io.labkeeper.web.frontend;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "io.labkeeper.frontend")
public class FrontendProperties {
    String yandexCaptchaSiteKey;
    String sentryDsn;
    String yandexMetrikaKey;
}
