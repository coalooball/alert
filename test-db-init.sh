#!/bin/bash

# 测试数据库自动初始化功能
# 此脚本将删除 alert_system 数据库并测试自动初始化

echo "============================================"
echo "测试数据库自动初始化功能"
echo "============================================"
echo ""

# 检查 PostgreSQL 是否运行
echo "1. 检查 PostgreSQL 状态..."
if ! docker ps | grep -q postgres-server; then
    echo "❌ PostgreSQL 容器未运行"
    echo "请先启动 PostgreSQL:"
    echo "  cd docker-compose && docker-compose -f postgres-compose.yml up -d"
    exit 1
fi
echo "✅ PostgreSQL 容器正在运行"
echo ""

# 删除现有数据库
echo "2. 删除现有的 alert_system 数据库..."
docker exec -it postgres-server psql -U admin -d postgres -c "DROP DATABASE IF EXISTS alert_system;"
echo "✅ 数据库已删除"
echo ""

# 编译项目
echo "3. 编译项目..."
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo "❌ 编译失败"
    exit 1
fi
echo "✅ 编译完成"
echo ""

# 运行应用（只运行几秒钟测试初始化）
echo "4. 启动应用测试自动初始化..."
echo "   应用将自动检测数据库不存在并创建..."
echo ""
timeout 15 java -jar target/alert-system-0.1.0.jar || true
echo ""

# 检查数据库是否创建
echo "5. 验证数据库是否创建成功..."
DB_EXISTS=$(docker exec postgres-server psql -U admin -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='alert_system';")
if [ "$DB_EXISTS" = "1" ]; then
    echo "✅ 数据库 alert_system 创建成功"
else
    echo "❌ 数据库 alert_system 未创建"
    exit 1
fi
echo ""

# 检查表是否创建
echo "6. 检查关键表是否创建..."
TABLES=$(docker exec postgres-server psql -U admin -d alert_system -tAc "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public';")
echo "   共创建 $TABLES 个表"

if [ "$TABLES" -gt 0 ]; then
    echo "✅ 表创建成功"
    echo ""
    echo "   主要表列表:"
    docker exec postgres-server psql -U admin -d alert_system -c "SELECT table_name FROM information_schema.tables WHERE table_schema='public' ORDER BY table_name;" | head -20
else
    echo "❌ 未创建表"
    exit 1
fi
echo ""

# 检查默认数据
echo "7. 检查默认数据是否插入..."
USER_COUNT=$(docker exec postgres-server psql -U admin -d alert_system -tAc "SELECT COUNT(*) FROM users;")
ALERT_TYPES_COUNT=$(docker exec postgres-server psql -U admin -d alert_system -tAc "SELECT COUNT(*) FROM alert_types;")
TAGS_COUNT=$(docker exec postgres-server psql -U admin -d alert_system -tAc "SELECT COUNT(*) FROM tags;")

echo "   用户数: $USER_COUNT (应该至少有 1 个默认管理员)"
echo "   告警类型数: $ALERT_TYPES_COUNT (应该有 3 个)"
echo "   标签数: $TAGS_COUNT (应该有 16 个)"

if [ "$USER_COUNT" -gt 0 ] && [ "$ALERT_TYPES_COUNT" -gt 0 ] && [ "$TAGS_COUNT" -gt 0 ]; then
    echo "✅ 默认数据插入成功"
else
    echo "❌ 默认数据插入失败"
    exit 1
fi
echo ""

echo "============================================"
echo "✅ 所有测试通过！"
echo "============================================"
echo ""
echo "数据库自动初始化功能正常工作："
echo "  ✅ 自动检测数据库不存在"
echo "  ✅ 自动创建数据库"
echo "  ✅ JPA 自动生成表结构"
echo "  ✅ 自动插入默认数据"
echo ""
echo "现在可以正常启动应用："
echo "  java -jar target/alert-system-0.1.0.jar"
echo ""