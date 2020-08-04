# SysProBot
情報システムプロジェクト1の講義で使う、LogをparseしてBotから個別メッセージを送信するプログラム

## SETTING

1. 予めSlackBotを作成し、(適切な権限を設定し）、tokenを取得する。
2. secret.propertiesを編集する。

```
$ cd SysProBot/src
$ mv _secret.properties secret.properties
$ nano secret.properties
```

3. 学籍番号のリストが入ったファイルを読み込ませ、SlackIDとの対応表(slack-member.properties)を作成する。
```
$ java main.BotNotification --userfile userid.txt
```

## USAGE

学籍番号毎に区切られたログファイルのパスを入れてやると、

```
$ java main.BotNotification sample.txt 
[SlackMember]slack-member.propertiesの読み込みに成功しました。
---19XXXXXXXX---
19XXXXXXXX

Botを改良したのでテストします。

---19XXXXXXXX---
19XXXXXXXX

Botを改良したのでテストします。
この文をコピペしてXXXXXXXXXXまでDMしてください

################################
件数: 2
送信人数: 1
送信宛先一覧: 
19XXXXXXXX(WXXXXXXXXX)
19XXXXXXXX(未登録)
PlainTextMode: false

【最終確認】Slack通知を行いますか? [y/N]: 
```
ログを学籍番号毎に分割し、詳細表示を行う。
最終確認でyを入力すれば、Botが各ユーザに通知を行う。

