# Apply Diff from Clipboard — IntelliJ IDEA Plugin

[![MIT License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

An IntelliJ plugin that lets you apply `git diff` / patch content directly from your clipboard into your project, with smart alignment and error tolerance — built for messy AI-generated diffs, unordered hunks, and real-world use.

---

## 🔍 Features

- **Clipboard-driven patching** — just copy a diff, run the action, and it applies to your project  
- **Tolerance to messiness** — handles extra or malformed lines, unordered hunks, and context misalignment  
- **Preview & confirmation** before applying  
- **Notifications & error messages** for patches that fail or partially apply  
- **Minimal dependencies** — Kotlin, IntelliJ APIs, no heavy external libs  

---

## 🚀 Quick Start (5-minute test)

1. Clone your repo:
   ```bash
   git clone https://github.com/<yourusername>/intellij-apply-diff-plugin.git
   cd intellij-apply-diff-plugin
   ```
2. Build plugin:
   ```bash
   ./gradlew buildPlugin
   ```
3. Open IntelliJ IDEA → **Settings → Plugins → ⚙ → Install Plugin from Disk**  
   Select the `.zip` file in `build/distributions/`  
4. Restart IDE  
5. Copy a diff to your clipboard, then run **“Apply Diff from Clipboard”** via **Actions / Tools / Search**  
6. Check that the patch is applied to the correct file.  

---

## 🧩 Example Diff

```diff
diff --git a/src/Main.kt b/src/Main.kt
index e69de29..4b825dc 100644
--- a/src/Main.kt
+++ b/src/Main.kt
@@ -0,0 +1,4 @@
+fun helloWorld() {
+    println("Hello, IntelliJ Plugin!")
+}
```

After copying that, run the plugin — it will create or modify `Main.kt` with the new lines.

---

## ⚙️ Installation / Publishing

### ✅ GitHub (for users / developers)

- The full source is in this repo  
- Releases include compiled `.zip` plugin  
- Developers can build from source using `./gradlew buildPlugin`

### 📦 JetBrains Marketplace (future)

When ready to publish:
1. Log into JetBrains Marketplace  
2. Upload your `.zip` (from `build/distributions`)  
3. Fill metadata (plugin ID, version, compatible builds, icons, screenshots)  
4. Submit for review & listing

---

## 📂 Project Structure

```
intellij-apply-diff-plugin/
├── build/
│   └── distributions/        ← plugin ZIPs here
├── src/
│   └── main/
│       ├── kotlin/com/yourorg/ApplyDiffAction.kt  
│       └── resources/META-INF/plugin.xml  
├── build.gradle.kts  
├── settings.gradle.kts  
├── gradle.properties  
├── LICENSE  
└── README.md  
```

---

## 🛠️ Developer Guide & Build

```bash
# Build plugin
./gradlew clean buildPlugin

# Run plugin in sandboxed IDE
./gradlew runIde
```

Use the `runIde` task to test without installing manually.

---

## 🤝 Contributing

1. Fork the repository  
2. Create a new feature/bugfix branch  
3. Make changes + test (use `runIde`)  
4. Open a Pull Request with clear description and diff examples  

Please sign off with `--signoff` if you contribute patches.

---

## 🛡 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

## 🧰 Troubleshooting

| Problem | Solution |
|---|---|
| Plugin not appearing | Confirm that your ZIP is in `build/distributions` and install via “Install Plugin from Disk” |
| Diff fails to apply | Try a simpler diff, check file paths are correct, view error messages in dialogue |
| Build errors | Run with `--refresh-dependencies`, delete `.gradle` and rebuild |

---

**Apply Diff from Clipboard** — one action to patch entire diffs from your clipboard.
