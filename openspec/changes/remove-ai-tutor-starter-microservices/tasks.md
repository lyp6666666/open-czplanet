---
title: 执行任务清单（移除 ai-tutor-starter）
status: draft
date: 2026-03-25
---

# 任务拆解

## A. 准备与基线

1) 记录当前可运行基线（全量 `mvn test` + 各服务启动现状）
2) 明确 Nacos 命名空间与 group 策略（dev/prod），补齐各服务约定

## B. Nacos Config 微服务化

3) 为每个服务引入 `nacos-config` 依赖并补齐 `spring.cloud.nacos.config.*`
4) 为每个服务补齐 `spring.config.import=optional:nacos:...`（公共 + 领域 + 服务专用）
5) 保留本地 `application.yml` 作为兜底默认值（不删除）

## C. 移除 ai-tutor-starter

6) 梳理 `ai-tutor-starter` 内代码用途与依赖方向
7) 迁移可复用代码到公共模块（默认 `ai-tutor-common`）
8) 删除/归位 monolith-only 代码（Local Facade 等跨服务内部依赖）
9) 从父工程移除模块并清理引用（pom/modules、文档、脚本、IDE 启动项）

## D. 一键启动脚本

10) 新增脚本一键启动：Docker 依赖 + 全服务启动 + 日志落盘 + 停止能力

## E. 验证与回归

11) 对每个服务做独立启动验证（Config 拉取 + Discovery 注册 + 最小接口验证）
12) 全量构建与测试：`mvn test`
13) 更新 skill 的 Change Log（记录本次迁移与启动方式变更）

