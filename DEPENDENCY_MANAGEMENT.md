# Dependency & Library Management

Structr runs on the **Java module path** (JPMS). Structr's own code is compiled into explicit JPMS
modules; third-party libraries that are JPMS-hostile are quarantined onto the **class path**. This
document explains how that split is produced, how to add dependencies, and how to add new modules.

## Runtime layout

A built distribution (zip / deb / docker) launches with:

```
java --module-path lib -cp 'lib-classpath/*:plugins/*:structr-<version>.jar' \
     --add-modules ALL-MODULE-PATH -m structr.base/org.structr.Server
```

| Path             | Class/module path | Contents |
|------------------|-------------------|----------|
| `lib/`           | **module path**   | Structr's own JPMS modules + all modular third-party jars. |
| `lib-classpath/` | class path        | JPMS-hostile "island" jars (see below), reached only via ServiceLoader SPIs from the quarantined modules. |
| `plugins/`       | class path        | **User / optional drop-in jars** (e.g. a JDBC driver). Put extra jars here — **never** in `lib/`. |
| `structr-*.jar`  | class path        | The resources-only application jar (web UI + config templates). |

> Never drop arbitrary jars into `lib/`: it is resolved with `--add-modules ALL-MODULE-PATH`, so a jar
> with split packages / an invalid automatic-module name / an unsatisfiable hard `requires` will abort
> startup. Use `plugins/`.

## How the split is produced (the partitioner)

`maven-dependency-plugin:copy-dependencies` first copies **all** runtime dependencies into a flat
`target/lib`. Then a `maven-antrun-plugin` execution (`partition-module-path`, `package` phase) runs a
shared tool that **moves** the JPMS-hostile jars to `target/lib-classpath`:

```
java structr-app/src/main/resources/build/ModulePathPartitioner.java <lib> <lib-classpath> <seed>
```

Classification is **curated seed + automated detection**:

1. **Seed** — `structr-app/src/main/resources/build/island-seed.txt`. One rule per line:
   - `<glob>` — force the jar to the class path (e.g. `neo4j-*.jar`)
   - `name:<module>` — force the jar with this *derived module name* to the class path (version-agnostic;
     e.g. `name:org.neo4j.annotations`)
   - `+<glob>` / `+name:<module>` — **pin** to the module path (wins over everything; e.g.
     `+neo4j-java-driver-*.jar`, kept on the module path for the Bolt driver)
2. **Automated pass** — for every jar still on the module path, the tool quarantines it if it has an
   (1) invalid/underivable automatic-module name, (2) duplicate module name, (3) split package shared
   with another module-path jar, or (4) an unsatisfiable hard (non-`static`) `requires`. It only ever
   *adds* to the class path and never moves a `+`-pinned jar.

The tool + seed are **shipped inside `structr-app.jar`** and **reused by the enterprise build**
(`structr-app-enterprise` harvests them when it unpacks `structr-app`). The seed encodes the shared
(OSS) knowledge; the automated pass handles whatever extra dependencies an edition adds, so editions do
not need to hand-curate their own glob lists.

## Adding a third-party dependency

1. Add it to the relevant module's `pom.xml` as usual and build.
2. **Most jars need no action** — modern, well-formed jars resolve cleanly on the module path.
3. If the jar is JPMS-hostile, the **automated pass quarantines it for you** — no config needed. Verify
   after a build:
   ```
   java --module-path structr-app/target/lib --validate-modules   # must print nothing
   ```
   and boot the feature that uses it.
4. **Only edit the seed** for cases the automated rules cannot detect — chiefly a jar that is healthy on
   its own but must travel *with* an already-quarantined engine (the Neo4j `server-api` /
   `org.neo4j.annotations` companions are the canonical example: needed on the class path so the
   embedded engine does not split across the module/class-path boundary). Add a `<glob>` or
   `name:<module>` line, or a `+` line to pin something the rules would otherwise move.

To see *why* a jar was moved, read the partitioner's build output (`[partitioner] … (reason)`).

## Adding a Structr module

- **Explicit module** (preferred): put `module-info.java` in `src/main/module/` (its own source root —
  keeps the compiler's QDox descriptor parser away from the rest of the sources). `requires structr.base`
  (+ its dependency modules), `exports`/`opens` only what is needed, and `provides` its
  `StructrModule` / `Service` / `Agent` SPI implementations.
- **Automatic module** (fallback): if the module pulls a JPMS-hostile dependency that breaks module
  resolution, omit `module-info.java` and keep the `META-INF/services/...` provider files. The module
  then sits on the module path as an automatic module and reads its hostile deps from the class-path
  islands (the `file-access` / `messaging-engine` modules work this way).
- Structr discovers all modules/services/agents/drivers via **`ServiceLoader`** (there is no class-path
  scan), so the provider files / `provides` clauses are mandatory.

### Cross-module reflection
`structr.base` reflectively instantiates impls that live in feature modules (servlets, websocket
commands, `Service`s, `Agent`s). The base-side call site adds a runtime read edge
(`X.class.getModule().addReads(target.getModule())`) and the providing module must `exports` the
package. When you add such a type in a module, export its package.

## Toolchain

Java 25 (GraalVM). First build needs network once (some transitive deps use open version ranges that
cannot resolve offline until cached); afterwards `mvn -o …` works. Tests run on the class path
(`useModulePath=false`, already configured) — see the surefire/failsafe config in `structr-base/pom.xml`.
