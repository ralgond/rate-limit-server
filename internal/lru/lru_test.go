package lru_test

import (
	"fmt"
	"github.com/ralgond/rate-limit-server/internal/lru"
	"testing"
)

func TestAdd(t *testing.T) {

	cache := lru.NewCache(5)
	// cache.Display()

	i := 0
	for i < 10 {
		key := fmt.Sprintf("%d", i)
		value := key

		n := cache.GetAndRemove("a")
		if n == nil {
			n = lru.CreateNode(key, value)
		}
		cache.Add(n)
		i += 1
	}

	cache.Display()
}
