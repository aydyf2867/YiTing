# 颐听 · 流程6 总规划

## 目标
为老人打造的安卓应用「颐听」：大字号、主色 #FA2D48（鲜亮红），
三大功能：听广播 / 听书 / 听戏。

## 技术栈
Kotlin + Jetpack Compose + Gradle Kotlin DSL + 版本目录
+ Media3 播放。8 模块架构：
app / core-ui / core-data / core-player
/ feature-radio / feature-book / feature-opera / feature-read

## 流程约定
- 分支：dev_1.0 开发，验收后 PR 合入 release_1.0
- 编辑：github.dev（仓库页按 . 键）或网页直接编辑
- 编译：GitHub Actions 自动构建，产物 Artifact 名 YiTing-debug-apk
- 测试：MuMu 模拟器装 APK 验收；最终版需借真机确认
- 提交习惯：多个文件一次提交，减少中间失败记录

## 任务进度
- [x] T01 骨架与主界面（模拟器验收通过）
- [ ] T02 首页与全局体验
- [ ] T04 听戏（本地音频播放）
- [ ] T05 听书（文本朗读）

## T01 验收记录（2024 · 模拟器）
应用名「颐听」✅ / 不闪退 ✅ / 红色标题 ✅ / 三 Tab 可切换 ✅
/ 各页占位文案正确 ✅ / 主色 #FA2D48 ✅ / 大字号 ✅
