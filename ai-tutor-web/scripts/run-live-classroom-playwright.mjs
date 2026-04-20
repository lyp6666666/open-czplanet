import { spawnSync } from 'node:child_process'

const args = ['playwright', 'test', 'e2e/live-classroom.spec.ts']
const result = spawnSync('npx', args, { stdio: 'inherit', shell: process.platform === 'win32' })
process.exit(result.status ?? 1)
