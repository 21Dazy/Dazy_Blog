# 博客系统部署指南 (CentOS 7)

本文档提供在CentOS 7服务器上使用Docker部署博客系统的详细步骤。

## 前提条件

1. CentOS 7系统
2. 已连接互联网
3. 有root权限或sudo权限
4. 已开放80端口(HTTP)和8080端口(API)

## 1. 安装Docker和Docker Compose

```bash
# 安装必要的系统工具
sudo yum install -y yum-utils device-mapper-persistent-data lvm2

# 添加Docker仓库
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo

# 安装Docker
sudo yum install -y docker-ce docker-ce-cli containerd.io

# 启动Docker并设置开机自启
sudo systemctl start docker
sudo systemctl enable docker

# 安装Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/download/v2.17.2/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
sudo ln -s /usr/local/bin/docker-compose /usr/bin/docker-compose

# 验证安装
docker --version
docker-compose --version
```

## 2. 获取项目代码

```bash
# 创建项目目录
mkdir -p /opt/blog-app
cd /opt/blog-app

# 将项目文件上传到此目录
# 可以使用scp, rsync或git clone等方式
```

## 3. 配置项目

确认项目结构如下:

```
/opt/blog-app/
├── backend/
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
├── frontend/
│   ├── src/
│   ├── package.json
│   ├── Dockerfile
│   └── nginx.conf
└── docker-compose.yml
```

## 4. 构建和启动容器

```bash
cd /opt/blog-app

# 构建并启动容器
sudo docker-compose up -d

# 查看运行状态
sudo docker-compose ps
```

## 5. 数据库初始化

MySQL会自动创建数据库，但您可能需要导入初始数据:

```bash
# 如果有SQL初始化脚本，可以执行:
# sudo docker exec -i blog-mysql mysql -uroot -pyby258014 blog < init.sql
```

## 6. 检查部署状态

```bash
# 查看所有容器的日志
sudo docker-compose logs

# 或者查看特定服务的日志
sudo docker-compose logs backend
sudo docker-compose logs frontend
```

## 7. 访问应用

- 前端: http://服务器IP地址
- 后端API: http://服务器IP地址:8080/api

## 8. 常用维护命令

```bash
# 停止所有服务
sudo docker-compose stop

# 启动所有服务
sudo docker-compose start

# 重启所有服务
sudo docker-compose restart

# 停止并删除所有容器
sudo docker-compose down

# 停止并删除所有容器及其数据卷(谨慎使用!)
sudo docker-compose down -v

# 查看服务日志
sudo docker-compose logs -f [service_name]

# 连接到容器内部
sudo docker exec -it [container_name] bash
```

## 9. 备份数据

```bash
# 备份MySQL数据
sudo docker exec blog-mysql mysqldump -u root -pyby258014 blog > blog_backup_$(date +%Y%m%d).sql

# 备份上传的文件
sudo tar -zcvf uploads_backup_$(date +%Y%m%d).tar.gz /var/lib/docker/volumes/blog-app_blog-uploads
```

## 10. 更新应用

如需更新应用，请按以下步骤操作:

```bash
# 下载或更新项目代码
# ...

# 重新构建并启动容器
cd /opt/blog-app
sudo docker-compose down
sudo docker-compose build
sudo docker-compose up -d
```

## 11. 故障排除

1. 如果前端无法访问API:
   - 检查后端容器是否正常运行
   - 检查Nginx配置中的代理设置
   - 检查网络连接和防火墙设置

2. 如果容器无法启动:
   - 查看日志: `sudo docker-compose logs`
   - 检查硬盘空间: `df -h`
   - 检查内存使用: `free -m`

3. 数据库连接问题:
   - 检查MySQL容器是否正常运行
   - 确认数据库凭据配置正确
   - 检查容器网络是否配置正确 