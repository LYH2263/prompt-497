# MEMS - 矿井提升机设备管理系统 (Mine Hoist Equipment Management System)

一个现代化的、容器化的矿井提升机设备管理平台，采用 Spring Boot 后端和 Vue 3 前端构建。

## 🛠 技术栈
- **前端**: Vue 3, Vite, TailwindCSS
- **后端**: Spring Boot 3, Spring Security, JPA
- **数据库**: PostgreSQL 15

## 🚀 如何运行
本项目已完全容器化。

1. 确保已运行 Docker Desktop。
2. 在项目根目录下，运行：
   ```bash
   docker compose up --build
   ```
3. 停止服务：
   ```bash
   docker compose down
   ```

## 🔍 访问应用 (指定端口)
为防止端口冲突，本项目配置了以下**冷门端口**：

- **前端**: [http://localhost:38080](http://localhost:38080) (宿主机端口 38080)
- **后端**: [http://localhost:38081](http://localhost:38081) (宿主机端口 38081)
- **数据库**: localhost:35432 (宿主机端口 35432)

## 🧪 测试账号
- **管理员**: `admin` / `123456`
- **普通用户**: `user` / `123456`

## 📂 项目结构
- `/frontend`: Vue 3 应用程序
- `/backend`: Spring Boot 应用程序
- `/database`: 数据库初始化脚本
