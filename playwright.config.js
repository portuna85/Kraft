const { defineConfig, devices } = require('@playwright/test');
const gradleCmd = process.platform === 'win32' ? 'gradlew.bat' : './gradlew';
const baseURL = process.env.KRAFT_E2E_BASE_URL || 'http://localhost:18080';
const externalServer = process.env.KRAFT_E2E_EXTERNAL_SERVER === 'true';
const jarPath = process.env.KRAFT_E2E_JAR_PATH;

const serverArgs = [
  '--server.port=18080',
  '--spring.profiles.active=local',
  '--kraft.skip.required-config-validator=true',
  '--spring.datasource.url=jdbc:h2:mem:e2e;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE',
  '--spring.datasource.driver-class-name=org.h2.Driver',
  '--spring.datasource.username=sa',
  '--spring.datasource.password=',
  '--spring.jpa.hibernate.ddl-auto=create-drop',
  '--spring.flyway.enabled=false',
  '--kraft.db.connectivity-check.enabled=false',
  '--kraft.security.ops.enabled=false',
].join(' ');

module.exports = defineConfig({
  testDir: './tests/e2e',
  timeout: 30000,
  retries: 1,
  reporter: 'list',
  webServer: externalServer ? undefined : {
    command: jarPath
      ? `java -jar ${jarPath} ${serverArgs}`
      : `${gradleCmd} bootRun --args="${serverArgs}"`,
    url: baseURL,
    reuseExistingServer: false,
    timeout: jarPath ? 30000 : 120000,
  },
  use: {
    baseURL,
    trace: 'on-first-retry',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'mobile-chrome', use: { ...devices['Pixel 7'] } },
  ],
});
