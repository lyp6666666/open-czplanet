# 运行与配置

## 默认端口

- `18086`

## 默认任务队列

- Redis DB 2
- queue name: `ai-agent`

## 默认 LLM provider

- `volcengine-ark`

## 当前关键环境变量

- `AI_AGENT_DATABASE_URL`
- `AI_AGENT_REDIS_URL`
- `AI_AGENT_QUEUE_NAME`
- `AI_AGENT_USE_ASYNC_WORKER`
- `AI_AGENT_LLM_PROVIDER`
- `AI_AGENT_LLM_BASE_URL`
- `AI_AGENT_LLM_API_KEY`
- `AI_AGENT_LLM_MODEL`
- `AI_AGENT_INTERNAL_TOKEN`
- `AI_AGENT_LLM_ARK_ENDPOINT_ID`
- `AI_AGENT_TENCENT_ASR_ENABLED`
- `AI_AGENT_TENCENT_ASR_APP_ID`
- `AI_AGENT_TENCENT_ASR_SECRET_ID`
- `AI_AGENT_TENCENT_ASR_SECRET_KEY`
- `AI_AGENT_TENCENT_SPEECH_SDK_PATH`

## 本地开发

```bash
cd ai-agent-service
sh scripts/bootstrap_env.sh
AI_AGENT_USE_ASYNC_WORKER=false sh scripts/run_dev.sh
```

## Worker

```bash
cd ai-agent-service
sh scripts/bootstrap_env.sh
sh scripts/run_worker.sh
```

## 远程开发机同步

代码同步：

```bash
REMOTE_SYNC_DELETE=0 bash scripts/dev_remote_sync_up.sh
```

数据库迁移：

```bash
ssh root@111.228.20.88 "cd /opt/ai-platform && sh scripts/db_apply_migrations.sh"
```

## 当前注意事项

- 第一版默认不会被现有启动脚本自动拉起。
- 如果需要接入统一启动脚本，应优先增加显式开关，避免影响现有 Java 服务稳定性。
- 腾讯云实时 ASR 依赖官方 `tencentcloud-speech-sdk-python` 代码或等价部署方式。
- 火山方舟当前按 OpenAI 兼容接口接入，`model` 推荐直接填写 EP：`ep-20260420222959-g4vrz`
