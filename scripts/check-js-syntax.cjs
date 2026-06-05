const { spawnSync } = require('node:child_process');
const fs = require('node:fs');
const path = require('node:path');

const roots = [
  'playwright.config.js',
  'postcss.config.cjs',
  'src/main/resources/static/js',
  'tests/e2e',
];

const files = roots.flatMap((root) => collectJsFiles(path.resolve(root)));
let failed = false;

for (const file of files) {
  const result = spawnSync(process.execPath, ['--check', file], { stdio: 'inherit' });
  if (result.status !== 0) {
    failed = true;
  }
}

if (failed) {
  process.exit(1);
}

console.log(`JavaScript syntax validation passed (${files.length} files).`);

function collectJsFiles(target) {
  if (!fs.existsSync(target)) {
    return [];
  }
  const stat = fs.statSync(target);
  if (stat.isFile()) {
    return isJavaScriptFile(target) ? [target] : [];
  }
  if (!stat.isDirectory()) {
    return [];
  }
  return fs.readdirSync(target, { withFileTypes: true })
    .flatMap((entry) => collectJsFiles(path.join(target, entry.name)));
}

function isJavaScriptFile(file) {
  return file.endsWith('.js') || file.endsWith('.cjs') || file.endsWith('.mjs');
}
