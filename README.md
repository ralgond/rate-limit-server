# rate-limit-server（开发中）

## 用途
[Rate-Limit-System](https://github.com/ralgond/Rate-Limit-System-Design)中的一个重要组件

## 构建
go build cmd/rate-limit-server/main.go

## 技术参数
1. 1000并发的情况下，平均延迟为25ms，相比其所代理的nginx的23ms，多了2ms
