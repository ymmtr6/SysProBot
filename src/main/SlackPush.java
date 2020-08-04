package main;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Properties;

/**
 * Slackを用いて通知を送る
 *
 * @author riku
 *
 */
public class SlackPush {

	/**
	 * テキストエンコードのパラメータ
	 */
	private String textEncode = "utf-8";

	/**
	 * Slack REST API chat.postMessage
	 */
	private final String baseURL = "https://slack.com/api/chat.postMessage";

	/**
	 * token(本来は環境変数やsecretファイルから入力するべき)
	 */
	private String token = "";

	/**
	 * secret.properteisからtokenやcodingなどのプロパティを読み込む。
	 */
	public SlackPush() {
		Properties properties = new Properties();
		try {
			InputStream is = getClass().getResourceAsStream("/secret.properties");
			properties.load(is);
			is.close();
			this.token = properties.getProperty("token");
			this.textEncode = properties.getProperty("coding");
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("secret.propertiesの読み取りに失敗しました。");
			System.err.println("src/secret.propertiesを確認してください。");
			System.exit(1);
		}
	}

	/**
	 * トークンとログファイルのテキストエンコーディングを指定して置く。
	 * @param token
	 * @param textEncode
	 */
	public SlackPush(String token, String textEncode) {
		this.token = token;
		this.textEncode = textEncode;
	}

	/**
	 * full_id -> slackUseridに変換する "1037|3334"は学籍番号のうち、所属コード。
	 *
	 * @return slack user id
	 */
	public String getSlackId(String full_id) {
 		//仕様変更(省略形からfullidへ)
//		String[] idParts = full_id.split("1037|3334");
//		return SlackMember.get(String.join("_", idParts));
		return SlackMember.get(full_id);
	}

	/**
	 * Slack APIにPOSTメッセージを送信する この辺は非同期の実装でやるべきかもしれないが、 量が多くない（100件程度？）であることが予想されるため
	 * 実装スピードを優先した。
	 * > Rate limiting conditions are unique for methods with this tier.
	 * For example, chat.postMessage generally allows posting one message per second
	 * per channel, while also maintaining a workspace-wide limit. Consult the
	 * method's documentation to better understand its rate limiting conditions.
	 *
	 * @param userid : slack user id(or channel id)
	 * @param text
	 */
	public boolean postMessage(String userid, String text, boolean debug) {
		boolean success = false;
		if (userid == null) {
			System.out.println(" [ERROR]USERID not Found");
			return success;
		}
		HttpURLConnection urlConn = null;
		try {
			String strUrl = baseURL;
			URL url = new URL(strUrl);
			urlConn = (HttpURLConnection) url.openConnection();
			urlConn.setRequestMethod("POST");

			// POST
			urlConn.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(urlConn.getOutputStream());
			wr.writeBytes(generateParam(userid, text));
			wr.flush();
			wr.close();

			success = urlConn.getResponseCode() == HttpURLConnection.HTTP_OK;
			if (success) {
				// System.out.println("200 OK");
				InputStream is = urlConn.getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(is));
				String line;
				String body = "";
				while((line = reader.readLine()) != null) {
					body += line;
				}
				if(body.indexOf("\"ok\":true") == -1) {
					success = false;
					System.out.println(" [ERROR]SlackAPI: " + line );
				}
				is.close();
			} else{
				System.out.println(" [ERROR]Response CODE: " + urlConn.getResponseCode());
			}
			urlConn.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (urlConn != null)
					urlConn.disconnect();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return success;
	}

	/**
	 *  Slack APIにPOSTメッセージを送信する この辺は非同期の実装でやるべきかもしれないが、 量が多くない（100件程度？）であることが予想されるため
	 * 実装スピードを優先した。
	 * > Rate limiting conditions are unique for methods with this tier.
	 * For example, chat.postMessage generally allows posting one message per second
	 * per channel, while also maintaining a workspace-wide limit. Consult the
	 * method's documentation to better understand its rate limiting conditions.
	 * @param userid
	 * @param text
	 * @param debug
	 */
	public boolean postMessage(String userid, String text) {
		return postMessage(userid, text, false);
	}

	/**
	 * URLパラメータを作成する。
	 *
	 * @param userid slackのuserID
	 * @param text   転送する内容
	 * @return URLパラメータ
	 * @throws UnsupportedEncodingException
	 */
	private String generateParam(String userid, String text) throws UnsupportedEncodingException {
		return "token=" + this.token + "&channel=" + userid
				+ "&link_names=true"
				+ "&text=" + URLEncoder.encode(text, textEncode);
	}

	/**
	 * 登録している全員に通知を行う。
	 *
	 * @param text
	 */
	public void postAllMember(String text) {
		String anim = "|/-\\";
		int count = 0;
		// 通知処理 (キャリッジリターンを追加)
		for (String id : SlackMember.members.values()) {
			count++;
			String data = String.format("\r %c 送信中(%d/%d) %s", anim.charAt(count % anim.length()), count, SlackMember.members.size(),
					id);
			try {
				System.out.write(data.getBytes());
				// デフォルトでは```で挟むことで、Slack上の表示をコードスタイルにする。
				postMessage(id, text);
				Thread.sleep(1000); // rate limit対策
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		System.out.println("\r   通知完了       　              ");
	}

	/**
	 * 送信テスト(テストでは自分に送信する)
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		SlackPush sp = new SlackPush();
		SlackMember.readProperties();
		String sid = sp.getSlackId("xxxxxxxxxx");
		System.out.println(sid);
		sp.postMessage(sid, "テストメッセージ", true);
	}

}
