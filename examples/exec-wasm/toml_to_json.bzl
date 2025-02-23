def _toml_to_json(ctx):
    ctx.file("BUILD.bazel", "exports_files(['example.json'])")

    toml_data = ctx.read(ctx.attr.toml)
    rc = ctx.execute_wasm(ctx.attr.toml_wasm, "toml_to_json", input = toml_data)
    if rc.return_code != 0:
        fail(json.decode(rc.output)["error"])

    ctx.file("example.json", rc.output)
    formatted_json = json.encode_indent(json.decode(rc.output))
    delim = ("-" * 20) + " >8 " + ("-" * 20)
    print("\nOriginal TOML:\n%s\n%s\n%s\nConverted to JSON:\n%s\n%s\n%s" % (delim, toml_data, delim, delim, formatted_json, delim))

toml_to_json = repository_rule(
    implementation = _toml_to_json,
    attrs = {
        "toml": attr.label(allow_single_file = True),
        "toml_wasm": attr.label(
            default = "//:tomlwasm/toml.wasm",
            allow_single_file = True,
        ),
    },
)
