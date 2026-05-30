# Web demo (WASM)

The `samples/reader-web` module is a Compose Multiplatform / wasmJs build of the
EPUB reader. It is published to GitHub Pages, served out of this `docs/` folder.

Live demo: `https://wavesonics.github.io/epub4kmp/`

## How to update the demo

The built WASM artifacts are kept on a dedicated `demo` branch so the generated
`.wasm` / `.js` bundles don't clutter `main`. GitHub Pages is configured to
serve the `docs/` folder from that branch.

1. Check out the `demo` branch and bring it up to date with `main`:
   ```bash
   git checkout demo
   git merge main
   ```

2. Run the `updateDemo` task:
   ```bash
   ./gradlew :samples:reader-web:updateDemo
   ```

   This will automatically:
   - Build the WASM distribution (`wasmJsBrowserDistribution`)
   - Sync all output files into the `docs/` directory, pruning any stale
     artifacts the current build no longer produces (e.g. old content-hashed
     `.wasm`/`.js` files) while preserving `DEMO.md` and `.nojekyll`
   - Write a `.nojekyll` marker so GitHub Pages serves the bundle verbatim

3. Commit and push:
   ```bash
   git add docs/
   git commit -m "Update web demo"
   git push
   ```

4. GitHub Pages will redeploy automatically. The demo will be available at the
   Pages URL above.

## One-time GitHub Pages setup

In the repository settings → Pages, set the source to **Deploy from a branch**,
branch `demo`, folder `/docs`.
