package main

import (
	"context"
	"fmt"
	"github.com/bsm/ratelimit/v3"
	"github.com/ralgond/rate-limit-server/internal/config"
	"github.com/ralgond/rate-limit-server/internal/lru"
	"github.com/redis/go-redis/v9"
	"io"
	"net/http"
	"os"
	"sync"
	"time"
)

// 创建一个 Transport，并设置连接池参数
var transport = &http.Transport{
	MaxIdleConnsPerHost: 10,
	IdleConnTimeout:     30 * time.Second,
	MaxConnsPerHost:     50,
}

var rdb = redis.NewClient(&redis.Options{
	Addr:     "localhost:6379",
	Password: "",
	DB:       0,
	PoolSize: 100,
})
var ctx = context.Background()

type MutexCache struct {
	mutex sync.Mutex
	cache lru.Cache
}

func (mc *MutexCache) shouldBeLimited(key string) bool {
	mc.mutex.Lock()

	node := mc.cache.GetAndRemove(key)
	if node == nil {
		node = lru.CreateNode(key, ratelimit.New(1000*1000, time.Second))
	}
	mc.cache.Add(node)

	var shouldLimit = false
	if rl, ok := node.Value.(*ratelimit.RateLimiter); ok {
		shouldLimit = rl.Limit()
	} else {
		os.Exit(-1)
	}

	mc.mutex.Unlock()
	return shouldLimit
}

func NewMutexCache(capacity int) *MutexCache {
	return &MutexCache{cache: lru.NewCache(capacity)}
}

var mutexCache = NewMutexCache(1 * 1000 * 1000)

func handler(w http.ResponseWriter, r *http.Request) {
	sessionId, err := r.Cookie("sessionId")
	if err != nil {
		http.Error(w, "Cookie not found", http.StatusBadRequest)
		return
	}

	//done := make(chan struct{})
	//go func() {
	//	_, err = rdb.Get(ctx, sessionId.Value).Result()
	//	close(done)
	//}()
	//
	//<-done

	_, err = rdb.Get(ctx, sessionId.Value).Result()

	// fmt.Printf("err: ====>%v", err)

	if err != nil {
		http.Error(w, "StatusForbidden", http.StatusForbidden)
		return
	}

	// 复制请求头
	var xUpstreamServer = ""
	var xRealIp = ""
	for key, value := range r.Header {
		if key == "X-Upstream-Server" {
			xUpstreamServer = value[0]
		} else if key == "X-Real-Ip" {
			xRealIp = value[0]
		}
	}

	if mutexCache.shouldBeLimited(xRealIp) {
		http.Error(w, "Requests are being made too frequently.", http.StatusTooManyRequests)
		return
	}

	// 创建目标请求
	r.URL.Scheme = "http"
	r.URL.Host = xUpstreamServer

	proxyReq, err := http.NewRequest(r.Method, r.URL.String(), r.Body)
	if err != nil {
		http.Error(w, "Failed to create request", http.StatusBadRequest)
		return
	}

	// 复制请求头
	for key, value := range r.Header {
		if key == "X-Upstream-Server" {
			continue
		}
		proxyReq.Header[key] = value
	}

	proxyReq.Header["Host"] = r.Header["X-Upstream-Server"]

	// fmt.Println(proxyReq)

	// 发送请求到目标服务器
	client := &http.Client{
		Transport: transport,
	}
	resp, err := client.Do(proxyReq)
	if err != nil {
		http.Error(w, "Failed to reach target server, err:"+err.Error(), http.StatusBadGateway)
		return
	}
	defer resp.Body.Close()

	// 复制响应头
	for key, value := range resp.Header {
		w.Header()[key] = value
	}

	// 设置响应状态码
	w.WriteHeader(resp.StatusCode)

	// 复制响应主体
	io.Copy(w, resp.Body)
}

func main() {
	conf, err := config.LoadConfig("./configs/rate-limit-server.xml")
	if err != nil {
		fmt.Println("load configuration failed, err: ", err)
		return
	}

	mux := http.NewServeMux()
	mux.HandleFunc("/", handler)

	addrAndPort := conf.Frontend.Address
	server := http.Server{
		Addr:           addrAndPort,
		Handler:        mux,
		MaxHeaderBytes: 16 * 1024,
	}

	fmt.Println("Starting proxy server on port", addrAndPort)
	if err := server.ListenAndServe(); err != nil {
		fmt.Println("Error starting server:", err)
	}
}
