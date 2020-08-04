package main;

import java.util.*;

import util.InitMembers;

import java.io.*;

/**
 * SlackBotによる通知を行う。実行はこのクラス。
 *
 * @author riku yamamoto
 *
 */
public class BotNotification {

	/** version */
	public static final String version = "1.1";
	/**
	 * ユーザ毎に送信内容を集めたMap
	 */
	public static Map<String, String> contentsMap = new TreeMap<>();;

	/**
	 * Slack APIへ送信する
	 */
	public static SlackPush sp = new SlackPush();

	/**
	 * ログの件数
	 */
	public static int logCount = 0;

	/**
	 * 送信人数
	 */
	public static int sendCount = 0;

	/**
	 * アプリケーションのバージョン表記
	 */
	public static void version() {
		System.out.println("シスプロ1通知BOT v" + BotNotification.version);
	}

	/**
	 * USAGE出力
	 */
	public static void usage() {
		BotNotification.version();
		System.out.println("[USAGE] $ java main.BotNotification [filepath] [code|plain]");
		System.out.println("デフォルトではcodeモードで送信します。Slack通知の際にコードやログを見易くなります。");
		System.out.println("plainモードでは、Slackの絵文字や修飾が効くようになります。");
		System.out.println();
		System.out.println("!!! (初期化)学籍番号のリストをSLACKID対応表に変換する !!!");
		System.out.println("$ java main.BotNotification --userfile [filepath]");
		System.out.println("!!! (ログ確認)どのようにログが送信されるか確認する　!!!");
		System.out.println("$ java main.BotNotification --log-parse [filepath]");
		System.out.println("!!! (USAGE)オプション確認 !!!");
		System.out.println("$ java main.BotNotification [-h|--help]");
		System.out.println("!!! (バージョン情報)バージョンを確認する !!!");
		System.out.println("$ java main.BotNotification [-v|-V|--version]");
	}

	/**
	 * LogParseを行う
	 *
	 * @param logname
	 */
	public static void parse(String logname) {
		LogParser lp = new LogParser(logname);
		// log取得
		String[] logs = lp.parse();
		BotNotification.logCount = logs.length;

		// 個人毎に集計する。idが抽出できない場合は弾く。
		for (String log : logs) {
			String id = lp.extractID(log);
			// そもそもparse時にidを使ってるので、null値はない気がするが...念のため。
			if (id == null)
				continue;
			if (contentsMap.containsKey(id))
				contentsMap.put(id, contentsMap.get(id) + log);
			else
				contentsMap.put(id, log);
		}
	}

	/**
	 * ContentsMapを表示
	 */
	public static void showContents() {
		for (String id : contentsMap.keySet()) {
			System.out.println("---" + id + "---");
			System.out.println(contentsMap.get(id));
		}
	}

	/**
	 * 送信情報を表示
	 *
	 * @param plainTextFlag
	 */
	public static void showSummary(boolean plainTextFlag) {
		sendCount = contentsMap.size();
		for (String id : contentsMap.keySet()) {
			String slackID = sp.getSlackId(id);
			if (slackID == null)
				sendCount--;
		}
		System.out.println("################################");
		System.out.println("件数: " + BotNotification.logCount);
		System.out.println("送信人数: " + sendCount);
		System.out.println("送信宛先一覧: ");
		for (String id : contentsMap.keySet()) {
			String slackId = sp.getSlackId(id);
			System.out.println(id + "(" + ((slackId != null) ? slackId : "未登録") + ")");
		}
		System.out.println("PlainTextMode: " + plainTextFlag);
	}

	/**
	 * 送信に関するエラーチェックを行う
	 */
	public static void errorCheck() {
		if (sendCount == 0) {
			System.out.println("送信人数が0人です。");
			System.exit(0);
		} else if (logCount == 0) {
			System.out.println("ログ件数が0件です。");
			System.exit(0);
		}
	}

	/**
	 * 確認を促し、t/fを返す。
	 *
	 * @return
	 */
	public static boolean finalCheck(String text) {
		System.out.print("\n" + text + " [y/N]: ");
		InputStreamReader isr = new InputStreamReader(System.in);
		BufferedReader br = new BufferedReader(isr);
		String str = null;
		try {
			str = br.readLine();
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return str.equals("y");
	}

	/**
	 * 送信処理
	 *
	 * @param plainTextFlag
	 */
	public static void send(boolean plainTextFlag) {
		String anim = "|/-\\";
		int count = 0;
		// 通知処理 (キャリッジリターンを追加)
		for (String id : contentsMap.keySet()) {
			count++;
			String data = String.format("\r %c 送信中(%d/%d) %s", anim.charAt(count % anim.length()), count,
					contentsMap.size(), id);
			try {
				System.out.write(data.getBytes());
				// デフォルトでは```で挟むことで、Slack上の表示をコードスタイルにする。
				String message = plainTextFlag ? contentsMap.get(id) : "```" + contentsMap.get(id) + "```";
				sp.postMessage(sp.getSlackId(id), message);
				Thread.sleep(1000); // rate limit対策
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		System.out.println("\r   通知完了       　              ");
	}

	/**
	 * CUIで動作することを前提に考えているため、引数を要求している。
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		// -------------------------------------------------------------
		// ここからオプションをparseして処理
		// -------------------------------------------------------------
		if (args.length == 0 || args[0].equals("-h") || args[0].equals("--help")) {
			BotNotification.usage();
			System.exit(0);
		} else if (args[0].equals("-v") || args[0].equals("--veresion") || args[0].equals("-V")) {
			// ここの時点で引数は1個以上
			BotNotification.version();
			System.exit(0);
		} else if (args.length == 2 && args[0].equals("--userfile")) {
			// 初期化(slackid対応表作成)
			InitMembers init = new InitMembers(args[1]);
			init.run();
			System.exit(0);
		} else if (args.length == 2 && args[0].equals("--log-parse")) {
			// LogParser確認
			parse(args[1]);
			showContents();
			System.exit(0);
		} else if (args.length >= 2 && args[0].equals("--everyone")) {
			// 全体アナウンス
			SlackMember.readProperties();
			String announce = String.format("【要注意】登録者%d人全員にSlack通知を行いますか?", SlackMember.members.size());
			String message = String.join(" ", args).replaceAll("--everyone ", "");
			System.out.println("--- 全員へ送信するメッセージ ---");
			System.out.println(message);
			if (!finalCheck(announce))
				System.exit(0);
			sp.postAllMember(message);
			System.exit(0);
		}

		// -------------------------------------------------------------
		// ここからメインの通知プログラム
		// -------------------------------------------------------------

		// plain Textにするかどうかのフラグ
		// 左優先なので、IndexOutBoundsにはならない
		boolean plainTextFlag = args.length >= 2 && args[1].equals("plain");
		// properties fileの読み込み
		SlackMember.readProperties();
		// ログ読み込み
		parse(args[0]);
		// 送信内容出力
		showContents();
		// 送信チェック
		showSummary(plainTextFlag);
		// エラー検出
		errorCheck();
		// 最終確認
		if (!finalCheck("【最終確認】Slack通知を行いますか?"))
			System.exit(0);
		// 送信処理
		send(plainTextFlag);
	}
}
