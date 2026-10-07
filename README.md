# 在线判题 OJ 平台

前后端分离的在线判题系统。后端基于 Spring Boot 3.x（用户、题目、测试用例、提交、本地 Java 判题沙箱）；前端基于 React 18 + Vite + TypeScript + Ant Design（含 Monaco 代码编辑器）。

## 技术栈

### 后端
- Java 17
- Spring Boot 3.3.5
- MyBatis-Plus 3.5.7
- MySQL 8（5.7 亦可）
- Lombok / Hutool
- Knife4j（OpenAPI3 文档）
- Session 登录态

### 前端（`frontend/`）
- React 18 + Vite 5 + TypeScript
- Ant Design 5
- @monaco-editor/react（在线代码编辑器）
- react-router-dom 6 + axios
- dev 环境 Vite 代理 `/api` 到后端，cookie 同源，零 CORS

## 目录结构

```
com.example.oj
├── controller        # 接口层
├── service           # 业务接口
│   └── impl          # 业务实现
├── sandbox           # 代码沙箱（与判题解耦）
│   └── model
├── mapper            # MyBatis-Plus Mapper
├── model
│   ├── entity        # 数据库实体
│   ├── dto           # 入参对象
│   ├── vo            # 出参对象
│   └── enums         # 枚举
├── common            # 统一返回 / 异常
└── config            # MyBatis-Plus 配置
```

## 快速启动

### 1. 环境要求

- JDK 17
- MySQL 8
- Maven 3.6+（项目自带 mvnw）

### 2. 初始化数据库

执行 `src/main/resources/sql/oj.sql`，会创建 `oj` 库并写入示例题目与 admin 账号。

> 注意：导入时务必指定 utf8mb4 字符集，否则中文会乱码。

```bash
# 本地 MySQL
mysql -uroot -p --default-character-set=utf8mb4 < src/main/resources/sql/oj.sql

# Docker 中的 MySQL（容器名按实际替换）
docker exec -i mysql mysql -uroot -proot --default-character-set=utf8mb4 < src/main/resources/sql/oj.sql
```

### 3. 修改配置

如你的 MySQL 账号密码不是 `root/root`，请修改 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/oj?...
    username: root
    password: root
```

### 4. 启动

```bash
./mvnw spring-boot:run
```

或用 IDEA 直接运行 `OjApplication`。

### 5. 接口文档

启动后访问 Knife4j 文档：http://localhost:8080/doc.html

## 内置账号

- 用户名：`admin`
- 密码：`123456`（角色 admin）

## 接口一览

> 所有接口以 `/api` 开头；除注册/登录外，均需先登录（基于 Session）。

### 用户
- `POST /api/user/register` 注册
- `POST /api/user/login` 登录
- `GET  /api/user/logout` 登出
- `GET  /api/user/current` 获取当前登录用户

### 题目（管理类接口需 admin）
- `POST   /api/problem/add` 新增（含测试用例）
- `POST   /api/problem/update` 修改
- `POST   /api/problem/delete?id=xxx` 删除
- `GET    /api/problem/get?id=xxx` 详情
- `GET    /api/problem/list?pageNum=1&pageSize=10` 分页

### 提交
- `POST /api/submission/add` 提交代码（同步判题）
- `GET  /api/submission/get?id=xxx` 详情
- `GET  /api/submission/list/my?pageNum=1&pageSize=10` 我的提交

## 统一返回格式

```json
{ "code": 0, "data": {}, "message": "ok" }
```

错误：`code` 为业务错误码（如 40000 参数错误、40100 未登录、40300 无权限），`data` 为 null。

## 判题流程（当前为同步）

```
提交 -> 保存(PENDING) -> JudgeService(JUDGING)
      -> JavaNativeCodeSandbox 编译/运行每个用例
      -> 忽略空白比较输出 -> 更新最终状态
```

`JudgeService` 依赖 `CodeSandbox` 接口，后续替换为 Docker 沙箱或 MQ 异步判题时只需新增实现。

## 代码沙箱说明

- 用户代码类名固定为 `Main`
- 工作目录：`/tmp/oj-code/{uuid}`，运行后自动清理
- 编译超时 30s，单用例运行超时 = 题目 `timeLimit`
- 第一版不做严格内存限制（字段保留）
- 直接在宿主机执行用户代码，无进程/系统级隔离，仅适合可信环境，请勿直接公网暴露

## 前端启动（`frontend/`）

```bash
cd frontend
npm install        # 首次
npm run dev        # 默认 http://localhost:5173，自动代理 /api -> http://localhost:8080
```

- 前端 dev server（5173）通过 Vite 代理把 `/api` 转发到后端（默认 8080），session cookie 同源，无需处理跨域。
- 若后端不在 8080（如被占用改跑在 8081），指定代理目标：
  ```bash
  VITE_PROXY_TARGET=http://localhost:8081 npm run dev
  ```
- 内置登录页默认填入 `admin/123456`，登录后进入题目列表 → 题目详情（Monaco 编辑器）→ 提交 → 查看判题结果。
- 打包：`npm run build`，产物在 `frontend/dist`。
- Monaco 编辑器默认从 CDN 加载，需联网；离线环境可自行配置本地 loader。

## 课程管理模块

新增课程相关 3 张表（`course` / `course_resource` / `course_problem`），SQL 在 `src/main/resources/sql/course.sql`，需导入到 `oj` 库：

```bash
docker exec -i mysql mysql -uroot -proot --default-character-set=utf8mb4 oj < src/main/resources/sql/course.sql
```

课程模块**复用**已有 `Problem` 表，通过 `course_problem` 关联，不做选择题。

### 文件上传

- 上传根目录：`uploads/`（启动时自动创建），访问 URL 前缀：`/uploads/**`
- 静态资源映射在 `FileUploadConfig` 中通过 `addResourceHandlers` 注册到本地磁盘
- 上传大小/后缀限制（可在 `application.yml` 的 `oj.upload` 节点调整）：
  - 封面 `course/cover/{courseId}/`：5MB，`jpg/jpeg/png/webp`
  - 视频 `course/video/{courseId}/`：500MB，`mp4/mov/avi/mkv`
  - 课件 `course/ppt/{courseId}/`：100MB，`ppt/pptx/pdf`
- 文件名统一 UUID 重命名，对 `subDir` 拒绝 `..` 并做 canonical 路径校验，防止目录穿越
- Spring multipart 配置已放宽：`spring.servlet.multipart.max-file-size=1024MB`、`max-request-size=1024MB`

### 接口清单（共 14 个，均在 `/api/course` 下）

课程基本信息：

| 方法 | 路径 | 说明 | 鉴权 |
|---|---|---|---|
| POST | `/add` | 创建课程（当前登录用户作为教师） | 登录 |
| POST | `/update` | 修改课程 | 创建者或 admin |
| POST | `/delete?id=` | 删除课程（逻辑删除，含资源与题目关联） | 创建者或 admin |
| GET  | `/list` | 分页查询（支持 keyword/difficulty/status） | 登录 |
| GET  | `/get?id=` | 课程详情（含视频、PPT、题目聚合） | 登录 |

文件上传：

| 方法 | 路径 | 说明 | 鉴权 |
|---|---|---|---|
| POST | `/upload/cover` | 上传封面，返回 URL | 创建者或 admin |
| POST | `/upload/video` | 上传视频，返回资源信息 | 创建者或 admin |
| POST | `/upload/ppt`  | 上传课件，返回资源信息 | 创建者或 admin |

资源管理：

| 方法 | 路径 | 说明 | 鉴权 |
|---|---|---|---|
| POST   | `/resource/delete?id=` | 逻辑删除资源 | 创建者或 admin |
| POST   | `/resource/update` | 修改资源标题/描述/排序 | 创建者或 admin |
| GET    | `/resource/list?courseId=&type=` | 查询课程资源列表 | 登录 |

题目绑定：

| 方法 | 路径 | 说明 | 鉴权 |
|---|---|---|---|
| POST | `/problem/bind` | 给课程批量绑定题目（去重，按列表顺序设置 sortOrder） | 创建者或 admin |
| POST | `/problem/unbind?courseId=&problemId=` | 解绑单个题目 | 创建者或 admin |
| GET  | `/problem/list?courseId=` | 查询课程下的题目列表 | 登录 |

### 权限规则

- 所有写操作（增删改、上传、绑定/解绑）只允许课程的**创建者**或 **admin** 角色
- 权限校验统一走 `CourseService.checkCourseAuth(courseId, loginUser)`：课程不存在抛 `NOT_FOUND_ERROR`，无权限抛 `NO_AUTH_ERROR`
- 读操作（list / get / problem-list / resource-list）仅需登录

### 数据模型要点

- `tags` 在 DB 中以 JSON 字符串存储（如 `["Java","基础"]`），在 VO/DTO 层用 `List<String>` 交互，转换由 Service 层完成
- 新建课程默认 `status=draft`，合法状态见 `CourseStatusEnum`（draft/published/offline）
- 资源类型枚举见 `CourseResourceTypeEnum`（video/ppt）
- 所有表均含 `create_time` / `update_time` 自动填充，`is_delete` 走 MyBatis-Plus `@TableLogic` 逻辑删除
- 接口响应永远返回 VO（`CourseVO` / `CourseDetailVO` / `CourseResourceVO`），不直接暴露实体

## 后续可扩展

- Docker 沙箱 / 沙箱安全隔离
- RocketMQ 异步判题（前端改为轮询/订阅结果）
- Redis 缓存
- 比赛 / 排行榜 / Special Judge / 代码查重

## AI 学习平台开发协作

平台内置 AI 助教的目标设计、MCP 工具契约、学习数据模型和跨 Agent 窗口任务划分见 [开发协作手册](docs/ai-learning/README.md)。新开发窗口从手册和 [任务索引](docs/ai-learning/TASKS.md) 开始，按任务文件认领、验证和交接。该部分目前为开发设计，业务功能尚未实现。
