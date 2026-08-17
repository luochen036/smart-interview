# 智面 · AI 面试学习平台

一个基于微服务架构的面试学习平台。用户可以学习互动教程、按分类练习题库、随机组卷测试、根据简历进行 AI 模拟面试，并持续追踪学习成果；管理员可以维护教程、用户、分类、题目及业务记录。

> 项目依据仓库内 `capstone.html` 教程实现，并在此基础上补齐 Arco Design 界面、角色权限和完整 CRUD 能力。

## 项目亮点

- **完整学习闭环**：题库学习 → 随机测试 → RabbitMQ 异步生成 AI 报告 → 个人中心复盘。
- **互动动画教程**：独立教程服务提供微服务、Docker、Nacos、Redis、RabbitMQ、Sentinel 与毕业实践课程。
- **定制 AI 面试**：根据简历技术关键词生成问题并逐题评分；未配置模型 Key 时自动使用本地规则兜底。
- **微服务治理**：Spring Cloud Gateway 统一入口，Nacos 服务发现，Sentinel 限流保护。
- **高性能题库**：Redis 使用 Cache-Aside 模式缓存分类题目，题目变更后主动失效缓存。
- **角色权限**：普通用户访问学习功能，管理员拥有用户、分类、题目和业务记录管理权限。
- **现代化界面**：Vue 3 + TypeScript + Arco Design，支持响应式侧边栏与按路由代码分割。

## 系统架构

```text
浏览器 / Arco Design 前端（5173）
              │ /api
              ▼
Spring Cloud Gateway（9000）
      ├── user-service（8081）──── MySQL / RabbitMQ
      ├── question-service（8082）─ MySQL / Redis
      ├── tutorial-service（8083）─ MySQL / HTML 互动课件
      └── ai-service（8000）─────── Django REST / AI 模型
                 ▲
                 └── RabbitMQ Consumer 异步回写测试报告

服务发现：Nacos（8848）   服务保护：Sentinel（8080，可选）
```

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 前端 | Vue 3、TypeScript、Vite、Pinia、Vue Router、Arco Design、Axios |
| 网关 | Spring Cloud Gateway、Nacos Discovery |
| Java 服务 | Java 17、Spring Boot 3、Spring Data JPA、Sentinel |
| AI 服务 | Python、Django、Django REST Framework、Gunicorn |
| 数据与中间件 | MySQL 8、Redis 7、RabbitMQ 3 |
| 部署 | Docker、Docker Compose、Nginx |

## 功能与权限

| 功能 | 普通用户 | 管理员 |
| --- | :---: | :---: |
| 注册、登录、个人数据概览 | ✓ | ✓ |
| 互动教程学习、进度记录 | ✓ | ✓ |
| 分类题库学习、学习打卡 | ✓ | ✓ |
| 随机测试、异步 AI 报告 | ✓ | ✓ |
| 简历 AI 模拟面试 | ✓ | ✓ |
| 用户和角色管理 |  | ✓ |
| 分类、题目 CRUD |  | ✓ |
| 教程元数据 CRUD 与发布管理 |  | ✓ |
| 学习、测试、面试记录管理 |  | ✓ |

首次启动会在用户表为空对应账号不存在时创建演示管理员：

```text
用户名：admin
密码：admin123
```

生产部署请通过 `ADMIN_USERNAME`、`ADMIN_PASSWORD` 环境变量覆盖默认值。

## 目录结构

```text
smart-interview/
├── frontend/          # Vue 3 + Arco Design 前端
├── gateway/           # Spring Cloud API 网关
├── user-service/      # 用户、学习、测试、面试记录服务
├── question-service/  # 分类题库与 Redis 缓存服务
├── tutorial-service/  # 教程数据库、上传接口与 HTML 课件服务
│   └── tutorials/     # 统一存放现有及后台上传的独立 HTML 教程
├── ai-service/        # AI 出题、评分和 MQ 消费者
├── docker-compose.yml # 完整环境编排
└── capstone.html      # 原始课程教程
```

## 快速开始

### 方式一：Docker Compose（推荐）

环境要求：Docker Desktop / Docker Engine，Docker Compose v2。

```bash
docker compose up --build
```

启动完成后访问：

- 前端：<http://localhost:5173>
- API 网关：<http://localhost:9000>
- Nacos：<http://localhost:8848/nacos>
- RabbitMQ 管理台：<http://localhost:15672>（guest / guest）

停止服务：

```bash
docker compose down
```

### 方式二：本地开发

先启动 MySQL、Redis、RabbitMQ 和 Nacos，并确认各服务 `application.yml` 中的连接信息正确。

```bash
# 用户服务
cd user-service
mvn spring-boot:run

# 题库服务
cd question-service
mvn spring-boot:run

# 网关
cd gateway
mvn spring-boot:run

# 教程服务
cd tutorial-service
mvn spring-boot:run

# AI 服务
cd ai-service
pip install -r requirements.txt
python manage.py runserver 0.0.0.0:8000

# 前端
cd frontend
npm install
npm run dev
```

如需调用真实通义千问模型，请为 AI 服务配置 `DASHSCOPE_API_KEY`；未配置时项目仍可完整演示。

## 核心接口

所有浏览器请求统一通过网关 `/api` 前缀访问。

| 模块 | 方法与路径 | 说明 |
| --- | --- | --- |
| 用户 | `POST /api/users/register`、`POST /api/users/login` | 注册与登录 |
| 用户管理 | `GET /api/users`、`PUT/DELETE /api/users/{id}` | 管理员维护用户 |
| 分类 | `GET/POST /api/questions/categories` | 查询或新增分类 |
| 分类 | `PUT/DELETE /api/questions/categories/{id}` | 修改或删除分类 |
| 题目 | `GET/POST /api/questions` | 查询或新增题目 |
| 题目 | `GET/PUT/DELETE /api/questions/{id}` | 题目详情、修改、删除 |
| 教程 | `GET /api/tutorials`、`GET /api/tutorials/{slug}/content` | 教程目录与 HTML 正文 |
| 教程管理 | `POST /api/tutorials` | 管理员上传 HTML 并创建教程 |
| 教程管理 | `PUT/DELETE /api/tutorials/{id}` | 修改或删除教程及课件 |

教程元数据保存在 MySQL 的 `tutorial_courses` 表，课件统一存放在
`tutorial-service/tutorials/`。管理员可在“管理工作台 → 教程管理”直接选择
独立 `.html` 文件上传；文件不能包含指向其他 HTML 教程的跳转链接。
| 测试 | `POST /api/questions/paper` | 随机组卷 |
| 学习记录 | `POST /api/study`、`GET /api/study/user/{userId}` | 打卡与个人进度 |
| 测试记录 | `POST /api/tests`、`GET /api/tests/user/{userId}` | 提交测试与查看报告 |
| 面试记录 | `POST /api/interviews`、`GET /api/interviews/user/{userId}` | 保存与查询面试 |
| AI | `POST /api/ai/questions`、`POST /api/ai/score` | AI 出题与评分 |

管理型接口需要携带登录返回的 `Authorization: Bearer <token>`。两个 Java 服务使用 `AUTH_SECRET` 独立校验 HMAC 签名、有效期与管理员角色；生产系统仍建议升级为 Spring Security + JWT，并对密码使用 BCrypt 哈希。

## 构建验证

```bash
cd frontend && npm run build
cd user-service && mvn test
cd question-service && mvn test
cd tutorial-service && mvn test
```

## 后续规划

- 接入 Spring Security、JWT 与刷新令牌
- 增加题目搜索、标签体系和批量导入
- 引入 ECharts 展示能力雷达图和学习趋势
- 增加服务监控、链路追踪与自动化测试

## License

本项目用于学习、课程实践与个人作品展示。
