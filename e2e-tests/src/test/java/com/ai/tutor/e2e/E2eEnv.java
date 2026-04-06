package com.ai.tutor.e2e;

public class E2eEnv {

    public final String mysqlUrl;
    public final String mysqlUser;
    public final String mysqlPassword;

    public final String imBaseUrl;
    public final String paymentBaseUrl;
    public final String adminBaseUrl;

    public final String gatewaySignSecret;
    public final String brokerageAdminToken;

    public E2eEnv(String mysqlUrl,
                  String mysqlUser,
                  String mysqlPassword,
                  String imBaseUrl,
                  String paymentBaseUrl,
                  String adminBaseUrl,
                  String gatewaySignSecret,
                  String brokerageAdminToken) {
        this.mysqlUrl = mysqlUrl;
        this.mysqlUser = mysqlUser;
        this.mysqlPassword = mysqlPassword;
        this.imBaseUrl = imBaseUrl;
        this.paymentBaseUrl = paymentBaseUrl;
        this.adminBaseUrl = adminBaseUrl;
        this.gatewaySignSecret = gatewaySignSecret;
        this.brokerageAdminToken = brokerageAdminToken;
    }

    public static E2eEnv load() {
        String mysqlUrl = getenv("E2E_MYSQL_URL", "jdbc:mysql://localhost:3306/ai_tutor?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true");
        String mysqlUser = getenv("E2E_MYSQL_USER", "root");
        String mysqlPassword = getenv("E2E_MYSQL_PASSWORD", "Aa123456");

        String imBaseUrl = getenv("E2E_IM_BASE_URL", "http://localhost:18082");
        String paymentBaseUrl = getenv("E2E_PAYMENT_BASE_URL", "http://localhost:18083");
        String adminBaseUrl = getenv("E2E_ADMIN_BASE_URL", "http://localhost:18084");

        String gatewaySignSecret = getenv("E2E_GATEWAY_SIGN_SECRET", "0123456789abcdef0123456789abcdef");
        String brokerageAdminToken = getenv("E2E_BROKERAGE_ADMIN_TOKEN", "E2E_ADMIN_TOKEN");
        return new E2eEnv(mysqlUrl, mysqlUser, mysqlPassword, imBaseUrl, paymentBaseUrl, adminBaseUrl, gatewaySignSecret, brokerageAdminToken);
    }

    private static String getenv(String key, String defaultValue) {
        String v = System.getenv(key);
        if (v == null || v.trim().isEmpty()) {
            return defaultValue;
        }
        return v.trim();
    }
}

