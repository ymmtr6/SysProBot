package main;

import java.io.*;

/**
 * 自動出力のログを個人単位で分割するためのParser
 *
 * @author riku yamamoto
 *
 */
public class LogParser {

	/**
	 * Log全文を表示する
	 */
	private String logText = "";

	/**
	 * Fileの読み込み部分
	 *
	 * @param file
	 */
	public LogParser(String filename) {
		BufferedReader br = null;

		String str = "";
		try {
			br = new BufferedReader(new FileReader(new File(filename)));
			while ((str = br.readLine()) != null) {
				this.logText += str + "\n";
			}
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
		}
		// compile Errorのバーを除外する
		// TODO: もし上手く動かないなら、ここを正規表現で対応する。
		logText = logText.replaceAll("------------ Error-------------------\n", "");
	}

	/**
	 * 学籍番号を基準に出力を分割する。 ログ出力に自分以外の学籍番号10桁を書かれない限りはこれで大丈夫なはず...
	 *
	 * @return 分割された出力
	 */
	public String[] parse() {
		// 正規表現の先読みによって分割文字（学籍番号）自体を結果に含める
		String[] logs = logText.split("(?=\\d{2}(10370|3334)\\d{3})");
		return logs;
	}

	/**
	 * logから学籍番号を抽出する。 先頭10桁であることを想定している。
	 *
	 * @param log
	 * @return 学籍番号(full) 番号切り出し失敗していればnull
	 */
	public String extractID(String log) {
		String id = null;
		try {
			id = log.substring(0, 10);
			if (!id.matches("\\d{10}")) {
				// 念のためidの切り出し確認
				System.err.println("学籍番号違反検出: " + log);
				id = null;
			}
		} catch (Exception e) {
			System.err.println("学籍番号切り出しエラー: ");
			e.printStackTrace();
		}
		return id;
	}

	/**
	 * テスト用 分割表示を行う。
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("[USAGE] java main.LogParser [filepath]");
			System.exit(0);
		}
		LogParser lp = new LogParser(args[0]);
		String[] logs = lp.parse();
		// System.out.println(lp.logText);
		for (String log : logs) {
			System.out.println("---" + lp.extractID(log) + "---");
			System.out.println(log);
		}
	}
}
