# Database Migrations

本目录预留给 Flyway 迁移脚本。

第 1 批只创建工程边界，不创建业务表。后续进入作品状态机与真实持久化时，再新增 `VYYYYMMDDHHMM__description.sql` 形式的迁移文件。

已冻结但尚未落库的关键口径：

- `works.version INTEGER NOT NULL DEFAULT 0` 用于状态流转和并发控制。
- Workflow 启动一致性进入真实模型链路前必须补可靠 Outbox 或等价补偿机制。
- 发布包可获取前必须经过 `ModerationAdapter.preCheckPublishPackage`。
