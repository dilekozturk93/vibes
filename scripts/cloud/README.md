# Cloud Cluster Pipeline (DigitalOcean)

Inspired by the user's prior ESG-Fx cluster scripts (`Setup_And_Run_DigitalOcean.sh`,
`Cluster_Monitor.sh`, `Collect_Results_RQ*.sh`), adapted to VIBeS layout.

## Why cloud?

Per-product cost on Syngovia is **~23.5 min** (78 states × 307 transitions ×
~2000 mutants × 300 random suites kill-replay). 400 products × 23.5 min =
**~167 hours on one machine**. Local run is intractable; sharded cloud
parallelism (9 nodes × 3 workers = 27 parallel slots) brings wall-clock to
**~6-7 hours**.

Tesla and HockertyShirts are small enough to run locally (~1 h each), but
running the full 8-SPL set on the same hardware avoids platform-variance
threats and produces a uniform-condition CSV set for the paper.

## Architecture

```
ips.txt
  ↓
setup_and_run.sh        ← bootstraps every droplet (SSH + apt + git clone + nohup)
  ↓
node_orchestrator.sh    ← per-node loop: round-robin assigned shards from shards.txt,
                          launches up to PARALLEL_WORKERS workers concurrently
  ↓
worker.sh               ← runs ONE shard: invokes Java with env vars
                          SHARD_ID, PRODUCT_START_IDX, PRODUCT_END_IDX, runID
  ↓
PerProductMutationReportGenerator / ExperimentRunner
  → CSV at  milestone-reports/metrics/shards/<SPL>/<task>_<SPL>_shard<ID>.csv

cluster_monitor.sh      ← one-shot status snapshot (wrap in `watch -n 30`)
collect_results.sh      ← rsync shard CSVs back, aggregate into unified rq{1,2,3}-*.csv
```

## Step-by-step

### 1. Provision 9× DigitalOcean droplets (one command, doctl)

**Recommended: doctl** (DigitalOcean CLI) — opens 9 droplets, cloud-init
pre-installs Java/Maven, IPs auto-written to `ips.txt`.

```bash
# One-time setup
brew install doctl                # macOS; Linux: snap install doctl
doctl auth init                   # paste your DO Personal Access Token
doctl compute ssh-key list        # note your SSH key ID

# Provision (replace 12345678 with YOUR ssh key id)
SSH_KEY_ID=12345678 scripts/cloud/provision_cluster.sh

# Defaults: 9 nodes, s-8vcpu-16gb, nyc3 region, ubuntu-22-04-x64
# Overrides: NODE_COUNT=5 REGION=fra1 SSH_KEY_ID=... ./provision_cluster.sh
```

This:
- Creates 9× `vibes-node-{1..9}` droplets tagged `vibes-cluster`
- Bakes user-data so cloud-init installs `openjdk-11-jdk + maven + git`
  while the droplets boot
- Waits for SSH readiness on every droplet (`/var/lib/cloud/instance/boot-finished`)
- Writes public IPs to `scripts/cloud/ips.txt`

About **3-5 minutes total**. Cost: $0.10/h × 9 = **~$0.90/h while running**.

**Alternative (manual web UI)**: Create droplets via DO console, copy
public IPs into `scripts/cloud/ips.txt` manually. `setup_and_run.sh` has
a fallback that runs `apt-get install` if cloud-init didn't pre-install.

### 2. Bootstrap pipeline

```bash
GIT_REPO=https://github.com/dilekozturk93/vibes.git \
GIT_BRANCH=feat/product-test-generation \
scripts/cloud/setup_and_run.sh
```

SSHes into every node in `ips.txt` and:
- clones the repo on the specified branch
- generates `shards.txt`
- starts `node_orchestrator.sh` in `nohup` mode

About 1-2 minutes since cloud-init already installed Java/Maven.

### 5. Monitor

```bash
watch -n 30 scripts/cloud/cluster_monitor.sh
```

Per node: Java worker count, RAM%, load avg, completed shard CSVs,
currently active shard tags. A node showing `🟢 3 ... 60% 4.0 ... 5 ... svia02 hs00`
means: 3 workers, 60% RAM, load 4.0, 5 shards done so far, currently
running Syngovia shard 02 and HockertyShirts shard 00 etc.

Expect total wall-clock **6-10 hours** for the full 118 work units.
Most workers idle out after their assigned shards complete; the slowest
worker (one of the 20 Syngovia shards) dictates total wall-clock.

### 6. Collect results

When `cluster_monitor.sh` shows every node `⚫ 0 ... idle`:

```bash
scripts/cloud/collect_results.sh
```

This rsyncs `milestone-reports/metrics/shards/` from every node, then
aggregates shard CSVs into unified `milestone-reports/metrics/rq{1,2,3}-*.csv`
locally. Header is dedup'd; data rows concatenated in shard-name order.

### 7. Tear down droplets (CRITICAL — do this!)

After collection, destroy the droplets immediately — billing continues
hourly until they're deleted.

```bash
scripts/cloud/destroy_cluster.sh
# Prompts "type 'destroy' to confirm" → deletes every droplet tagged
# vibes-cluster and clears ips.txt
```

Verify in DO console afterwards that the droplets are gone.

## Per-shard CSV naming convention

```
milestone-reports/metrics/shards/<SPL>/
├── rq2-coverage-directed_<SPL>_shard<ID>.csv
├── rq2-random-baseline_<SPL>_shard<ID>.csv
├── rq3-efficiency_<SPL>_shard<ID>.csv
├── rq1-coverage-directed_<SPL>_shard<ID>.csv   (RQ1 sweep, runID embedded in <ID>)
└── rq1-family-baseline_<SPL>_shard<ID>.csv
```

`<ID>` examples: `svia07`, `sas01`, `hs00`, `te01`, `bav200`, `el00`,
`em00`, `svm00`, `svia_r03` (= RQ1 SPL=Syngovia, runID=3).

Aggregation produces the same `rq1-coverage-directed.csv`, `rq2-...`,
`rq3-...` files the local pipeline writes, drop-in compatible with the
existing analysis scripts.

## Compute estimate

| Workload | Shards | Cost/shard | Total CPU-hours |
|---|---|---|---|
| Syngovia RQ2 | 20 | ~7.8 h | 156 |
| SAS RQ2 | 2 | ~44 min | 1.5 |
| HockertyShirts / Tesla RQ2 | 4 | ~30 min | 2 |
| Small SPLs RQ2 (BAv2/El/eM/SVM) | 4 | <5 min | <1 |
| RQ1 sweep (8 SPL × 11 run) | 88 | varies | ~10 |
| **Total CPU-hours** | **118** | | **~170 h** |

With 27 parallel workers: **170 / 27 ≈ 6.3 h wall-clock** (Syngovia
shards dominate; their 7.8h-per-shard is the lower bound).

## Threats-to-Validity notes for the paper

1. **Platform variance**: All 8 SPLs run on identical s-8vcpu-16gb
   DigitalOcean droplets — uniform CPU model, OS, JVM, JIT warm-up
   exposure. Paper-fair comparison.

2. **Per-worker JVM heap**: 5 GB (Xmx=5g), matches the local 5-SPL
   137-min baseline run from 2026-05-24. No SAS-style OOM since heap
   per worker mirrors per-process local heap.

3. **Random ensemble seeds**: Identical 100×3 ensemble across all SPLs.
   Workers do NOT share random-baseline state — each shard generates
   its own 300 random suites from seeds `[0, 100)` × 3 budgets.
   Determinism preserved (same input → same output) within each shard.

4. **Family baseline**: Skipped for samples-mode SPLs (Syngovia, Tesla,
   HockertyShirts) since Devroey AllStates is intractable on 60+ FM
   variables. Documented as `terminationReason=skipped-samples-mode`
   in `rq1-family-baseline.csv`.
