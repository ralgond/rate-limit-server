package main

import (
	"fmt"
	"github.com/ralgond/rate-limit-server/internal/config"
	"io"
	"net/http"
	"sync"
	"time"
)

// 创建一个 Transport，并设置连接池参数
var transport = &http.Transport{
	MaxIdleConnsPerHost: 10,
	IdleConnTimeout:     30 * time.Second,
	MaxConnsPerHost:     50,
	WriteBufferSize:     1 * 1024 * 1024,
}

type MuI struct {
	mutex sync.Mutex
	i     int
}

var kMuI = &MuI{}

func incr(mui *MuI) {
	mui.mutex.Lock()
	mui.i += 1
	mui.mutex.Unlock()
}

func handler(w http.ResponseWriter, r *http.Request) {
	incr(kMuI)

	// 复制请求头
	var xUpstreamServer = ""
	for key, value := range r.Header {
		if key == "X-Upstream-Server" {
			xUpstreamServer = value[0]
			break
		}
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
