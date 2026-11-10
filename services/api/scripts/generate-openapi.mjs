import { spawn, spawnSync } from 'node:child_process';
import { access, rename, rm, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { setTimeout as sleep } from 'node:timers/promises';
import { fileURLToPath } from 'node:url';

const apiDirectory = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const target = path.resolve(apiDirectory, '../../docs/openapi/openapi.yaml');
const temporaryFile = `${target}.${process.pid}.tmp`;
const endpoint = 'http://localhost:8080/v3/api-docs.yaml';
const isWindows = process.platform === 'win32';
const wrapper = isWindows ? 'gradlew.bat' : './gradlew';
const server = spawn(wrapper, ['bootRun', '--args=--spring.profiles.active=local'], {
  cwd: apiDirectory,
  shell: isWindows,
  stdio: 'inherit',
});

let exited = false;
let exitCode;
let spawnError;
server.once('error', (error) => {
  spawnError = error;
  exited = true;
});
server.once('exit', (code) => {
  exited = true;
  exitCode = code;
});

async function stopServer() {
  if (exited || server.pid === undefined) return;

  if (isWindows) {
    spawnSync('taskkill', ['/PID', String(server.pid), '/T', '/F'], { stdio: 'ignore' });
  } else {
    server.kill('SIGTERM');
  }

  await Promise.race([
    new Promise((resolve) => server.once('exit', resolve)),
    sleep(5000),
  ]);
}

try {
  await rm(temporaryFile, { force: true });

  let downloaded = false;
  for (let attempt = 0; attempt < 120; attempt += 1) {
    if (exited) {
      if (spawnError) throw new Error(`Could not start local bootRun: ${spawnError.message}`);
      throw new Error(`Local bootRun exited before the API was ready (code ${exitCode}).`);
    }

    try {
      const response = await fetch(endpoint, { signal: AbortSignal.timeout(5000) });
      if (!response.ok) {
        throw new Error(`Endpoint returned HTTP ${response.status}.`);
      }

      const definition = await response.text();
      if (!definition.trim()) {
        throw new Error('Endpoint returned an empty API definition.');
      }

      await writeFile(temporaryFile, definition, 'utf8');
      downloaded = true;
      break;
    } catch (error) {
      if (attempt === 119) throw new Error(`Timed out waiting for ${endpoint}: ${error.message}`);
      await sleep(1000);
    }
  }

  if (!downloaded) throw new Error(`Failed to download ${endpoint}.`);

  await access(path.dirname(target));
  await rename(temporaryFile, target);
  console.log(`OpenAPI definition written to ${target}`);
} finally {
  await rm(temporaryFile, { force: true });
  await stopServer();
}
