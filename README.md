# bi - 智能BI 平台

> 作者：[lyj](https://github.com/)



## 🌐 Translations

[English](./translations/en/README.md) | [Français](./translations/fr/README.md)

---


## 项目简介

### 项目介绍

基于React+Spring Boot+MQ+AIGC的 **智能BI 平台。**

传统项目（数据分析平台） ：由专业的数据分析师完成分析，
我的项目：用户只需输入想要分析的目标，并上传原始数据，系统将利用AI自动生成可视化图表和学习的分析结论。


### 业务流程
①客户端输入分析诉求和原始数据，向业务后端发送请求。②业务后端利用 AI 服务处理客户端数据，保持到数据库，并生成图表。③处理后的数据由业务后端发送给 AI 服务，AI 服务生成结果并返回给后端，最终将结果返回给客户端展示。
<img width="987" height="648" alt="image" src="https://github.com/user-attachments/assets/8285eca1-fb46-43e2-b8b1-96155d5dcea6" />





### 业务功能

- 用户登录、注册、注销、更新、检索、权限管理
- 图表AI分析、可视化展示



## 技术选型

### 后端

- Java Spring Boot 开发框架
- 存储层：MySQL 数据库 + Redis 缓存
- MyBatis-Plus 及 MyBatis X 自动生成
- Redisson 分布式锁
- Caffeine 本地缓存
- ⭐️ 基于 deepseek 大模型的通用 AI 能力
- ⭐️ redisson内置的限流工具类
- ⭐️ 线程池本地异步化改造
- ⭐️ MQ分布式异步化改造
- ⭐️ 保证上传文件安全性


### 工具
- Easy Excel 表格处理
- Hutool 工具库
- Apache Commons Lang3 工具类
- Lombok 注解
- 前端 IDE：VsCode
- 后端 IDE：JetBrains IDEA
- [CodeGeeX 智能编程助手](https://codegeex.cn/)



