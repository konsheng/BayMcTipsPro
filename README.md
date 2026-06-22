# BayMcTipsPro

BayMcTipsPro 是一个面向 Paper 和 Folia 的轻量级聊天栏公告插件, 负责向全服在线玩家发送 MiniMessage 聊天公告和可选公告声音

插件不提供 GUI, ActionBar, Title, BossBar 或跨服广播能力, 适合只需要简单聊天栏公告的小型服务器和登录服

## ✨ 功能特性

- 支持 Paper 和 Folia, Folia 环境使用全局调度器执行定时任务
- 支持 MiniMessage 公告文本, 可使用颜色, 悬停, 点击, URL 和命令事件
- 支持为单条公告配置可选声音, 使用 Adventure sound key 播放
- 支持从配置文件随机选择一条有效公告发送给所有在线玩家
- 支持自动定时公告, 可配置首次延迟和公告间隔
- 支持 `/tips next` 手动立即发送一条随机公告
- 支持 `/tips status` 查看公告功能, 任务状态, 有效公告数量和调度模式
- 支持 `/tips reload` 重载配置和语言文件
- 支持通过配置选择语言文件, 管理命令反馈, 日志和校验提示
- 支持将已发送公告的纯文本内容同步输出到控制台
- 支持启动和重载时校验公告文本, 无效 MiniMessage 公告会被跳过并输出日志

## 🧩 运行环境

| 项目 | 要求 |
| --- | --- |
| Java | 25 或更高版本 |
| Minecraft 服务端 | Paper 或 Folia |
| Paper API | 26.1.2 |
| 构建工具 | Gradle Wrapper |

## 🖥️ 服务端支持

| 服务端 | 支持状态 |
| --- | --- |
| Paper 26.1.2 | 支持 |
| Folia 26.1.2 | 支持 |
| Spigot | 不支持 |
| CraftBukkit | 不支持 |
| 其他 Paper 分支 | 未完整验证 |

## 📦 安装

1. 下载或构建 `BayMcTipsPro-*.jar`
2. 将 jar 文件放入服务器 `plugins` 目录
3. 启动一次服务器, 生成 `plugins/BayMcTipsPro/config.yml`, `plugins/BayMcTipsPro/tips.yml` 和 `plugins/BayMcTipsPro/lang/zh_CN.yml`
4. 编辑 `config.yml` 中的语言文件, 公告开关, 间隔和控制台输出
5. 编辑 `tips.yml` 中的公告内容
6. 按需编辑 `language.file` 指向的语言文件中的命令反馈和日志文本
7. 执行 `/tips reload` 重载插件, 或重启服务器
8. 给需要使用命令的玩家或权限组分配对应子命令权限

## 🧭 命令

主命令:
- `/baymctipspro`
- `/tips`

参数约定:
- `<>` 必填
- `[]` 选填

命令列表:

| 命令 | 说明 | 权限 |
| --- | --- | --- |
| `/tips` | 查看插件信息 | `baymctipspro.info` |
| `/tips help` | 查看命令帮助 | `baymctipspro.help` |
| `/tips info` | 查看插件信息 | `baymctipspro.info` |
| `/tips next` | 立即发送一条随机公告 | `baymctipspro.next` |
| `/tips status` | 查看公告运行状态 | `baymctipspro.status` |
| `/tips reload` | 重载配置和语言文件 | `baymctipspro.reload` |

玩家执行命令需要对应子命令权限, 控制台可直接执行命令

## 🔐 权限

| 权限 | 默认 | 说明 |
| --- | --- | --- |
| `baymctipspro.help` | OP | 允许查看命令帮助 |
| `baymctipspro.info` | OP | 允许查看插件信息 |
| `baymctipspro.next` | OP | 允许立即发送随机公告 |
| `baymctipspro.status` | OP | 允许查看公告运行状态 |
| `baymctipspro.reload` | OP | 允许重载配置和语言文件 |

## ⚙️ 配置

默认配置文件为 `plugins/BayMcTipsPro/config.yml`

```yaml
language:
  file: "zh_CN.yml"

announcements:
  enabled: true
  interval-seconds: 300
  initial-delay-seconds: 30
  send-to-console: true
```

配置项说明:

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `language.file` | `zh_CN.yml` | 使用的语言文件, 对应 `lang` 目录下的 yml 文件 |
| `announcements.enabled` | `true` | 是否启用自动定时公告 |
| `announcements.interval-seconds` | `300` | 自动公告间隔秒数, 小于 `5` 会自动提升到 `5` |
| `announcements.initial-delay-seconds` | `30` | 启动或重载后的首次公告延迟秒数, 小于 `1` 会自动提升到 `1` |
| `announcements.send-to-console` | `true` | 是否将已发送公告的纯文本写入控制台 |

`announcements.enabled` 只控制自动定时任务, 即使关闭自动公告, 只要存在有效公告, 仍可使用 `/tips next` 手动发送

## 💬 MiniMessage

公告文本只支持 MiniMessage, 不解析 `&a` 这类旧式颜色代码

默认公告文件为 `plugins/BayMcTipsPro/tips.yml`

可使用 MiniMessage 的点击和悬停语法:

```yaml
tips:
  - message: "<gold>[公告]</gold> 欢迎来到 <aqua>BayMC</aqua>"
    sound:
      name: "minecraft:block.note_block.pling"
      source: "ui"
      volume: 1.0
      pitch: 1.0
  - "<green>[主城]</green> <hover:show_text:'点击执行 /spawn'><click:run_command:'/spawn'>点击返回主城</click></hover>"
  - "<yellow>[官网]</yellow> <hover:show_text:'点击打开官网'><click:open_url:'https://example.com'><underlined>点击访问</underlined></click></hover>"
```

`tips` 支持纯字符串公告, 也支持对象公告, 对象公告使用 `message` 保存 MiniMessage 文本, 使用可选 `sound` 配置声音

| 声音项 | 默认值 | 说明 |
| --- | --- | --- |
| `sound.enabled` | `true` | 是否启用当前公告的声音 |
| `sound.name` | 无 | Minecraft 声音 key, 可省略 `minecraft:` 命名空间 |
| `sound.source` | `master` | 声音分类, 支持 `master`, `music`, `record`, `weather`, `block`, `hostile`, `neutral`, `player`, `ambient`, `voice`, `ui` |
| `sound.volume` | `1.0` | 声音音量, 不能小于 `0` |
| `sound.pitch` | `1.0` | 声音音调, 必须大于 `0` |

## 🌐 语言文件

默认语言文件为 `plugins/BayMcTipsPro/lang/zh_CN.yml`

可在 `config.yml` 中通过 `language.file` 选择其他语言文件, 文件必须放在 `plugins/BayMcTipsPro/lang/` 目录下

语言文件包含:
- 命令反馈
- 帮助文本
- 插件信息
- 状态输出
- 重载结果
- 配置校验提示
- 控制台日志

修改语言文件后执行 `/tips reload` 即可生效

## 🛠️ 构建

Windows:

```powershell
.\gradlew.bat clean build
```

Linux 或 macOS:

```bash
./gradlew clean build
```

本地产物:

```text
build/libs/BayMcTipsPro-1.0.0-SNAPSHOT.jar
```

开发版 CI 构建会在版本号后追加当前提交的 7 位短哈希:

```text
build/libs/BayMcTipsPro-1.0.0-SNAPSHOT-<short-sha>.jar
```

## 📄 许可证

本项目使用 GNU General Public License version 3, 详情见 [LICENSE](LICENSE)
