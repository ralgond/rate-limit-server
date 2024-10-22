package config

import (
	"encoding/xml"
	"fmt"
	"os"
)

type Config struct {
	Frontend FrontendConfig `xml:"frontend"`
}

type FrontendConfig struct {
	Address string `xml:"bind,attr"`
}

func LoadConfig(filePath string) (*Config, error) {
	file, err := os.Open(filePath)
	if err != nil {
		return nil, err
	}
	defer file.Close()

	var config Config
	decoder := xml.NewDecoder(file)
	if err := decoder.Decode(&config); err != nil {
		fmt.Println("Error decoding XML:", err)
		return nil, err
	}

	fmt.Printf("frontend bind: %s\n", config.Frontend.Address)

	return &config, nil
}
