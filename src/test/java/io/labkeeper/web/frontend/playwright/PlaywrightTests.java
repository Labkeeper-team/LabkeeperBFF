package io.labkeeper.web.frontend.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.labkeeper.businesslogic.api.service.UserService;
import io.labkeeper.web.common.internal.service.yandex.YandexCaptchaService;
import io.labkeeper.web.frontend.FrontendConfig;
import io.labkeeper.web.frontend.internal.security.FrontendSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.security.oauth2.client.reactive.ReactiveOAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.HttpHandlerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ContextConfiguration;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {
        FrontendConfig.class,
        FrontendSecurityConfig.class,
        ReactiveWebServerFactoryAutoConfiguration.class,
        ReactiveSecurityAutoConfiguration.class,
        ReactiveOAuth2ClientAutoConfiguration.class,
        HttpHandlerAutoConfiguration.class
})
public class PlaywrightTests {
    @LocalServerPort
    long port;
    @MockBean
    UserService userService;
    @MockBean
    YandexCaptchaService yandexCaptchaService;

    @Test
    public void simpleTest() {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch();
            Page page = browser.newPage();
            page.navigate("http://localhost:" + port + "/");
            assertThat(page.getByText("Labkeeper")).isVisible();
        }
    }
}
