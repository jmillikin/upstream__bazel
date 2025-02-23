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

import com.google.devtools.build.docgen.annot.DocCategory;
import com.google.devtools.build.lib.concurrent.ThreadSafety.Immutable;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.StarlarkValue;

@Immutable
@StarlarkBuiltin(
    name = "wasm_execution_result",
    category = DocCategory.BUILTIN,
    doc = "TODO")
final class StarlarkWasmExecutionResult implements StarlarkValue {
  private final int returnCode;
  private final String output;

  public StarlarkWasmExecutionResult(int returnCode, String output) {
    this.returnCode = returnCode;
    this.output = output;
  }

  @Override
  public boolean isImmutable() {
    return true;
  }

  @StarlarkMethod(
      name = "return_code",
      structField = true,
      doc = "TODO")
  public int getReturnCode() {
    return returnCode;
  }

  @StarlarkMethod(
      name = "output",
      structField = true,
      doc = "TODO")
  public String getOutput() {
    return output;
  }
}
