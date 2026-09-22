# FEATO Coin Exchange

## Overview

FEATO Ancient Coin が生成する古銭をゲーム内通貨へ交換する専用 Paper Plugin です。古銭1枚を10G（`config.yml` で変更可能）へ交換し、NPCなどが発行するコンソールコマンドからのみ利用できます。古銭の生成、Loot、NPC、GUI機能は持ちません。

## Requirements

- Paper 26.2+
- Java 25
- Vault互換Plugin（想定: VaultUnlocked）
- Vaultへ登録されるEconomy provider（想定: EssentialsX Economy）

## Installation

`build/libs/FEATO-Coin-Exchange-<version>.jar` をサーバーの `plugins/` へ配置し、サーバーを再起動してください。Vault互換PluginとEconomy providerが必要です。Economy providerが見つからない場合、このPluginは安全のため無効化されます。

## Command

```text
/feato-coin-exchange <player>
```

オンラインPlayerの古銭を1枚だけ交換します。Bukkit ConsoleSenderからの実行専用で、一般Player・OP Playerのどちらからも直接実行できません。

## FancyNpcs

```text
/npc action ancient_coin_exchange RIGHT_CLICK add console_command feato-coin-exchange {player}
```

FancyNpcsには上記の交換コマンド1個だけを登録してください。`clear`、scoreboard、`eco give`、`wait` など複数actionの組み合わせで交換処理を構成しないでください。

## Ancient Coin identification

表示名やLoreではなく、次のMaterialと `minecraft:custom_data` を照合します。

```text
Material: GOLD_NUGGET
custom_data:
  feato_coin:
    id: ancient_coin
    schema: 1
```

Item nameやLoreなど、識別対象外のData Componentは変更されても判定に影響しません。

## Economy and transaction safety

Vault Economy APIの `depositPlayer` を使用します。対象slotのItemStackをcloneして1枚消費した後に入金し、`EconomyResponse` が失敗を示すか入金処理が例外を投げた場合は、cloneした正確なItemStackを元のslotへ復元します。

交換額は `plugins/FEATOCoinExchange/config.yml` の次の値で管理します。

```yaml
exchange-value: 10.0
```

## Build

```bash
./gradlew clean build
```

## Release

GitHub Actionsの **Release** workflowをdefault branchから手動実行し、`version`（例: `1.0.0` または `1.1.0-rc.1`）を入力します。workflowはversionと既存tag/releaseを検証し、Java 25でbuild後、JARをworkflow artifactへ保存してannotated tagとGitHub Releaseを作成します。
