package io.labkeeper.web.frontend.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.labkeeper.businesslogic.api.domain.LabkeeperUser;
import io.labkeeper.businesslogic.api.service.UserService;
import io.labkeeper.web.common.CommonConfig;
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
        CommonConfig.class,
        FrontendSecurityConfig.class,
        ReactiveWebServerFactoryAutoConfiguration.class,
        ReactiveSecurityAutoConfiguration.class,
        ReactiveOAuth2ClientAutoConfiguration.class,
        HttpHandlerAutoConfiguration.class
})
public class PlaywrightTests {
    static final String TEST_EMAIL = "test@test.com";
    static final long TEST_ID = 11L;
    static final LabkeeperUser USER = new LabkeeperUser(TEST_EMAIL, TEST_ID);

    @MockBean
    UserService userService;
    @LocalServerPort
    long port;

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
