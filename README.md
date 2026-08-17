# My Ampere

App Android native de monitoring live du courant batterie (mA), avec widget home 2x1 rouge (décharge) / vert (charge) — dans l'esprit d'Ampere, en plus léger.

## Fonctionnalités

- **Écran live** : courant instantané en mA, couleur charge/décharge, min/max de session, graphe temps réel des 10 dernières minutes, tension/température/niveau/santé/technologie.
- **Widget 2x1** (redimensionnable) : valeur courante colorée, mise à jour toutes les 1-30 s selon l'état écran/charge, tap pour ouvrir l'app.
- **Historique** : persistance locale (Room), downsampling automatique, graphe 24 h.
- **Sobriété** : un seul échantillonneur (foreground service), zéro wakeup écran éteint en décharge, updates widget partielles et gatées — budget cible < 1 %/jour.

## Stack

Kotlin, Jetpack Compose, RemoteViews (widget), Room, coroutines. minSdk 31, targetSdk 34.

## Build

```bash
./gradlew assembleDebug
./gradlew :app:testDebugUnitTest
```

Un JDK 17 est requis pour la toolchain (voir `app/build.gradle.kts`). Testé sur Samsung Galaxy S20+ (Android 13) — l'émulateur ne renvoie pas de valeurs de courant réelles, la validation se fait sur device.

## Documentation

- [`docs/superpowers/specs/`](docs/superpowers/specs/) — design et choix d'architecture (normalisation OEM du courant, machine à états d'échantillonnage, budget conso).
- [`docs/superpowers/plans/`](docs/superpowers/plans/) — plan d'implémentation détaillé, tâche par tâche.

## Licence

Projet personnel, aucune licence associée pour l'instant.
