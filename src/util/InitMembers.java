package util;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

import main.SlackMember;

/**
 * 初期化用のプログラム
 * id:slackid対応表の永続化ファイル(slack-member.properties)を作成する。
 * @author riku
 *
 */
public class InitMembers {

	/** propertiesファイル名 */
	private String fileName;

	/** Slack APP Bot token */
	private String token;

	/** users.list call URL */
	private final String usersListURL = "https://slack.com/api/users.list";

	/** users.lookupByEmail call URl; Slack API側で権限の設定が必要 */
	private final String lookupByEmail = "https://slack.com/api/users.lookupByEmail";

	/** emailの正規表現。テストでは院生のメールアカウントを用いるため広めにしている */
	private final String emailRegex = "(\\d{10}\\w@kindai.ac.jp)";

	/** email map (学籍番号, email) */
	private Map<String, String> emailMap = new HashMap<>();

	/** 永続化ファイルの名前 */
	private final String slackMemberProperties = "slack-member.properties";

	/**
	 * 初期化するためのインスタンス
	 * @param fileName
	 */
	public InitMembers(String fileName) {
		this.fileName = fileName;
		Properties properties = new Properties();
		try {
			InputStream is = getClass().getResourceAsStream("/secret.properties");
			properties.load(is);
			is.close();
			this.token = properties.getProperty("token");
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("secret.propertiesの読み取りに失敗しました。");
			System.err.println("src/secret.propertiesを確認してください。");
			System.exit(1);
		}
	}

	/**
	 * SlackAPIで問い合わせ
	 * @param filename
	 * @param token
	 */
	public InitMembers(String filename, String token) {
		this.fileName = filename;
		this.token = token;
	}

	/**
	 * 学籍番号リストを読み込む
	 * 改行区切りを想定している。
	 * @param filename
	 * @return
	 */
	private List<String> readIDs(String filename){
		List<String> ids = new ArrayList<>();
		try {
			File file = new File(filename);
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
			while( (line = br.readLine()) != null) {
				ids.add(line);
			}
			br.close();
		} catch(IOException e) {
			e.printStackTrace();
			System.err.println("学籍番号リストの読み込みに失敗しました。");
			System.exit(1);
		}
		System.out.println("[Init]学籍番号リストから" + ids.size() + "件取得しました。");
		return ids;
	}


	/**
	 * 学籍番号("\\d{10}")からSlackIDを取得し、SlackMember.membersに反映する
	 * @param 学籍番号のリスト
	 */
	private void setUsersList(List<String> ids) {
		System.out.println("[Init]学籍:SlackID対応表を作成中");
		SlackMember.members = new HashMap<>();
		int count = 0;
		String anim = "|/-\\";
		for(String id : ids) {
			try {
				count++;
				String data = String.format("\r %c 受信中(%d/%d) %s",
						anim.charAt(count % anim.length()), count, ids.size(), id);
				System.out.write(data.getBytes());
				// idからemailを取得する。
				if(!emailMap.containsKey(id)) {
					System.err.println("はemailが発見できませんでした");
					continue;
				}
				String email = emailMap.get(id);
				// emailからSlackIDを取得する
				String slackid = this.lookupByEmaiil(email);
				Thread.sleep(1000);//　API Rate: 50+/s -> 1.2s
				if(slackid != null) {
					SlackMember.members.put(id, slackid);
				}else {
					System.err.println("はslackidが発見できませんでした");
				}
			} catch (Exception e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
				System.err.println(id + "はslackidが発見できません");			}
		}
		System.out.println("\r   受信完了    　　　　          ");
		System.out.println("[Init]学籍:SlackID対応表を作成完了");
	}

	/**
	 * emailマップを作成する
	 * @param emailList
	 */
	private void setEmailMap(List<String> emailList) {
		for(String email: emailList) {
			String fullId = email.substring(0, 10);
			emailMap.put(fullId, email);
		}
	}

	/**
	 * users.listから全てのSlackユーザを取得し、emailのみを抽出する。
	 * @param emailRegex "(\\d{10}\\w@kindai.ac.jp)"など。
	 * @return
	 */
	private List<String> getUserEmail(String emailRegex){
		System.out.println("[Init]WSの全ユーザの情報を取得...");
		ArrayList<String> l = new ArrayList<>();
		String users = usersList(null);
		Pattern p = Pattern.compile(emailRegex);
		Matcher m = p.matcher(users);
		while(m.find()) {
			l.add(m.group());
			//System.out.println(m.group());
		}
		System.out.println("[Init]" + emailRegex + "に一致した" + l.size() + "件取得");
		return l;
	}

	/**
	 * emailからidを取得する。
	 * @param email
	 * @return slackid (未発見はnull)
	 */
	private String lookupByEmaiil(String email) {
		HttpURLConnection urlConn = null;
		try {
			String strUrl = lookupByEmail
					+ "?token=" + token + "&email=" + email;
			URL url = new URL(strUrl);
			urlConn = (HttpURLConnection) url.openConnection();
			urlConn.setRequestMethod("GET");

			BufferedReader in = new BufferedReader(new InputStreamReader(urlConn.getInputStream(), "UTF-8"));
			String line;
			StringBuffer response = new StringBuffer();

			while( (line = in.readLine()) != null)
				response.append(line);
			in.close();

			//System.out.println(response.toString());
			Pattern p = Pattern.compile("\"id\":\"(?<id>[A-Z0-9]+)\"");
			Matcher m = p.matcher(response.toString());
			while(m.find())
				return m.group("id");

		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("lookupByEmail");
		}
		return null;
	}

	/**
	 * users.listにアクセスし文字列を返す。
	 * (日本語にunicode_escapeが働いているので注意)
	 * ページングを考慮して、nextCursorが存在する限り繰り返して呼び出す。
	 */
	private String usersList(String cursor) {
		StringBuffer response = new StringBuffer();
		HttpURLConnection urlConn = null;
		try {
			String strUrl = usersListURL + "?token="+this.token;
			if(cursor != null)
				strUrl += "&cursor=" + cursor;
			URL url = new URL(strUrl);
			urlConn = (HttpURLConnection) url.openConnection();
			urlConn.setRequestMethod("GET");

			//int responsecode = urlConn.getResponseCode();
			BufferedReader in = new BufferedReader(new InputStreamReader(urlConn.getInputStream(), "UTF-8"));
			String line;
			//System.out.println(responsecode);
			while( (line = in.readLine()) != null)
				response.append(line);
			in.close();
		}catch(Exception e) {
			e.printStackTrace();
		}
		String c = response.toString().split("next_cursor\":")[1];
		c = c.replaceAll("\"", "").replaceAll("}", "").replaceAll("\n", "");
		if(!c.equals("")){
			return response.toString() + usersList(c);
		}
		return response.toString();
	}

	/**
	 *　SlackMember.membersをプロパティファイルに置き換えて永続化する。
	 */
	private void writeProperties() {
		Properties properties = new Properties();
		try {
			for (String key : SlackMember.members.keySet()) {
				properties.setProperty(key, SlackMember.members.get(key));
			}
			properties.store(new FileOutputStream(slackMemberProperties), "Slack-member");
		} catch (IOException e) {
			e.printStackTrace();
			System.err.print("ファイル出力に失敗しました");
		}
	}

	/**
	 * 各工程を合わせて、永続化ファイルを作成する。
	 */
	public void run() {
		// ファイル読み込み
		List<String> ids = this.readIDs(fileName);
		// slackユーザ全員のメールアドレスを取得し、Mapに保存
		setEmailMap(this.getUserEmail(emailRegex));
		// 学籍番号->メールアドレス->SlackIDへ変換し対応を記録する
		setUsersList(ids);
		// (学籍番号: SlackID)の対応表を永続化
		writeProperties();
	}

	/**
	 * テスト関数
	 */
	public static void main(String[] args) {
		InitMembers obj = new InitMembers("idList.txt");
		obj.run();
	}

}
