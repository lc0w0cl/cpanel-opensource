# 系统信息监控API文档

## 概述

本项目使用oshi-core库实现系统信息监控功能，提供CPU、内存、磁盘和系统基本信息的查询接口。

## 依赖配置

已在`backend/pom.xml`中添加oshi-core依赖：

```xml
<!-- OSHI 系统信息监控依赖 -->
<dependency>
    <groupId>com.github.oshi</groupId>
    <artifactId>oshi-core</artifactId>
    <version>6.6.5</version>
</dependency>
```

## API接口

### 1. 获取完整系统信息

**接口地址：** `GET /api/system/info`

**响应示例：**
```json
{
  "code": 200,
  "message": "操作成功",
  "success": true,
  "data": {
    "cpu": {
      "model": "Intel(R) Core(TM) i7-10700K CPU @ 3.80GHz",
      "cores": 8,
      "logicalProcessors": 16,
      "usage": 15.67,
      "frequency": 3800000000
    },
    "memory": {
      "total": 32.0,
      "used": 16.45,
      "available": 15.55,
      "usage": 51.41
    },
    "disks": [
      {
        "name": "C:\\",
        "fileSystem": "NTFS",
        "total": 931.51,
        "used": 456.78,
        "available": 474.73,
        "usage": 49.03
      }
    ],
    "system": {
      "osName": "Windows",
      "osVersion": "Windows 11 version 10.0.22000",
      "architecture": "amd64",
      "hostname": "DESKTOP-ABC123",
      "uptime": 86400
    }
  }
}
```

### 2. 获取CPU信息

**接口地址：** `GET /api/system/cpu`

**响应示例：**
```json
{
  "code": 200,
  "message": "操作成功",
  "success": true,
  "data": {
    "model": "Intel(R) Core(TM) i7-10700K CPU @ 3.80GHz",
    "cores": 8,
    "logicalProcessors": 16,
    "usage": 15.67,
    "frequency": 3800000000
  }
}
```

### 3. 获取内存信息

**接口地址：** `GET /api/system/memory`

**响应示例：**
```json
{
  "code": 200,
  "message": "操作成功",
  "success": true,
  "data": {
    "total": 32.0,
    "used": 16.45,
    "available": 15.55,
    "usage": 51.41
  }
}
```

### 4. 获取磁盘信息

**接口地址：** `GET /api/system/disk`

**响应示例：**
```json
{
  "code": 200,
  "message": "操作成功",
  "success": true,
  "data": [
    {
      "name": "C:\\",
      "fileSystem": "NTFS",
      "total": 931.51,
      "used": 456.78,
      "available": 474.73,
      "usage": 49.03
    },
    {
      "name": "D:\\",
      "fileSystem": "NTFS",
      "total": 1863.01,
      "used": 1200.50,
      "available": 662.51,
      "usage": 64.44
    }
  ]
}
```

### 5. 获取系统基本信息

**接口地址：** `GET /api/system/basic`

**响应示例：**
```json
{
  "code": 200,
  "message": "操作成功",
  "success": true,
  "data": {
    "osName": "Windows",
    "osVersion": "Windows 11 version 10.0.22000",
    "architecture": "amd64",
    "hostname": "DESKTOP-ABC123",
    "uptime": 86400
  }
}
```

## 数据说明

### CPU信息字段
- `model`: CPU型号
- `cores`: 物理核心数
- `logicalProcessors`: 逻辑处理器数
- `usage`: CPU使用率（百分比，保留两位小数）
- `frequency`: CPU频率（Hz）

### 内存信息字段
- `total`: 总内存（GB，保留两位小数）
- `used`: 已使用内存（GB，保留两位小数）
- `available`: 可用内存（GB，保留两位小数）
- `usage`: 内存使用率（百分比，保留两位小数）

### 磁盘信息字段
- `name`: 磁盘名称/挂载点
- `fileSystem`: 文件系统类型
- `total`: 总容量（GB，保留两位小数）
- `used`: 已使用容量（GB，保留两位小数）
- `available`: 可用容量（GB，保留两位小数）
- `usage`: 使用率（百分比，保留两位小数）

### 系统基本信息字段
- `osName`: 操作系统名称
- `osVersion`: 操作系统版本
- `architecture`: 系统架构
- `hostname`: 主机名
- `uptime`: 系统运行时间（秒）

## 错误处理

所有接口在发生错误时都会返回统一的错误格式：

```json
{
  "code": 500,
  "message": "获取系统信息失败：具体错误信息",
  "success": false,
  "data": null
}
```

## 注意事项

1. **性能考虑**: CPU使用率的计算需要两次采样（间隔1秒），因此CPU相关接口响应时间会稍长。

2. **权限要求**: 某些系统信息的获取可能需要特定权限，在受限环境中可能无法获取完整信息。

3. **Docker环境**: 在Docker容器中运行时，获取的是容器内的系统信息，而非宿主机信息。如需获取宿主机信息，需要特殊配置。

4. **单位统一**: 所有容量相关数据统一使用GB作为单位，所有百分比数据保留两位小数。

## 测试

可以使用提供的测试类 `SystemInfoServiceTest` 来验证功能：

```bash
cd backend
mvn test -Dtest=SystemInfoServiceTest
```
