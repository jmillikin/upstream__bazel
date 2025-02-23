// Copyright 2025 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.devtools.build.lib.bazel.repository.starlark;

import static com.dylibso.chicory.runtime.Memory.PAGE_SIZE;
import static java.nio.charset.StandardCharsets.ISO_8859_1;

import com.dylibso.chicory.runtime.ExportFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.ChicoryException;
import com.dylibso.chicory.wasm.WasmModule;
import com.google.devtools.build.docgen.annot.DocCategory;
import com.google.devtools.build.lib.concurrent.ThreadSafety.Immutable;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkValue;

@Immutable
@StarlarkBuiltin(
    name = "wasm_module",
    category = DocCategory.BUILTIN,
    doc = "TODO")
final class StarlarkWasmModule implements StarlarkValue {
  private final StarlarkPath path;
  private final WasmModule wasmModule;
  private final String allocateFn;

  public StarlarkWasmModule(StarlarkPath path, byte[] moduleContent) throws EvalException {
    this(path, moduleContent, "bazel_wasm_allocate");
  }

  public StarlarkWasmModule(StarlarkPath path, byte[] moduleContent, String allocateFn) throws EvalException {
    WasmModule wasmModule;
    try {
      wasmModule = com.dylibso.chicory.wasm.Parser.parse(moduleContent);
    } catch (ChicoryException e) {
      throw new EvalException(e);
    }
    this.path = path;
    this.wasmModule = wasmModule;
    this.allocateFn = allocateFn;
  }

  @Override
  public boolean isImmutable() {
    return true;
  }

  public StarlarkPath getPath() {
    return path;
  }

  public StarlarkWasmExecutionResult execute(String functionName, byte[] input) throws EvalException {
    try {
      var instance = Instance.builder(wasmModule).withStart(false).build();
      ExportFunction initFn = instance.export("_initialize");
      if (initFn != null) {
        initFn.apply();
      }
      return execute(instance, functionName, input);
    } catch (ChicoryException e) {
      throw new EvalException(e);
    }
  }

  public StarlarkWasmExecutionResult execute(Instance instance, String functionName, byte[] input) throws ChicoryException, EvalException {
    var memory = instance.memory();
    ExportFunction allocFn = instance.export(allocateFn);
    ExportFunction function = instance.export(functionName);

    int inputPtr = alloc(allocFn, input.length);
    memory.write(inputPtr, input);

    int paramsPtr = alloc(allocFn, 8);
    int outputPtrPtr = paramsPtr;
    int outputLenPtr = paramsPtr + 4;
    memory.writeI32(outputPtrPtr, 0);
    memory.writeI32(outputLenPtr, 0);

    long[] execResult = function.apply(inputPtr, input.length, outputPtrPtr, outputLenPtr);
    int returnCode = (int)execResult[0];
    int outputPtr = memory.readInt(outputPtrPtr);
    int outputLen = (int)memory.readU32(outputLenPtr);

    String output = "";
    if (outputLen > 0) {
      byte[] outputBytes = memory.readBytes(outputPtr, outputLen);
      output = new String(outputBytes, ISO_8859_1);
    }
    return new StarlarkWasmExecutionResult(returnCode, output);
  }

  static int alloc(ExportFunction allocFn, long size) throws ChicoryException, EvalException {
    long[] allocResult = allocFn.apply(size);
    if (allocResult[0] == 0) {
      throw Starlark.errorf("failed to allocate %d bytes in WebAssembly instance", size);
    }
    return (int)allocResult[0];
  }
}
