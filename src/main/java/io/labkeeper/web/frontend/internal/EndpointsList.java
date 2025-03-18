package io.labkeeper.web.frontend.internal;

public class EndpointsList {
    private EndpointsList() {}

    // PAGES
    public static final String HOME_PAGE = "/";
    public static final String PROJECT_PAGE = "/project/{id}";
    public static final String PROJECTS_PAGE = "/projects";
    public static final String DEFAULT_PROJECT_PAGE = "/project/default";

    // ENDPOINTS
    public static final String FORM_LOGIN_ENDPOINT = "/formlogin";
    public static final String USER_INFO_ENDPOINT = "/user-info";
    public static final String SSO_LOGIN_ENDPOINT = "/oauth2/authorization/sso";
    public static final String SSO_CODE_ENDPOINT = "/login/oauth2/code/sso";
    public static final String LOGOUT_ENDPOINT = "/logout";
    public static final String EMAIL_ENDPOINT = "/email";
    public static final String CODE_ENDPOINT = "/code";
    public static final String SET_PASSWORD_ENDPOINT = "/password";
}
