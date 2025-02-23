/*

% tinygo version
tinygo version 0.35.0 [...]
% tinygo build -o toml.wasm -target wasm-unknown tomlwasm.go

*/

package main

import (
	"encoding/json"
	"fmt"
	"math"
	"unsafe"

	toml "github.com/pelletier/go-toml/v2"
)

var buffers = make(map[*uint8][]uint8)

//go:export bazel_wasm_allocate
func bazelWasmAllocate(size uint32) *uint8 {
	if size > math.MaxInt32 {
		return nil
	}
	buf := make([]uint8, int(size))
	ptr := unsafe.SliceData(buf)
	buffers[ptr] = buf
	return ptr
}

//go:export toml_to_json
func wasmTomlToJson(inputPtr *uint8, inputLen uint32, outputPtrPtr **uint8, outputLenPtr *uint32) int32 {
	input := unsafe.Slice(inputPtr, inputLen)
	output, rc := tomlToJson(input)

	outputPtr := unsafe.SliceData(output)
	buffers[outputPtr] = output
	*outputPtrPtr = outputPtr
	*outputLenPtr = uint32(len(output))
	return rc
}

func tomlToJson(input []uint8) ([]uint8, int32) {
	parsed := make(map[string]interface{})
	if err := toml.Unmarshal(input, &parsed); err != nil {
		output, _ := json.Marshal(map[string]string{
			"error": fmt.Sprintf("%v", err),
		})
		return output, 1
	}

	jsonBytes, err := json.Marshal(parsed)
	if err != nil {
		output, _ := json.Marshal(map[string]string{
			"error": fmt.Sprintf("%v", err),
		})
		return output, 1
	}

	return jsonBytes, 0
}
