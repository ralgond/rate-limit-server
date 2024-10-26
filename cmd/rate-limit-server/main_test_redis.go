package main

import (
	"context"
	"fmt"
	"github.com/redis/go-redis/v9"
	"net/http"
)

var rdb2 = redis.NewClient(&redis.Options{
	Addr: "localhost:6379",
})

func handlerSet(w http.ResponseWriter, r *http.Request) {
	go func() {
		// 处理 Redis 请求
		key := "a"
		// value := "1"

		//if key == "" || value == "" {
		//	http.Error(w, "key and value are required", http.StatusBadRequest)
		//	return
		//}

		ctx := context.Background()
		// err := rdb2.Set(ctx, key, value, 0).Err()
		err := rdb2.Incr(ctx, key).Err()
		if err != nil {
			http.Error(w, fmt.Sprintf("Error setting value in Redis: %v", err), http.StatusInternalServerError)
			return
		}

		// fmt.Printf("Set %s = %s in Redis\n", key, value)
		// w.WriteHeader(http.StatusOK)
		return
	}()
}

func main() {
	http.HandleFunc("/set", handlerSet)
	fmt.Println("Server started at :8002")
	http.ListenAndServe(":8002", nil)
}
