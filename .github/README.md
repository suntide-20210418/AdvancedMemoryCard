# GitHub Actions 工作流说明

本项目包含以下GitHub Actions工作流：

## 🏗️ 构建和测试 (build.yml)
- **触发条件**: 推送到主分支或Pull Request
- **功能**: 
  - 使用Java 17构建项目
  - 运行所有测试
  - 上传构建产物作为artifact
  - 上传测试报告

## 🚀 发布 (release.yml)
- **触发条件**: 创建带`v`前缀的标签 (如 `v1.0.0`)
- **功能**:
  - 自动构建发布版本
  - 创建GitHub Release
  - 上传构建的jar文件作为Release Asset

## ✨ 代码质量检查 (quality.yml)
- **触发条件**: 推送或Pull Request到主分支
- **功能**:
  - 运行Spotless代码格式化检查
  - 执行自定义lint检查
  - 生成并上传Javadoc文档

## 🔍 PR验证 (pr-validation.yml)
- **触发条件**: 创建Pull Request
- **功能**:
  - 验证Gradle包装器
  - 检查合并冲突
  - 验证模组元数据文件
  - 验证构建配置文件

## 环境变量
工作流使用的环境变量定义在 `.github/vars.env` 文件中。

## 使用说明
1. **自动触发**: 推送到主分支会自动运行构建和质量检查
2. **PR检查**: 创建Pull Request会运行验证和测试工作流
3. **手动发布**: 创建版本标签(`v1.0.0`)会自动发布新版本
4. **查看结果**: 在Actions标签页查看工作流执行状态和日志

## 配置要求
- Java 17
- Minecraft 1.20.1
- Forge 47.4.10
- Gradle包装器

所有工作流都会自动处理依赖下载和环境配置。