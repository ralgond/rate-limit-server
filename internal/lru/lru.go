package lru

import "fmt"

type Node struct {
	key   string
	value any
	next  *Node
	prev  *Node
}

func CreateNode(key string, value any) *Node {
	return &Node{key: key, value: value, next: nil, prev: nil}
}

type HashMap map[string]*Node

type Queue struct {
	head   *Node
	tail   *Node
	length int
}

func NewQueue() Queue {
	head := &Node{}
	tail := &Node{}
	head.next = tail
	tail.prev = head
	return Queue{head: head, tail: tail, length: 0}
}

type Cache struct {
	MaxSize int
	queue   Queue
	hash    HashMap
}

func NewCache(MaxSize int) Cache {
	return Cache{MaxSize: MaxSize, queue: NewQueue(), hash: HashMap{}}
}

func (c *Cache) GetAndRemove(key string) *Node {
	if val, ok := c.hash[key]; ok {
		c.Remove(val)
		return val
	} else {
		return nil
	}
}

func (c *Cache) Remove(node *Node) *Node {
	// fmt.Printf("Remove %s\n", node.value)
	prev := node.prev
	next := node.next

	next.prev = prev
	prev.next = next

	c.queue.length -= 1
	delete(c.hash, node.key)
	return node
}

func (c *Cache) Add(node *Node) {
	// fmt.Printf("Add %s\n", node.value)
	headRight := c.queue.head.next

	node.prev = c.queue.head
	node.next = headRight
	headRight.prev = node
	c.queue.head.next = node
	c.queue.length += 1

	c.hash[node.key] = node

	if c.queue.length > c.MaxSize {
		c.Remove(c.queue.tail.prev)
	}
}

func (c *Cache) Display() {
	fmt.Printf("queue.head: %p\n", c.queue.head)
	fmt.Printf("queue.tail: %p\n", c.queue.tail)
	node := c.queue.head.next

	for i := 0; i < c.queue.length; i++ {
		fmt.Printf("{%s}", node.value)
		if i < c.queue.length-1 {
			fmt.Printf("<-->")
		}
		node = node.next
	}
	fmt.Println()
}
