package main;

import java.io.*;
import java.util.*;

/**
 * 講義を受けている人を管理する。
 *
 * @author riku yamamoto
 *
 */
public class SlackMember {

	/**
	 * hashmap: 学籍番号とslack uidの対応付けを行う。
	 * propertiesファイルから学籍番号とSlackの対応を読み込む。
	 */
	public static Map<String, String> members;


	/**
	 * GC形式の学籍番号(??_????)からSlackIDを返す。 今後の変更を減らすためのラップ
	 *
	 * @param id
	 * @return SlackID 学籍番号が合致しない場合はnull
	 */
	public static String get(String id) {
		if(SlackMember.members == null) {
			System.err.println("SlackMember.readProperties()を呼び出してください");
			System.exit(1);
		}
		String ans = SlackMember.members.get(id);
		return ans;
	}


	/**
	 * slack-member.propertiesを元に
	 * (学籍番号"XX_XXXX": SlackID"WXXXXX")の対応を持つMapを生成する。
	 */
	public static void readProperties() {
		SlackMember.readProperties("slack-member.properties");
	}

	/**
	 * 指定したProperties fileを元に(学籍番号"XX_XXXX": SlackID"WXXXXX")の対応を持つMapを生成する。
	 * @param fileName
	 */
	public static void readProperties(String fileName) {
		SlackMember.members = new HashMap<>();
		// load properties
		Properties properties = new Properties();
		try {
			InputStream is = new FileInputStream(fileName);
			properties.load(is);
			is.close();
			// mapに格納する。
			for (Map.Entry<Object, Object> e : properties.entrySet()) {
				members.put(e.getKey().toString(), e.getValue().toString());
			}
		} catch(FileNotFoundException notFound) {
			System.err.println("slack-member.propertiesが見つかりませんでした。");
			System.err.println("SlackID対応表作成(初期化)を実行の場合は正常動作です。");
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("slack-member.propertiesの読み取りに失敗しました。");
			System.err.println("初期化を実行するか、");
			System.err.println("実行ファイルと同階層にslack-member.propertiesを設置してください。");
		}
		System.out.println("[SlackMember]slack-member.propertiesの読み込みに成功しました。");
	}

	/**
	 * SlackMemberに一斉に通知を送るためのログファイルを生成する
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != 2) {
			System.out.println("USAGE: java main.SlackMember [filepath] [message]");
			System.exit(0);
		}
		try {
			FileWriter fw = new FileWriter(args[0]);
			PrintWriter pw = new PrintWriter(new BufferedWriter(fw));
			for (String key : SlackMember.members.keySet()) {
				String id = String.join("1037", key.split("_"));
				pw.println(id + "\n" + args[1]);
			}
			pw.close();
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
	}

}
