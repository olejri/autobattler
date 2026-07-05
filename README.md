# Autobattler

An async autobattler game built with **Kotlin** and **libGDX**.

## Project Structure

```
core/       - Platform-independent game logic (battle engine, models)
desktop/    - Desktop (LWJGL3) launcher for development
```

## Key Concepts

- **Async Battles**: Battles are deterministic simulations seeded with a shared random seed. Players submit their team compositions and battles resolve without requiring both players online simultaneously.
- **Deterministic Engine**: `BattleEngine.simulate()` produces identical results given the same teams + seed, enabling replay and verification.
- **Serializable State**: All models use kotlinx.serialization for easy save/load and network transmission.

## Building & Running

```bash
./gradlew desktop:run
```

## Tech Stack

- Kotlin 1.9.22
- libGDX 1.12.1
- kotlinx.serialization (JSON)
- kotlinx.coroutines
- Gradle 8.5 (Kotlin DSL)

