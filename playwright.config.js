const { defineConfig, devices } = require('@playwright/test');

module.exports = defineConfig({
  testDir: './tests/e2e',
  timeout: 30000,
  retries: 1,
  reporter: 'list',
  webServer: {
    command: 'gradlew.bat bootRun --args="--server.port=18080 --spring.profiles.active=local --kraft.skip.required-config-validator=true --spring.datasource.url=jdbc:h2:mem:e2e;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE --spring.datasource.driver-class-name=org.h2.Driver --spring.datasource.username=sa --spring.datasource.password= --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false --kraft.db.connectivity-check.enabled=false --kraft.security.ops.enabled=false"',
    url: 'http://localhost:18080',
    reuseExistingServer: false,
    timeout: 120000,
  },
  use: {
    baseURL: 'http://localhost:18080',
    trace: 'on-first-retry',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
  ],
});
