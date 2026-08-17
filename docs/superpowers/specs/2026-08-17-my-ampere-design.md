# My-Ampere — Design

**Date** : 2026-08-17
**Statut** : approuvé (design chat validé, spec à relire)
**Cible** : app native Android de monitoring live de la consommation/charge batterie (type Ampere), avec widget 2x1 rouge/vert.

## Contexte et objectifs

- Afficher en live le courant batterie (mA) : charge (vert) / décharge (rouge).
- Widget home screen 2x1 montrant la valeur courante avec le même code couleur.
- Historique persisté + graphe temps réel dans l'app.
- **Contrainte forte** : l'app elle-même doit consommer très peu (< 1 %/jour, voir §Critère de succès).
- Distribution : usage perso, sideload (pas de contrainte Google Play).
- Cible : téléphone perso récent, minSdk 31 (Android 12), targetSdk courant.
- Dev/test : vrai téléphone via adb (l'émulateur renvoie des valeurs de courant factices).

## Stack retenue (approche A)

- **Kotlin**, Gradle Kotlin DSL.
- **App** : Jetpack Compose (Material 3).
- **Widget** : RemoteViews XML classique + `partiallyUpdateAppWidget` (`setTextViewText` + `setTextColor` uniquement par tick). Glance rejeté : il régénère l'arbre RemoteViews complet à chaque update — trop cher pour un push toutes les 1-5 s.
- **Persistance** : Room.
- **Pas de lib de graphe externe** : Canvas Compose.

## Architecture — un seul échantillonneur

```
BatteryManager ──> SamplerService (FGS) ──> BatteryRepository (StateFlow<BatterySample>)
                        │                        ├──> UI Compose (collecte le flow)
                        │                        └──> Ring buffer ──> Room (flush par lots)
                        └──> WidgetPusher (RemoteViews partiel, gaté)
```

- **`SamplerService`** : foreground service, type `specialUse`, notification `IMPORTANCE_MIN`, permission runtime `POST_NOTIFICATIONS` (Android 13+). Seul producteur de mesures. Lit :
  - `BatteryManager.BATTERY_PROPERTY_CURRENT_NOW` (courant instantané) ;
  - sticky broadcast `ACTION_BATTERY_CHANGED` (%, tension, température, statut charge, santé, technologie).
- **`BatteryRepository`** (singleton) : expose `StateFlow<BatterySample>` + min/max session. L'app **observe ce flow** — jamais de deuxième sampler quand l'app est ouverte.
- **`BatterySample`** : `timestamp, currentMa (signé, normalisé), levelPct, voltageMv, tempDeciC, status`.

### Normalisation OEM (étape 0 du plan)

`BATTERY_PROPERTY_CURRENT_NOW` varie selon OEM : unité (µA vs mA) et signe (charge positive ou négative). Sonde adb obligatoire avant d'écrire la couche de normalisation, téléphone branché puis débranché :

```bash
adb shell getprop ro.product.manufacturer ro.product.model
adb shell dumpsys battery
adb shell cat /sys/class/power_supply/battery/current_now   # plusieurs lectures espacées
```

La sonde tranche : unité, convention de signe, granularité de rafraîchissement de la fuel gauge (certaines ne bougent que toutes les ~10 s — ajusterait l'intervalle app visible). La normalisation vit dans une fonction pure testée unitairement. Convention interne : **charge = positif = vert ; décharge = négatif = rouge**.

## Machine à états écran × charge (budget conso)

Receivers `SCREEN_ON`/`SCREEN_OFF` **dynamiques**, enregistrés par le service (manifest impossible de toute façon).

| État | Intervalle d'échantillonnage |
|------|------------------------------|
| App visible | 1 s |
| Widget seul, écran on | 5 s |
| Écran off + décharge | **stop total** (0 wakeup, 0 timer) |
| Écran off + en charge | 30 s (courbe de charge nocturne, gratuit sur secteur) |

Transitions déclenchées par : screen on/off, `ACTION_POWER_CONNECTED`/`DISCONNECTED`, lifecycle de l'app (ProcessLifecycleOwner). La machine à états est une fonction pure `(état, événement) -> intervalle` testée unitairement.

## Widget 2x1

- Layout : valeur mA en gros + libellé d'état (charge/décharge/plein).
- Couleur : **vert** charge, **rouge** décharge.
- **Push gaté** : écran on ET (|Δ| ≥ 50 mA OU flip rouge/vert OU ≥ 10 s depuis dernier push). Chaque update RemoteViews a un coût — on ne pousse pas à chaque échantillon.
- Update partiel uniquement (`partiallyUpdateAppWidget`).
- Tap → ouvre l'app.
- Le `AppWidgetProvider` garde un `updatePeriodMillis = 0` (jamais de refresh système) ; fallback `onUpdate` = dernière valeur connue (DataStore) grisée si le service ne tourne pas.

## Historique — Room par lots

- Ring buffer mémoire dans le repository ; **flush** vers Room toutes les 60 s ou 100 lignes, et au stop du service.
- Rétention : brut 24 h ; au-delà, downsampling 1 point/min ; purge > 30 jours. (Chiffres par défaut, ajustables.)
- Downsampling + purge : job périodique dans le service au moment d'un flush (pas de WorkManager séparé).
- Écriture via DAO Room uniquement.

## Écran app

1. **Live** : gros chiffre mA rouge/vert, min/max session, état de charge, graphe temps réel des 10 dernières minutes (Canvas Compose, données en mémoire depuis le flow).
2. **Historique** : graphe sur heures/jours, requête Room downsamplée à la fenêtre affichée.
3. **Infos** : tension, température, niveau %, santé, technologie (depuis `ACTION_BATTERY_CHANGED`).
4. Contrôle : toggle marche/arrêt du monitoring (start/stop du FGS).

## Gestion d'erreurs

- `BATTERY_PROPERTY_CURRENT_NOW` renvoie `Integer.MIN_VALUE` ou 0 constant → afficher « non supporté » plutôt que des zéros verts.
- Service tué par l'OS → `START_STICKY` ; widget retombe sur la dernière valeur grisée.
- Batterie du monitoring : notification du FGS minimale, pas de wakelock, aucun wakeup écran off en décharge.

## Critère de succès conso

- **Budget : < 1 %/jour attribué à l'app**, mesuré après ≥ 24 h d'usage normal :
  ```bash
  adb shell dumpsys batterystats --charged <package> 
  ```
- Test d'acceptation de la contrainte « optimiser la conso ». Si dépassé : suspecter les updates widget (gate trop lâche) puis l'intervalle écran-on.

## Tests

- **Unitaires** (JVM) : machine à états d'intervalle, normalisation signe/unité, gate de push widget, downsampling/rétention.
- **Room** : DAO en instrumenté ou Robolectric.
- **Sur device uniquement** : valeurs de courant réelles, comportement FGS, widget. Émulateur = valeurs factices → jamais de conclusion depuis l'émulateur.
- Boucle dev : `adb install`, logcat filtré par tag.

## Hors scope (YAGNI)

- Publication Play/F-Droid, multi-device, thèmes du widget, export de données, alertes/notifications de seuil, mesure par app tierce.
